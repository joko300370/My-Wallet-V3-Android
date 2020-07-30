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
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.SendValidationError
import piuk.blockchain.android.coincore.impl.OnChainSendProcessorBase
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class Erc20SendTransaction(
    override val asset: CryptoCurrency,
    private val erc20Account: Erc20Account,
    private val feeManager: FeeDataManager,
    sendingAccount: Erc20NonCustodialAccount,
    sendTarget: CryptoAddress,
    requireSecondPassword: Boolean
) : OnChainSendProcessorBase(
        sendingAccount,
        sendTarget,
        requireSecondPassword
) {
    private val ethDataManager: EthDataManager =
        erc20Account.ethDataManager

    override val feeOptions = setOf(FeeLevel.Regular)

    override val isNoteSupported: Boolean = true

    override fun absoluteFee(pendingTx: PendingSendTx): Single<Money> =
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

    override fun availableBalance(pendingTx: PendingSendTx): Single<Money> =
        sendingAccount.balance

    // In an ideal world, we'd get this via a CryptoAccount object.
    // However accessing one for Eth here would break the abstractions, so:
    private fun getEthAccountBalance(): Single<Money> =
        ethDataManager.fetchEthAddress()
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }
            .map { it as Money }

    override fun validate(pendingTx: PendingSendTx): Completable =
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
                    throw SendValidationError(SendValidationError.INVALID_ADDRESS)
                } else {
                    isContract
                }
            }.ignoreElement()

    private fun validateAmount(pendingTx: PendingSendTx): Completable =
        Completable.fromCallable {
            if (pendingTx.amount <= CryptoValue.zero(asset)) {
                throw SendValidationError(SendValidationError.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingSendTx): Completable =
        sendingAccount.balance
            .map { balance ->
                if (pendingTx.amount > balance) {
                    throw SendValidationError(SendValidationError.INSUFFICIENT_FUNDS)
                } else {
                    true
                }
            }.ignoreElement()

    private fun validateSufficientGas(pendingTx: PendingSendTx): Completable =
        Singles.zip(
            getEthAccountBalance(),
            absoluteFee(pendingTx)
        ) { balance, fee ->
            if (fee > balance) {
                throw SendValidationError(SendValidationError.INSUFFICIENT_GAS)
            } else {
                true
            }
        }.ignoreElement()

    private fun validateNoPendingTx() =
        ethDataManager.isLastTxPending()
            .flatMapCompletable { hasUnconfirmed: Boolean ->
                if (hasUnconfirmed) {
                    Completable.error(SendValidationError(SendValidationError.HAS_TX_IN_FLIGHT))
                } else {
                    Completable.complete()
                }
            }

    override fun executeTransaction(pendingTx: PendingSendTx, secondPassword: String): Single<String> =
        createTransaction(pendingTx)
            .flatMap {
                ethDataManager.signEthTransaction(it, secondPassword)
            }
            .flatMap { ethDataManager.pushTx(it) }
            .flatMap { ethDataManager.setLastTxHashNowSingle(it) }
            .doOnSuccess { ethDataManager.updateErc20TransactionNotes(it, pendingTx.notes) }

    private fun createTransaction(pendingTx: PendingSendTx): Single<RawTransaction> =
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
