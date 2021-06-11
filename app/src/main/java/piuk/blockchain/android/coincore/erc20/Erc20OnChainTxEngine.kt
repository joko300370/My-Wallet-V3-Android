package piuk.blockchain.android.coincore.erc20

import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.android.ui.transactionflow.flow.FeeInfo
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import java.math.BigDecimal
import java.math.BigInteger

open class Erc20OnChainTxEngine(
    private val ethDataManager: EthDataManager,
    private val feeManager: FeeDataManager,
    walletPreferences: WalletStatus,
    requireSecondPassword: Boolean
) : OnChainTxEngineBase(
    requireSecondPassword,
    walletPreferences
) {

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = CryptoValue.zero(sourceAsset),
                totalBalance = CryptoValue.zero(sourceAsset),
                availableBalance = CryptoValue.zero(sourceAsset),
                feeForFullAvailable = CryptoValue.zero(CryptoCurrency.ETHER),
                feeAmount = CryptoValue.zero(CryptoCurrency.ETHER),
                feeSelection = FeeSelection(
                    selectedLevel = mapSavedFeeToFeeLevel(fetchDefaultFeeLevel(sourceAsset)),
                    availableLevels = AVAILABLE_FEE_LEVELS,
                    asset = CryptoCurrency.ETHER
                ),
                selectedFiat = userFiat
            )
        )

    private fun buildConfirmationTotal(pendingTx: PendingTx): TxConfirmationValue.NewTotal {
        val fiatAmount = pendingTx.amount.toFiat(exchangeRates, userFiat) as FiatValue

        return TxConfirmationValue.NewTotal(
            totalWithFee = pendingTx.amount,
            exchange = fiatAmount
        )
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                confirmations = listOfNotNull(
                    TxConfirmationValue.NewFrom(sourceAccount, sourceAsset),
                    TxConfirmationValue.NewTo(
                        txTarget, AssetAction.Send, sourceAccount
                    ),
                    TxConfirmationValue.CompoundNetworkFee(
                        sendingFeeInfo = if (!pendingTx.feeAmount.isZero) {
                            FeeInfo(
                                pendingTx.feeAmount,
                                pendingTx.feeAmount.toFiat(exchangeRates, userFiat),
                                sourceAsset
                            )
                        } else null,
                        feeLevel = pendingTx.feeSelection.selectedLevel
                    ),
                    buildConfirmationTotal(pendingTx),
                    TxConfirmationValue.Description()
                )
            )
        )

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

    override fun makeFeeSelectionOption(pendingTx: PendingTx): TxConfirmationValue.FeeSelection =
        TxConfirmationValue.FeeSelection(
            feeDetails = getFeeState(pendingTx),
            exchange = pendingTx.feeAmount.toFiat(exchangeRates, userFiat),
            availableLevels = AVAILABLE_FEE_LEVELS,
            selectedLevel = pendingTx.feeSelection.selectedLevel,
            asset = sourceAsset
        )

    private fun feeOptions(): Single<FeeOptions> =
        feeManager.getErc20FeeOptions(ethDataManager.erc20ContractAddress(sourceAsset))
            .singleOrError()

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == sourceAsset)
        return Singles.zip(
            sourceAccount.accountBalance.map { it as CryptoValue },
            sourceAccount.actionableBalance.map { it as CryptoValue },
            absoluteFee(pendingTx.feeSelection.selectedLevel)
        ) { total, available, fee ->
            pendingTx.copy(
                amount = amount,
                totalBalance = total,
                availableBalance = available,
                feeForFullAvailable = fee,
                feeAmount = fee
            )
        }
    }

    // In an ideal world, we'd get this via a CryptoAccount object.
    // However accessing one for Eth here would break the abstractions, so:
    private fun getEthAccountBalance(): Single<Money> =
        ethDataManager.fetchEthAddress()
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }
            .map { it }

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
            if (pendingTx.amount <= CryptoValue.zero(sourceAsset)) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        sourceAccount.actionableBalance
            .map { balance ->
                if (pendingTx.amount > balance) {
                    throw TxValidationFailure(
                        ValidationState.INSUFFICIENT_FUNDS
                    )
                } else {
                    true
                }
            }.ignoreElement()

    private fun validateSufficientGas(pendingTx: PendingTx): Completable =
        Singles.zip(
            getEthAccountBalance(),
            absoluteFee(pendingTx.feeSelection.selectedLevel)
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
                    Completable.error(
                        TxValidationFailure(
                            ValidationState.HAS_TX_IN_FLIGHT
                        )
                    )
                } else {
                    Completable.complete()
                }
            }

    override fun doExecute(
        pendingTx: PendingTx,
        secondPassword: String
    ): Single<TxResult> =
        createTransaction(pendingTx)
            .flatMap {
                ethDataManager.signEthTransaction(
                    it,
                    secondPassword
                )
            }
            .flatMap { ethDataManager.pushTx(it) }
            .flatMap { ethDataManager.setLastTxHashNowSingle(it) }
            .flatMap { hash ->
                pendingTx.getOption<TxConfirmationValue.Description>(
                    TxConfirmation.DESCRIPTION
                )?.let { notes ->
                    ethDataManager.updateErc20TransactionNotes(
                        hash, notes.text, sourceAsset
                    )
                }?.toSingle {
                    hash
                } ?: Single.just(hash)
            }.onErrorResumeNext {
                Single.error(TransactionError.ExecutionFailed)
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

    private fun createTransaction(pendingTx: PendingTx): Single<RawTransaction> {
        val tgt = txTarget as CryptoAddress

        return Singles.zip(
            ethDataManager.getNonce(),
            feeOptions()
        ).map { (nonce, fees) ->
            ethDataManager.createErc20Transaction(
                nonce = nonce,
                to = tgt.address,
                contractAddress = ethDataManager.erc20ContractAddress(
                    sourceAsset
                ),
                gasPriceWei = fees.gasPrice(
                    pendingTx.feeSelection.selectedLevel
                ),
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

    private
    val FeeOptions.gasLimitGwei: BigInteger
        get() = BigInteger.valueOf(
            gasLimitContract
        )

    companion object {
        private val AVAILABLE_FEE_LEVELS =
            setOf(FeeLevel.Regular, FeeLevel.Priority)
    }
}
