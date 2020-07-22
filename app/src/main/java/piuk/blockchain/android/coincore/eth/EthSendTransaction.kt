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
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.SendValidationError
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
    address: CryptoAddress,
    requireSecondPassword: Boolean
) : OnChainSendProcessorBase(
        sendingAccount,
        address,
        requireSecondPassword
) {
    override val asset: CryptoCurrency = CryptoCurrency.ETHER

    override val feeOptions = setOf(FeeLevel.Regular)

    override fun absoluteFee(pendingTx: PendingSendTx): Single<Money> =
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

    override fun availableBalance(pendingTx: PendingSendTx): Single<Money> =
        Singles.zip(
            sendingAccount.balance,
            absoluteFee(pendingTx)
        ) { balance: Money, fees: Money ->
            max(balance - fees, CryptoValue.ZeroEth)
        }

    // We can make some assumptions here over the previous impl;
    // 1. a CryptAddress object will be self-validating, so we need not check that it's valid
    override fun validate(pendingTx: PendingSendTx): Completable =
        validateAmount(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .then { validateNoPendingTx() }
            .doOnError { Timber.e("Validation failed: $it") }

    override fun executeTransaction(pendingTx: PendingSendTx, secondPassword: String): Single<String> =
        createTransaction(pendingTx)
            .flatMap {
                ethDataManager.signEthTransaction(it, secondPassword)
            }
            .flatMap { ethDataManager.pushTx(it) }
            .flatMap { ethDataManager.setLastTxHashNowSingle(it) }
            .doOnSuccess { ethDataManager.updateTransactionNotes(it, pendingTx.notes) }

    override fun isNoteSupported(): Single<Boolean> = Single.just(true)

    private fun createTransaction(pendingTx: PendingSendTx): Single<RawTransaction> =
        Singles.zip(
            ethDataManager.getNonce(),
            ethDataManager.isContractAddress(address.address),
            feeOptions()
        ).map { (nonce, isContract, fees) ->
            ethDataManager.createEthTransaction(
                nonce = nonce,
                to = address.address,
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

    private fun validateAmount(pendingTx: PendingSendTx): Completable =
        Completable.fromCallable {
            if (pendingTx.amount <= CryptoValue.ZeroEth) {
                throw SendValidationError(SendValidationError.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingSendTx): Completable =
        Singles.zip(
            sendingAccount.balance,
            absoluteFee(pendingTx)
        ) { balance: Money, fee: Money ->
            if (fee + pendingTx.amount > balance) {
                throw SendValidationError(SendValidationError.INSUFFICIENT_FUNDS)
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
}
