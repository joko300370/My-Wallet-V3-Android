package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.swap.nabu.datamanagers.SwapPair
import com.blockchain.swap.nabu.datamanagers.repositories.swap.SwapRepository
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.coincore.AddressFactory
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NonCustodialAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import timber.log.Timber

class TransactionInteractor(
    private val coincore: Coincore,
    private val addressFactory: AddressFactory,
    private val swapRepository: SwapRepository
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
                    TxValidationFailure(ValidationState.INVALID_ADDRESS))
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
        if (action != AssetAction.Swap) coincore.getTransactionTargets(sourceAccount, action)
        else coincore.getTransactionTargets(sourceAccount, action).zipWith(swapRepository.getSwapAvailablePairs())
            .map { (accountList, pairs) ->
                accountList.filterIsInstance(CryptoAccount::class.java)
                    .filter { account ->
                        pairs.any { it.source == sourceAccount.asset && account.asset == it.destination }
                    }
            }

    fun getAvailableSourceAccounts(action: AssetAction): Single<List<CryptoAccount>> {
        require(action == AssetAction.Swap) { "Source account should be preselected for action $action" }
        return coincore.allWallets().zipWith(swapRepository.getSwapAvailablePairs()).map { (accountGroup, pairs) ->
            accountGroup.accounts.filter { account ->
                (account is TradingAccount || account is NonCustodialAccount) &&
                        (account as? CryptoAccount)?.isAvailableToSwapFrom(pairs) ?: false &&
                        account.isFunded && !account.isArchived
            }
        }.map { it.map { account -> account as CryptoAccount } }
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
        transactionProcessor?.let {
            it.reset()
        } ?: Timber.e("Transaction Interactor - calling reset on uninitialised transaction")
    }
}

private fun CryptoAccount.isAvailableToSwapFrom(pairs: List<SwapPair>): Boolean =
    pairs.any { it.source == this.asset }

private val Throwable.isUnexpectedContractError
    get() = (this is AddressParseError && this.error == AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
