package piuk.blockchain.android.coincore.erc20

import com.blockchain.preferences.WalletStatus
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
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import java.math.BigDecimal
import java.math.BigInteger

open class Erc20OnChainTxEngine(
    private val erc20Account: Erc20Account,
    private val feeManager: FeeDataManager,
    walletPreferences: WalletStatus,
    requireSecondPassword: Boolean
) : OnChainTxEngineBase(
    requireSecondPassword,
    walletPreferences
) {
    private val ethDataManager: EthDataManager =
        erc20Account.ethDataManager

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = CryptoValue.zero(asset),
                available = CryptoValue.zero(asset),
                fees = CryptoValue.ZeroEth,
                feeLevel = mapSavedFeeToFeeLevel(getFeeType(erc20Account.cryptoCurrency)),
                selectedFiat = userFiat
            )
        )

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(options = listOf(
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
            )))

    private fun absoluteFee(feeLevel: FeeLevel): Single<CryptoValue> =
        feeOptions().map {
            CryptoValue.fromMinor(
                CryptoCurrency.ETHER,
                Convert.toWei(
                    BigDecimal.valueOf(it.gasLimitContract * it.mapFeeLevel(feeLevel)),
                    Convert.Unit.GWEI
                )
            )
        }

    private fun FeeOptions.mapFeeLevel(feeLevel: FeeLevel) =
        when (feeLevel) {
            FeeLevel.None -> 0L
            FeeLevel.Regular -> regularFee
            FeeLevel.Priority,
            FeeLevel.Custom -> priorityFee
        }

    private fun makeFeeSelectionOption(pendingTx: PendingTx): TxOptionValue.FeeSelection =
        TxOptionValue.FeeSelection(
            feeDetails = getFeeState(pendingTx.fees, pendingTx.amount, pendingTx.available),
            exchange = pendingTx.fees.toFiat(exchangeRates, userFiat),
            selectedLevel = pendingTx.feeLevel,
            availableLevels = setOf(FeeLevel.Regular, FeeLevel.Priority)
        )

    private fun feeOptions(): Single<FeeOptions> =
        feeManager.ethFeeOptions.singleOrError()

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == asset)

        return Singles.zip(
            sourceAccount.actionableBalance.map { it as CryptoValue },
            absoluteFee(pendingTx.feeLevel)
        ) { available, fee ->
            pendingTx.copy(
                amount = amount,
                available = available,
                fees = fee
            )
        }
    }

    override fun doOptionUpdateRequest(pendingTx: PendingTx, newOption: TxOptionValue): Single<PendingTx> =
        if (newOption is TxOptionValue.FeeSelection) {
            if (newOption.selectedLevel != pendingTx.feeLevel) {
                updateFeeSelection(erc20Account.cryptoCurrency, pendingTx, newOption)
            } else {
                super.doOptionUpdateRequest(pendingTx, makeFeeSelectionOption(pendingTx))
            }
        } else {
            super.doOptionUpdateRequest(pendingTx, newOption)
        }

    // In an ideal world, we'd get this via a CryptoAccount object.
    // However accessing one for Eth here would break the abstractions, so:
    private fun getEthAccountBalance(): Single<Money> =
        ethDataManager.fetchEthAddress()
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }
            .map { it as Money }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .then { validateSufficientGas(pendingTx) }
            .updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAddresses()
            .then { validateAmounts(pendingTx) }
            .then { validateSufficientFunds(pendingTx) }
            .then { validateSufficientGas(pendingTx) }
            .then { validateNoPendingTx() }
            .updateTxValidity(pendingTx)

    // This should have already been checked, but we'll check again because
    // burning tokens by sending them to the contract address is probably not what we
    // want to do
    private fun validateAddresses(): Completable {
        val tgt = txTarget as CryptoAddress

        return ethDataManager.isContractAddress(tgt.address)
            .map { isContract ->
                if (isContract || tgt !is Erc20Address) {
                    throw TxValidationFailure(ValidationState.INVALID_ADDRESS)
                } else {
                    isContract
                }
            }.ignoreElement()
    }

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.amount <= CryptoValue.zero(asset)) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        sourceAccount.actionableBalance
            .map { balance ->
                if (pendingTx.amount > balance) {
                    throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
                } else {
                    true
                }
            }.ignoreElement()

    private fun validateSufficientGas(pendingTx: PendingTx): Completable =
        Singles.zip(
            getEthAccountBalance(),
            absoluteFee(pendingTx.feeLevel)
        ) { balance, fee ->
            if (fee > balance) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_GAS)
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

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createTransaction(pendingTx)
            .flatMap {
                ethDataManager.signEthTransaction(it, secondPassword)
            }
            .flatMap { ethDataManager.pushTx(it) }
            .flatMap { ethDataManager.setLastTxHashNowSingle(it) }
            .flatMap { hash ->
                pendingTx.getOption<TxOptionValue.Description>(TxOption.DESCRIPTION)?.let { notes ->
                    ethDataManager.updateErc20TransactionNotes(hash, notes.text)
                }?.toSingle {
                    hash
                } ?: Single.just(hash)
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

    private fun createTransaction(pendingTx: PendingTx): Single<RawTransaction> {
        val tgt = txTarget as CryptoAddress

        return Singles.zip(
            ethDataManager.getNonce(),
            feeOptions()
        ).map { (nonce, fees) ->
            erc20Account.createTransaction(
                nonce = nonce,
                to = tgt.address,
                contractAddress = erc20Account.contractAddress,
                gasPriceWei = fees.gasPrice(pendingTx.feeLevel),
                gasLimitGwei = fees.gasLimitGwei,
                amount = pendingTx.amount.toBigInteger()
            )
        }
    }

    private fun FeeOptions.gasPrice(feeLevel: FeeLevel): BigInteger =
        Convert.toWei(
            BigDecimal.valueOf(this.mapFeeLevel(feeLevel)),
            Convert.Unit.GWEI
        ).toBigInteger()

    private val FeeOptions.gasLimitGwei: BigInteger
        get() = BigInteger.valueOf(
            gasLimitContract
        )
}
