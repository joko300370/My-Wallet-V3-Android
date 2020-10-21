package piuk.blockchain.android.coincore.btc

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.WalletStatus
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Transaction
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransferError
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.txEngine.BitPayClientEngine
import piuk.blockchain.android.coincore.impl.txEngine.EngineTransaction
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.copyAndPut
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigInteger

private const val STATE_UTXO = "btc_utxo"

private val PendingTx.utxoBundle: SpendableUnspentOutputs
    get() = (this.engineState[STATE_UTXO] as? SpendableUnspentOutputs) ?: SpendableUnspentOutputs()

private class BtcPreparedTx(
    val btcTx: Transaction
) : EngineTransaction {
    override val encodedMsg: String
        get() = String(Hex.encode(btcTx.bitcoinSerialize()))
    override val msgSize: Int = btcTx.messageSize
    override val txHash: String = btcTx.hashAsString
}

class BtcOnChainTxEngine(
    private val btcDataManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val feeDataManager: FeeDataManager,
    private val btcNetworkParams: NetworkParameters,
    walletPreferences: WalletStatus,
    requireSecondPassword: Boolean
) : OnChainTxEngineBase(
    requireSecondPassword,
    walletPreferences
), BitPayClientEngine, KoinComponent {

    override fun assertInputsValid() {
        require(sourceAccount is BtcCryptoWalletAccount)
        require(txTarget is CryptoAddress)
        require((txTarget as CryptoAddress).asset == CryptoCurrency.BTC)
        require(asset == CryptoCurrency.BTC)
    }

    private val btcTarget: CryptoAddress by unsafeLazy {
        txTarget as CryptoAddress
    }

    private val btcSource: BtcCryptoWalletAccount by unsafeLazy {
        sourceAccount as BtcCryptoWalletAccount
    }

    private val sourceAddress: String by unsafeLazy {
        if (btcSource.isHDAccount) {
            (btcSource.internalAccount as Account).xpub
        } else {
            (btcSource.internalAccount as LegacyAddress).address
        }
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = CryptoValue.ZeroBtc,
                available = CryptoValue.ZeroBtc,
                fees = CryptoValue.ZeroBtc,
                feeLevel = mapSavedFeeToFeeLevel(getFeeType(CryptoCurrency.BTC)),
                selectedFiat = userFiat
            )
        )

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        Singles.zip(
            getDynamicFeePerKb(pendingTx),
            getUnspentApiResponse(sourceAddress)
        ).map { (feePerKb, coins) ->
            updatePendingTxFromAmount(
                amount as CryptoValue,
                pendingTx,
                feePerKb,
                coins
            )
        }

    private fun getUnspentApiResponse(address: String): Single<UnspentOutputs> =
        if (btcDataManager.getAddressBalance(address) > CryptoValue.ZeroBtc) {
            sendDataManager.getUnspentBtcOutputs(address)
                // If we get here, we should have balance... but if we have no UTXOs then we have
                // a problem:
                .map { utxo ->
                    if (utxo.unspentOutputs.isEmpty()) {
                        throw fatalError(IllegalStateException("No BTC UTXOs found for non-zero balance"))
                    } else {
                        utxo
                    }
                }
                .singleOrError()
        } else {
            Single.error(Throwable("No BTC funds"))
        }

    private fun getDynamicFeePerKb(pendingTx: PendingTx): Single<CryptoValue> =
        feeDataManager.btcFeeOptions
            .map { feeOptions ->
                when (pendingTx.feeLevel) {
                    FeeLevel.Regular -> feeToCrypto(feeOptions.regularFee)
                    FeeLevel.None -> CryptoValue.ZeroBtc
                    FeeLevel.Priority -> feeToCrypto(feeOptions.priorityFee)
                    FeeLevel.Custom -> TODO() // feeToCrypto(view!!.getCustomFeeValue())
                }
            }.singleOrError()

    private fun feeToCrypto(feePerKb: Long): CryptoValue =
        CryptoValue.fromMinor(CryptoCurrency.BTC, (feePerKb * 1000).toBigInteger())

    private fun updatePendingTxFromAmount(
        amount: CryptoValue,
        pendingTx: PendingTx,
        feePerKb: CryptoValue,
        coins: UnspentOutputs
    ): PendingTx {
        val sweepBundle = sendDataManager.getMaximumAvailable(
            cryptoCurrency = CryptoCurrency.BTC,
            unspentCoins = coins,
            feePerKb = feePerKb.toBigInteger(),
            useNewCoinSelection = true
        )

        val maxAvailable = sweepBundle.left

        val utxoBundle = sendDataManager.getSpendableCoins(
            unspentCoins = coins,
            paymentAmount = amount,
            feePerKb = feePerKb.toBigInteger(),
            useNewCoinSelection = true
        )

        return pendingTx.copy(
            amount = amount,
            available = CryptoValue.fromMinor(CryptoCurrency.BTC, maxAvailable),
            fees = CryptoValue.fromMinor(CryptoCurrency.BTC, utxoBundle.absoluteFee),
            engineState = pendingTx.engineState.copyAndPut(STATE_UTXO, utxoBundle)
        )
    }

    override fun doOptionUpdateRequest(pendingTx: PendingTx, newOption: TxOptionValue): Single<PendingTx> =
        if (newOption is TxOptionValue.FeeSelection) {
            if (newOption.selectedLevel != pendingTx.feeLevel) {
                updateFeeSelection(CryptoCurrency.BTC, pendingTx, newOption)
            } else {
                super.doOptionUpdateRequest(pendingTx, makeFeeSelectionOption(pendingTx))
            }
        } else {
            super.doOptionUpdateRequest(pendingTx, newOption)
        }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .updateTxValidity(pendingTx)

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                options = mutableListOf(
                    TxOptionValue.From(from = sourceAccount.label),
                    TxOptionValue.To(to = txTarget.label),
                    makeFeeSelectionOption(pendingTx),
                    TxOptionValue.FeedTotal(
                        amount = pendingTx.amount,
                        fee = pendingTx.fees,
                        exchangeFee = pendingTx.fees.toFiat(exchangeRates, userFiat),
                        exchangeAmount = pendingTx.amount.toFiat(exchangeRates, userFiat)
                    ),
                    TxOptionValue.Description()
                ).apply {
                    if (isLargeTransaction(pendingTx)) {
                        add(TxOptionValue.TxBooleanOption<Unit>(TxOption.LARGE_TRANSACTION_WARNING))
                    }
                }.toList()
            )
        )

    private fun makeFeeSelectionOption(pendingTx: PendingTx): TxOptionValue.FeeSelection =
        TxOptionValue.FeeSelection(
            feeDetails = getFeeState(pendingTx.fees, pendingTx.amount, pendingTx.available),
            exchange = pendingTx.fees.toFiat(exchangeRates, userFiat),
            selectedLevel = pendingTx.feeLevel,
            availableLevels = setOf(
                FeeLevel.Regular, FeeLevel.Priority
            )
        )

    // Returns true if bitcoin transaction is large by checking against 3 criteria:
    //  * If the fee > $0.50 AND
    //  * the Tx size is over 1kB AND
    //  * the ratio of fee/amount is over 1%
    private fun isLargeTransaction(pendingTx: PendingTx): Boolean {
        val fiatValue = pendingTx.fees.toFiat(exchangeRates, LARGE_TX_FIAT)

        val txSize = sendDataManager.estimateSize(
            inputs = pendingTx.utxoBundle.spendableOutputs.size,
            outputs = 2 // assumes change required
        )

        val relativeFee = 100.toBigDecimal() * (pendingTx.fees.toBigDecimal() / pendingTx.amount.toBigDecimal())
        return fiatValue.toBigDecimal() > LARGE_TX_FEE.toBigDecimal() &&
                txSize > LARGE_TX_SIZE &&
                relativeFee > LARGE_TX_PERCENTAGE
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAddress()
            .then { validateAmounts(pendingTx) }
            .then { validateSufficientFunds(pendingTx) }
            .then { validateOptions(pendingTx) }
            .logValidityFailure()
            .updateTxValidity(pendingTx)

    private fun validateAddress(): Completable =
        Completable.fromCallable {
            if (!FormatsUtil.isValidBitcoinAddress(btcNetworkParams, btcTarget.address)) {
                throw TxValidationFailure(ValidationState.INVALID_ADDRESS)
            }
        }

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            val amount = pendingTx.amount.toBigInteger()
            if (amount < Payment.DUST) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }

            if (amount > maxBTCAmount.toBigInteger()) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }

            if (amount <= BigInteger.ZERO) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.available < pendingTx.amount) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }

            if (pendingTx.utxoBundle.spendableOutputs.isEmpty()) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }
        }

    private fun validateOptions(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            // If the large_fee warning is present, make sure it's ack'd.
            // If it's not, then there's nothing to do
            if (pendingTx.getOption<TxOptionValue.TxBooleanOption<Unit>>(
                    TxOption.LARGE_TRANSACTION_WARNING)?.value == false
            ) {
                throw TxValidationFailure(ValidationState.OPTION_INVALID)
            }
        }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        doPrepareTransaction(pendingTx, secondPassword)
            .flatMap { engineTx ->
                val btcTx = engineTx as BtcPreparedTx
                sendDataManager.submitBtcPayment(btcTx.btcTx)
            }.doOnSuccess {
                doOnTransactionSuccess(pendingTx)
            }.doOnError { e ->
                doOnTransactionFailed(pendingTx, e)
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

    override fun doPrepareTransaction(
        pendingTx: PendingTx,
        secondPassword: String
    ): Single<EngineTransaction> =
        Singles.zip(
            getBtcChangeAddress(),
            getBtcKeys(pendingTx, secondPassword)
        ).map { (changeAddress, keys) ->
            BtcPreparedTx(
                sendDataManager.createAndSignBtcTransaction(
                    pendingTx.utxoBundle,
                    keys,
                    btcTarget.address,
                    changeAddress,
                    pendingTx.fees.toBigInteger(),
                    pendingTx.amount.toBigInteger()
                )
            )
        }

    override fun doOnTransactionSuccess(pendingTx: PendingTx) {
        incrementBtcReceiveAddress(pendingTx)
    }

    override fun doOnTransactionFailed(pendingTx: PendingTx, e: Throwable) {
        Timber.e("BTC Send failed: $e")
    }

    private fun getBtcKeys(pendingTx: PendingTx, secondPassword: String): Single<List<ECKey>> {
        if (btcSource.isHDAccount) {
            if (btcDataManager.isDoubleEncrypted) {
                btcDataManager.decryptHDWallet(secondPassword)
            }

            return Single.just(
                btcDataManager.getHDKeysForSigning(
                    account = btcSource.internalAccount as Account,
                    unspentOutputBundle = pendingTx.utxoBundle
                )
            )
        } else {
            val password = if (btcDataManager.isDoubleEncrypted) secondPassword else null
            return Single.just(
                listOf(
                    btcDataManager.getAddressECKey(
                        legacyAddress = btcSource.internalAccount as LegacyAddress,
                        secondPassword = password
                    ) ?: throw fatalError(TransferError("Private key not found for legacy BTC address"))
                )
            )
        }
    }

    private fun getBtcChangeAddress(): Single<String> {
        return if (btcSource.isHDAccount) {
            btcDataManager.getNextChangeAddress(btcSource.internalAccount as Account)
                .singleOrError()
        } else {
            Single.just((btcSource.internalAccount as LegacyAddress).address)
        }
    }

    private fun incrementBtcReceiveAddress(pendingTx: PendingTx) {
        if (btcSource.isHDAccount) {
            val account = btcSource.internalAccount as Account
            btcDataManager.incrementChangeAddress(account)
            btcDataManager.incrementReceiveAddress(account)
            updateInternalBtcBalances(pendingTx)
        }
    }

    // Update balance immediately after spend - until refresh from server
    private fun updateInternalBtcBalances(pendingTx: PendingTx) {
        try {
            val totalSent = pendingTx.totalSent.toBigInteger()
            if (btcSource.isHDAccount) {
                val account = btcSource.internalAccount as Account
                btcDataManager.subtractAmountFromAddressBalance(
                    account.xpub,
                    totalSent.toLong()
                )
            } else {
                val address = btcSource.internalAccount as LegacyAddress
                btcDataManager.subtractAmountFromAddressBalance(
                    address.address,
                    totalSent.toLong()
                )
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    companion object {
        const val LARGE_TX_FIAT = "USD"
        const val LARGE_TX_FEE = 0.5
        const val LARGE_TX_SIZE = 1024
        val LARGE_TX_PERCENTAGE = 1.0.toBigDecimal()
    }

    // TEMP diagnostics - TODO Remove this once we're stable
    private val crashLogger: CrashLogger by inject()

    private fun Completable.logValidityFailure(): Completable =
        this.doOnError { crashLogger.logException(it) }

    private fun fatalError(e: Throwable): Throwable {
        crashLogger.logException(e)
        return e
    }
}

private val PendingTx.totalSent: Money
    get() = amount + fees
