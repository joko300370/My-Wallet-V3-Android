package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CurrencyPair
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.EligibilityProvider
import com.blockchain.swap.nabu.datamanagers.repositories.swap.CustodialRepository
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.coincore.AddressFactory
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.NonCustodialAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import timber.log.Timber

class TransactionInteractor(
    private val coincore: Coincore,
    private val addressFactory: AddressFactory,
    private val custodialRepository: CustodialRepository,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs,
    private val eligibilityProvider: EligibilityProvider
) {
    private var transactionProcessor: TransactionProcessor? = null

    fun invalidateTransaction() =
        Completable.fromAction {
            transactionProcessor = null
        }

    fun validatePassword(password: String): Single<Boolean> =
        Single.just(coincore.validateSecondPassword(password))

    fun validateTargetAddress(address: String, asset: CryptoCurrency): Single<ReceiveAddress> =
        addressFactory.parse(address, asset)
            .switchIfEmpty(
                Single.error<ReceiveAddress>(
                    TxValidationFailure(ValidationState.INVALID_ADDRESS)
                )
            )
            .onErrorResumeNext { e ->
                if (e.isUnexpectedContractError) {
                    Single.error(TxValidationFailure(ValidationState.ADDRESS_IS_CONTRACT))
                } else {
                    Single.error(e)
                }
            }

    fun initialiseTransaction(
        sourceAccount: SingleAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Observable<PendingTx> =
        coincore.createTransactionProcessor(sourceAccount, target, action)
            .doOnSubscribe { Timber.d("!TRANSACTION!> SUBSCRIBE") }
            .doOnSuccess {
                if (transactionProcessor != null)
                    throw IllegalStateException("TxProcessor double init")
            }
            .doOnSuccess { transactionProcessor = it }
            .doOnError {
                Timber.e("!TRANSACTION!> error initialising $it")
            }.flatMapObservable {
                it.initialiseTx()
            }

    val canTransactFiat: Boolean
        get() = transactionProcessor?.canTransactFiat ?: throw IllegalStateException("TxProcessor not initialised")

    fun updateTransactionAmount(amount: Money): Completable =
        transactionProcessor?.updateAmount(amount) ?: throw IllegalStateException("TxProcessor not initialised")

    fun getTargetAccounts(sourceAccount: CryptoAccount, action: AssetAction): Single<SingleAccountList> =
        Singles.zip(
            coincore.getTransactionTargets(sourceAccount, action),
            action.apiPairs(),
            eligibilityProvider.isEligibleForSimpleBuy()
        ).map { (accountList, currencyPairs, eligible) ->
            if (action == AssetAction.Swap) {
                val pairs = currencyPairs.map { it as CurrencyPair.CryptoCurrencyPair }
                accountList.filterIsInstance(CryptoAccount::class.java)
                    .filter { account ->
                        pairs.any { it.source == sourceAccount.asset && account.asset == it.destination }
                    }.filter { account ->
                        eligible or (account is NonCustodialAccount)
                    }
            } else {
                val pairs = currencyPairs.map { it as CurrencyPair.CryptoToFiatCurrencyPair }
                accountList.filterIsInstance(FiatAccount::class.java)
                    .filter { account ->
                        pairs.any { it.source == sourceAccount.asset && account.fiatCurrency == it.destination }
                    }
            }
        }

    private fun AssetAction.apiPairs(): Single<List<CurrencyPair>> =
        when (this) {
            AssetAction.Swap -> custodialRepository.getSwapAvailablePairs()
                .map { it.map { pair -> pair as CurrencyPair } }
            AssetAction.Sell -> kotlin.run {
                val availableFiats =
                    custodialWalletManager.getSupportedFundsFiats(currencyPrefs.selectedFiatCurrency, true)
                return@run custodialWalletManager.getSupportedBuySellCryptoCurrencies()
                    .zipWith(availableFiats) { supportedPairs, fiats ->
                        supportedPairs.pairs.filter { fiats.contains(it.fiatCurrency) }
                            .map {
                                CurrencyPair.CryptoToFiatCurrencyPair(it.cryptoCurrency,
                                    it.fiatCurrency) as CurrencyPair
                            }
                    }
            }
            else -> Single.just(emptyList())
        }

    fun getAvailableSourceAccounts(action: AssetAction): Single<List<CryptoAccount>> {
        require(action == AssetAction.Swap) { "Source account should be preselected for action $action" }
        return coincore.allWallets()
            .zipWith(
                custodialRepository.getSwapAvailablePairs()
            ).map { (accountGroup, pairs) ->
                accountGroup.accounts.filter { account ->
                    (account as? CryptoAccount)?.isAvailableToSwapFrom(pairs) ?: false
                }
            }.map {
                it.map { account -> account as CryptoAccount }.filter { account ->
                    account.actions.contains(AssetAction.Swap)
                }
            }
    }

    fun verifyAndExecute(secondPassword: String): Completable =
        transactionProcessor?.execute(secondPassword) ?: throw IllegalStateException("TxProcessor not initialised")

    fun modifyOptionValue(newConfirmation: TxConfirmationValue): Completable =
        transactionProcessor?.setOption(newConfirmation) ?: throw IllegalStateException("TxProcessor not initialised")

    fun startFiatRateFetch(): Observable<ExchangeRate.CryptoToFiat> =
        transactionProcessor?.userExchangeRate()
            ?.map { it as ExchangeRate.CryptoToFiat }
            ?: throw IllegalStateException("TxProcessor not initialised")

    fun startTargetRateFetch(): Observable<ExchangeRate> =
        transactionProcessor?.targetExchangeRate() ?: throw IllegalStateException("TxProcessor not initialised")

    fun validateTransaction(): Completable =
        transactionProcessor?.validateAll() ?: throw IllegalStateException("TxProcessor not initialised")

    fun reset() {
        transactionProcessor?.reset() ?: Timber.i("TxProcessor is not initialised yet")
    }
}

private fun CryptoAccount.isAvailableToSwapFrom(pairs: List<CurrencyPair.CryptoCurrencyPair>): Boolean =
    pairs.any { it.source == this.asset }

private val Throwable.isUnexpectedContractError
    get() = (this is AddressParseError && this.error == AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
