package piuk.blockchain.android.coincore.erc20

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.impl.OnChainSendProcessorBase
import piuk.blockchain.androidcore.data.erc20.Erc20Account

class Erc20SendTransaction(
    override val asset: CryptoCurrency,
    private val erc20Account: Erc20Account,
//    private val feeManager: FeeDataManager,
    sendingAccount: Erc20NonCustodialAccount,
    address: CryptoAddress,
    requireSecondPassword: Boolean
) : OnChainSendProcessorBase(
        sendingAccount,
        address,
        requireSecondPassword
) {

    override val feeOptions = setOf(FeeLevel.Regular)

    override val isNoteSupported: Boolean = true

    override fun absoluteFee(pendingTx: PendingSendTx): Single<Money> =
        Single.just(CryptoValue.ZeroEth)
//        feeOptions().map {
//            CryptoValue.fromMinor(
//                CryptoCurrency.ETHER,
//                Convert.toWei(
//                    BigDecimal.valueOf(it.gasLimit * it.regularFee),
//                    Convert.Unit.GWEI
//                )
//            )
//        }

//    private fun feeOptions(): Single<FeeOptions> =
//        feeManager.ethFeeOptions.singleOrError()

    override fun availableBalance(pendingTx: PendingSendTx): Single<Money> =
        Single.just(CryptoValue.fromMajor(asset, 10.toBigDecimal()))
//        Singles.zip(
//            sendingAccount.balance,
//            absoluteFee(pendingTx)
//        ) { balance: Money, fees: Money ->
//            max(balance - fees, CryptoValue.ZeroEth)
//        }

    // We can make some assumptions here over the previous impl;
    // 1. a CryptAddress object will be self-validating, so we need not check that it's valid
    override fun validate(pendingTx: PendingSendTx): Completable =
        Completable.complete()
//        validateAmount(pendingTx)
//            .then { validateSufficientFunds(pendingTx) }
//            .then { validateNoPendingTx() }
//            .doOnError { Timber.e("Validation failed: $it") }

    override fun executeTransaction(pendingTx: PendingSendTx, secondPassword: String): Single<String> =
        Single.just("STUB")
//        createTransaction(pendingTx)
//            .flatMap {
//                ethDataManager.signEthTransaction(it, secondPassword)
//            }
//            .flatMap { ethDataManager.pushTx(it) }
//            .flatMap { ethDataManager.setLastTxHashNowSingle(it) }
//            .doOnSuccess { ethDataManager.updateTransactionNotes(it, pendingTx.notes) }

//    private fun createTransaction(pendingTx: PendingSendTx): Single<RawTransaction> =
//        Singles.zip(
//            ethDataManager.getNonce(),
//            ethDataManager.isContractAddress(address.address),
//            feeOptions()
//        ).map { (nonce, isContract, fees) ->
//            ethDataManager.createEthTransaction(
//                nonce = nonce,
//                to = address.address,
//                gasPriceWei = fees.gasPrice,
//                gasLimitGwei = fees.getGasLimit(isContract),
//                weiValue = pendingTx.amount.toBigInteger()
//            )
//        }

//    private val FeeOptions.gasPrice: BigInteger
//        get() = Convert.toWei(
//            BigDecimal.valueOf(regularFee),
//            Convert.Unit.GWEI
//        ).toBigInteger()

//    private fun FeeOptions.getGasLimit(isContract: Boolean): BigInteger =
//        BigInteger.valueOf(
//            if (isContract) gasLimitContract else gasLimit
//        )

//    private fun validateAmount(pendingTx: PendingSendTx): Completable =
//        Completable.fromCallable {
//            if (pendingTx.amount <= CryptoValue.ZeroEth) {
//                throw SendValidationError(SendValidationError.INVALID_AMOUNT)
//            }
//        }
}
