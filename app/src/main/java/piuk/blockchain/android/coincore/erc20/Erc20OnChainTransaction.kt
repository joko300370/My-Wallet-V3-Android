package piuk.blockchain.android.coincore.erc20

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionValidationError
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.impl.OnChainSendProcessorBase
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

open class Erc20OnChainTransaction(
    final override val asset: CryptoCurrency,
    private val erc20Account: Erc20Account,
    private val feeManager: FeeDataManager,
    exchangeRates: ExchangeRateDataManager,
    sendingAccount: Erc20NonCustodialAccount,
    sendTarget: CryptoAddress,
    requireSecondPassword: Boolean
) : OnChainSendProcessorBase(
        exchangeRates,
        sendingAccount,
        sendTarget,
        requireSecondPassword
) {
    private val ethDataManager: EthDataManager =
        erc20Account.ethDataManager

    override val feeOptions = setOf(FeeLevel.Regular)

    override var pendingTx = PendingTx(
        amount = CryptoValue.zero(asset),
        available = CryptoValue.zero(asset),
        fees = CryptoValue.ZeroEth,
        feeLevel = FeeLevel.Regular,
        options = setOf(
            TxOptionValue.TxTextOption(
                option = TxOption.DESCRIPTION
            )
        )
    )

    private fun absoluteFee(): Single<CryptoValue> =
        feeOptions().map {
            CryptoValue.fromMinor(
                CryptoCurrency.ETHER,
                Convert.toWei(
                    BigDecimal.valueOf(it.gasLimitContract * it.regularFee),
                    Convert.Unit.GWEI
                )
            )
        }

    private fun feeOptions(): Single<FeeOptions> =
        feeManager.ethFeeOptions.singleOrError()

    override fun updateAmount(amount: CryptoValue): Single<PendingTx> =
        Singles.zip(
            sendingAccount.balance.map { it as CryptoValue },
                absoluteFee()
        ) { available, fee ->
                if (amount <= available) {
                    pendingTx.copy(
                        amount = amount,
                        available = available,
                        fees = fee
                    )
                } else {
                    throw TransactionValidationError(TransactionValidationError.INSUFFICIENT_FUNDS)
                }
            }
            .doOnSuccess { this.pendingTx = it }

    // In an ideal world, we'd get this via a CryptoAccount object.
    // However accessing one for Eth here would break the abstractions, so:
    private fun getEthAccountBalance(): Single<Money> =
        ethDataManager.fetchEthAddress()
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }
            .map { it as Money }

    override fun validate(): Completable =
        validateAddresses()
            .then { validateAmount(pendingTx) }
            .then { validateSufficientFunds(pendingTx) }
            .then { validateSufficientGas(pendingTx) }
            .then { validateNoPendingTx() }
            .doOnError { Timber.e("Validation failed: $it") }

    // This should have already been checked, but we'll check again because
    // burning tokens by sending them to the contract address is probably not what we
    // want to do
    private fun validateAddresses(): Completable =
        ethDataManager.isContractAddress(sendTarget.address)
            .map { isContract ->
                if (isContract || sendTarget !is Erc20Address) {
                    throw TransactionValidationError(TransactionValidationError.INVALID_ADDRESS)
                } else {
                    isContract
                }
            }.ignoreElement()

    private fun validateAmount(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.amount <= CryptoValue.zero(asset)) {
                throw TransactionValidationError(TransactionValidationError.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        sendingAccount.balance
            .map { balance ->
                if (pendingTx.amount > balance) {
                    throw TransactionValidationError(TransactionValidationError.INSUFFICIENT_FUNDS)
                } else {
                    true
                }
            }.ignoreElement()

    private fun validateSufficientGas(pendingTx: PendingTx): Completable =
        Singles.zip(
            getEthAccountBalance(),
            absoluteFee()
        ) { balance, fee ->
            if (fee > balance) {
                throw TransactionValidationError(TransactionValidationError.INSUFFICIENT_GAS)
            } else {
                true
            }
        }.ignoreElement()

    private fun validateNoPendingTx() =
        ethDataManager.isLastTxPending()
            .flatMapCompletable { hasUnconfirmed: Boolean ->
                if (hasUnconfirmed) {
                    Completable.error(TransactionValidationError(TransactionValidationError.HAS_TX_IN_FLIGHT))
                } else {
                    Completable.complete()
                }
            }

    override fun executeTransaction(secondPassword: String): Single<String> =
        createTransaction(pendingTx)
            .flatMap {
                ethDataManager.signEthTransaction(it, secondPassword)
            }
            .flatMap { ethDataManager.pushTx(it) }
            .flatMap { ethDataManager.setLastTxHashNowSingle(it) }
            .doOnSuccess { hash ->
                pendingTx.getOption<TxOptionValue.TxTextOption>(TxOption.DESCRIPTION)?.let { notes ->
                    ethDataManager.updateErc20TransactionNotes(hash, notes.text)
                }
            }

    private fun createTransaction(pendingTx: PendingTx): Single<RawTransaction> =
        Singles.zip(
            ethDataManager.getNonce(),
            feeOptions()
        ).map { (nonce, fees) ->
            erc20Account.createTransaction(
                nonce = nonce,
                to = sendTarget.address,
                contractAddress = erc20Account.contractAddress,
                gasPriceWei = fees.gasPrice,
                gasLimitGwei = fees.gasLimitGwei,
                amount = pendingTx.amount.toBigInteger()
            )
        }

    private val FeeOptions.gasPrice: BigInteger
        get() = Convert.toWei(
            BigDecimal.valueOf(regularFee),
            Convert.Unit.GWEI
        ).toBigInteger()

    private val FeeOptions.gasLimitGwei: BigInteger
        get() = BigInteger.valueOf(
            gasLimitContract
        )
}
