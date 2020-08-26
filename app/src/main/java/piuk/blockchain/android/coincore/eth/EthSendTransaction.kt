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
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.OnChainTxProcessorBase
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import java.math.BigDecimal
import java.math.BigInteger

open class EthSendTransaction(
    private val ethDataManager: EthDataManager,
    private val feeManager: FeeDataManager,
    exchangeRates: ExchangeRateDataManager,
    sendingAccount: CryptoAccount,
    sendTarget: CryptoAddress,
    requireSecondPassword: Boolean
) : OnChainTxProcessorBase(
        sendingAccount,
        sendTarget,
        exchangeRates,
        requireSecondPassword
) {
    init {
        require(asset == CryptoCurrency.ETHER)
    }

    override val feeOptions = setOf(FeeLevel.Regular)

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
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

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == CryptoCurrency.ETHER)

        return Singles.zip(
            sendingAccount.accountBalance.map { it as CryptoValue },
            absoluteFee()
        ) { available, fees ->
            pendingTx.copy(
                amount = amount,
                available = max(available - fees, CryptoValue.ZeroEth) as CryptoValue,
                fees = fees
            )
        }
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .updateTxValidity(pendingTx)

    // We can make some assumptions here over the previous impl;
    // 1. a CryptAddress object will be self-validating, so we need not check that it's valid
    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .then { validateNoPendingTx() }
            .updateTxValidity(pendingTx)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Completable =
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
            }.ignoreElement()

    private fun createTransaction(pendingTx: PendingTx): Single<RawTransaction> {
        require(sendTarget is CryptoAddress)

        return Singles.zip(
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

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.amount <= CryptoValue.ZeroEth) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        Singles.zip(
            sendingAccount.accountBalance,
            absoluteFee()
        ) { balance: Money, fee: Money ->
            if (fee + pendingTx.amount > balance) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            } else {
                true
            }
        }.ignoreElement()

    private fun validateNoPendingTx() =
        ethDataManager.isLastTxPending()
            .flatMapCompletable { hasUnconfirmed: Boolean ->
                if (hasUnconfirmed) {
                    Completable.error(TxValidationFailure(ValidationState.HAS_TX_IN_FLIGHT))
                } else {
                    Completable.complete()
                }
            }
}
