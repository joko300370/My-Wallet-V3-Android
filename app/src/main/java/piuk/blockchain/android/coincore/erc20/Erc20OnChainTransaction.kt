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
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.OnChainTxProcessorBase
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import java.math.BigDecimal
import java.math.BigInteger

open class Erc20OnChainTransaction(
    private val erc20Account: Erc20Account,
    private val feeManager: FeeDataManager,
    exchangeRates: ExchangeRateDataManager,
    sendingAccount: Erc20NonCustodialAccount,
    sendTarget: CryptoAddress,
    requireSecondPassword: Boolean
) : OnChainTxProcessorBase(
    sendingAccount,
    sendTarget,
    exchangeRates,
    requireSecondPassword
) {
    private val ethDataManager: EthDataManager =
        erc20Account.ethDataManager

    override val feeOptions = setOf(FeeLevel.Regular)

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = CryptoValue.zero(asset),
                available = CryptoValue.zero(asset),
                fees = CryptoValue.ZeroEth,
                feeLevel = FeeLevel.Regular,
                selectedFiat = userFiat
            )
        )

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(options = listOf(
                TxOptionValue.From(from = sendingAccount.label),
                TxOptionValue.To(to = sendTarget.label),
                TxOptionValue.Fee(fee = pendingTx.fees, exchange = pendingTx.fees.toFiat(exchangeRates, userFiat)),
                TxOptionValue.FeedTotal(
                    amount = pendingTx.amount,
                    fee = pendingTx.fees,
                    exchangeFee = pendingTx.fees.toFiat(exchangeRates, userFiat),
                    exchangeAmount = pendingTx.amount.toFiat(exchangeRates, userFiat)
                ),
                TxOptionValue.Description()
            )))

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

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == asset)

        return Singles.zip(
            sendingAccount.accountBalance.map { it as CryptoValue },
            absoluteFee()
        ) { available, fee ->
            pendingTx.copy(
                amount = amount,
                available = available,
                fees = fee
            )
        }
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
        require(sendTarget is CryptoAddress)

        return ethDataManager.isContractAddress(sendTarget.address)
            .map { isContract ->
                if (isContract || sendTarget !is Erc20Address) {
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
        sendingAccount.accountBalance
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
            absoluteFee()
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

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Completable =
        createTransaction(pendingTx)
            .flatMap {
                ethDataManager.signEthTransaction(it, secondPassword)
            }
            .flatMap { ethDataManager.pushTx(it) }
            .flatMap { ethDataManager.setLastTxHashNowSingle(it) }
            .flatMapCompletable { hash ->
                pendingTx.getOption<TxOptionValue.Description>(TxOption.DESCRIPTION)?.let { notes ->
                    ethDataManager.updateErc20TransactionNotes(hash, notes.text)
                } ?: Completable.complete()
            }

    private fun createTransaction(pendingTx: PendingTx): Single<RawTransaction> {
        require(sendTarget is CryptoAddress)

        return Singles.zip(
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
