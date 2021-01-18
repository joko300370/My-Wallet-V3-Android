package piuk.blockchain.android.coincore.eth

import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.preferences.WalletStatus
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
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import java.math.BigDecimal
import java.math.BigInteger

open class EthOnChainTxEngine(
    private val ethDataManager: EthDataManager,
    private val feeManager: FeeDataManager,
    walletPreferences: WalletStatus,
    requireSecondPassword: Boolean
) : OnChainTxEngineBase(
    requireSecondPassword,
    walletPreferences
) {
    override fun assertInputsValid() {
        require(txTarget is CryptoAddress)
        require((txTarget as CryptoAddress).asset == CryptoCurrency.ETHER)
        require(asset == CryptoCurrency.ETHER)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = CryptoValue.ZeroEth,
                available = CryptoValue.ZeroEth,
                fees = CryptoValue.ZeroEth,
                feeLevel = mapSavedFeeToFeeLevel(getFeeType(CryptoCurrency.ETHER)),
                selectedFiat = userFiat
            )
        )

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(confirmations = listOf(
                TxConfirmationValue.From(from = sourceAccount.label),
                TxConfirmationValue.To(to = txTarget.label),
                makeFeeSelectionOption(pendingTx),
                TxConfirmationValue.FeedTotal(
                    amount = pendingTx.amount,
                    fee = pendingTx.fees,
                    exchangeFee = pendingTx.fees.toFiat(exchangeRates, userFiat),
                    exchangeAmount = pendingTx.amount.toFiat(exchangeRates, userFiat)
                ),
                TxConfirmationValue.Description()
            )))

    private fun absoluteFee(feeLevel: FeeLevel): Single<CryptoValue> =
        feeOptions().map {
            CryptoValue.fromMinor(
                CryptoCurrency.ETHER,
                Convert.toWei(
                    BigDecimal.valueOf(it.gasLimit * it.mapFeeLevel(feeLevel)),
                    Convert.Unit.GWEI
                )
            )
        }

    private fun FeeOptions.mapFeeLevel(feeLevel: FeeLevel) =
        when (feeLevel) {
            FeeLevel.None -> 0L
            FeeLevel.Regular -> regularFee
            FeeLevel.Priority -> priorityFee
            FeeLevel.Custom -> priorityFee
        }

    private fun makeFeeSelectionOption(pendingTx: PendingTx): TxConfirmationValue.FeeSelection =
        TxConfirmationValue.FeeSelection(
            feeDetails = getFeeState(pendingTx),
            exchange = pendingTx.fees.toFiat(exchangeRates, userFiat),
            selectedLevel = pendingTx.feeLevel,
            availableLevels = setOf(FeeLevel.Regular, FeeLevel.Priority),
            asset = sourceAccount.asset
        )

    private fun feeOptions(): Single<FeeOptions> =
        feeManager.ethFeeOptions.singleOrError()

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == CryptoCurrency.ETHER)

        return Singles.zip(
            sourceAccount.actionableBalance.map { it as CryptoValue },
            absoluteFee(pendingTx.feeLevel)
        ) { available, fees ->
            pendingTx.copy(
                amount = amount,
                available = max(available - fees, CryptoValue.ZeroEth) as CryptoValue,
                fees = fees
            )
        }
    }

    override fun doOptionUpdateRequest(pendingTx: PendingTx, newConfirmation: TxConfirmationValue): Single<PendingTx> =
        if (newConfirmation is TxConfirmationValue.FeeSelection) {
            if (newConfirmation.selectedLevel != pendingTx.feeLevel) {
                updateFeeSelection(CryptoCurrency.ETHER, pendingTx, newConfirmation)
            } else {
                super.doOptionUpdateRequest(pendingTx, makeFeeSelectionOption(pendingTx))
            }
        } else {
            super.doOptionUpdateRequest(pendingTx, newConfirmation)
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

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createTransaction(pendingTx)
            .flatMap {
                ethDataManager.signEthTransaction(it, secondPassword)
            }
            .flatMap { ethDataManager.pushTx(it) }
            .flatMap { ethDataManager.setLastTxHashNowSingle(it) }
            .flatMap { hash ->
                pendingTx.getOption<TxConfirmationValue.Description>(TxConfirmation.DESCRIPTION)?.let { notes ->
                    ethDataManager.updateTransactionNotes(hash, notes.text)
                }?.toSingle {
                    hash
                } ?: Single.just(hash)
            }.onErrorResumeNext {
                Single.error(TransactionError.ExecutionFailed)
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

    private fun createTransaction(pendingTx: PendingTx): Single<RawTransaction> {
        val targetAddress = txTarget as CryptoAddress

        return Singles.zip(
            ethDataManager.getNonce(),
            ethDataManager.isContractAddress(targetAddress.address),
            feeOptions()
        ).map { (nonce, isContract, fees) ->
            ethDataManager.createEthTransaction(
                nonce = nonce,
                to = targetAddress.address,
                gasPriceWei = fees.gasPrice(pendingTx.feeLevel),
                gasLimitGwei = fees.getGasLimit(isContract),
                weiValue = pendingTx.amount.toBigInteger()
            )
        }
    }

    // TODO: Have FeeOptions deal with this conversion
    private fun FeeOptions.gasPrice(feeLevel: FeeLevel): BigInteger =
        Convert.toWei(
            BigDecimal.valueOf(this.mapFeeLevel(feeLevel)),
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
            sourceAccount.actionableBalance,
            absoluteFee(pendingTx.feeLevel)
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
