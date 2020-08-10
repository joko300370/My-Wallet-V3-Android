package piuk.blockchain.android.coincore.eth

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.balance.Money.Companion.max
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionValidationError
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.impl.OnChainSendProcessorBase
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class EthSendTransaction(
    private val ethDataManager: EthDataManager,
    private val feeManager: FeeDataManager,
    sendingAccount: CryptoAccount,
    sendTarget: CryptoAddress,
    requireSecondPassword: Boolean
) : OnChainSendProcessorBase(
        sendingAccount,
        sendTarget,
        requireSecondPassword
) {
    override val asset: CryptoCurrency = CryptoCurrency.ETHER

    override val feeOptions = setOf(FeeLevel.Regular)

    override var pendingTx: PendingTx =
        PendingTx(
            amount = CryptoValue.ZeroEth,
            available = CryptoValue.ZeroEth,
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
                    BigDecimal.valueOf(it.gasLimit * it.regularFee),
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
        ) { available, fees ->
            if (amount + fees <= available) {
                pendingTx.copy(
                    amount = amount,
                    available = max(available - fees, CryptoValue.ZeroEth) as CryptoValue,
                    fees = fees
                )
            } else {
                throw TransactionValidationError(TransactionValidationError.INSUFFICIENT_FUNDS)
            }
        }.doOnSuccess { pendingTx = it }

    // We can make some assumptions here over the previous impl;
    // 1. a CryptAddress object will be self-validating, so we need not check that it's valid
    override fun validate(): Completable =
        validateAmount(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .then { validateNoPendingTx() }
            .doOnError { Timber.e("Validation failed: $it") }

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
            ethDataManager.isContractAddress(sendTarget.address),
            feeOptions()
        ).map { (nonce, isContract, fees) ->
            ethDataManager.createEthTransaction(
                nonce = nonce,
                to = sendTarget.address,
                gasPriceWei = fees.gasPrice,
                gasLimitGwei = fees.getGasLimit(isContract),
                weiValue = pendingTx.amount.toBigInteger()
            )
        }

    // TODO: Have FeeOptions deal with this conversion
    private val FeeOptions.gasPrice: BigInteger
        get() = Convert.toWei(
            BigDecimal.valueOf(regularFee),
            Convert.Unit.GWEI
        ).toBigInteger()

    private fun FeeOptions.getGasLimit(isContract: Boolean): BigInteger =
        BigInteger.valueOf(
            if (isContract) gasLimitContract else gasLimit
        )

    private fun validateAmount(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.amount <= CryptoValue.ZeroEth) {
                throw TransactionValidationError(TransactionValidationError.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        Singles.zip(
            sendingAccount.balance,
            absoluteFee()
        ) { balance: Money, fee: Money ->
            if (fee + pendingTx.amount > balance) {
                throw TransactionValidationError(TransactionValidationError.INSUFFICIENT_FUNDS)
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
}
