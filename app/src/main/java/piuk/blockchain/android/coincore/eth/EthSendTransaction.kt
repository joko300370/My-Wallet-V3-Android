package piuk.blockchain.android.coincore.eth

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CryptoValue.Companion.max
import info.blockchain.balance.compareTo
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.SendValidationError
import piuk.blockchain.android.coincore.impl.OnChainSendTransactionBase
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import java.math.BigDecimal
import java.math.BigInteger

class EthSendTransaction(
    private val ethDataManager: EthDataManager,
    private val feeManager: FeeDataManager,
    sendingAccount: CryptoSingleAccount,
    address: CryptoAddress,
    requireSecondPassword: Boolean
) : OnChainSendTransactionBase(
        sendingAccount,
        address,
        requireSecondPassword
) {
    override val asset: CryptoCurrency = CryptoCurrency.ETHER

    override var amount: CryptoValue = CryptoValue.ZeroEth
        set(value) {
            field = value
        } // Do some checks here? Or not? TBD

    override var notes: String = ""

    override val feeOptions = setOf(FeeLevel.Regular)

    override val absoluteFee: Single<CryptoValue> =
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

    override val availableBalance: Single<CryptoValue> =
        Singles.zip(
            sendingAccount.balance,
            absoluteFee
        ) { balance: CryptoValue, fees: CryptoValue ->
            max(balance - fees, CryptoValue.ZeroEth)
        }

    // We can make some assumptions here over the previous impl;
    // 1. a CryptAddress object will be self-validating, so we need not check that it's valid
    override fun validate(): Completable =
        validateAmount()
            .then { validateSufficientFunds() }
            .then { validateNoPendingTx() }

    override fun executeTransaction(secondPassword: String): Single<String> =
        createTransaction()
            .flatMap {
                ethDataManager.signEthTransaction(it, secondPassword)
            }
            .flatMap { ethDataManager.pushTx(it) }
            .flatMap { ethDataManager.setLastTxHashNowSingle(it) }
            .doOnSuccess { ethDataManager.updateTransactionNotes(it, notes) }

    private fun createTransaction(): Single<RawTransaction> =
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
                weiValue = amount.amount
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

    private fun validateAmount(): Completable =
        Completable.fromCallable {
            if (amount <= CryptoValue.ZeroEth) {
                throw SendValidationError(SendValidationError.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(): Completable =
        Singles.zip(
            sendingAccount.balance,
            absoluteFee
        ) { balance: CryptoValue, fee: CryptoValue ->
            if (fee + amount > balance) {
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
