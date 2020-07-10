package piuk.blockchain.android.data.datamanagers

import com.blockchain.remoteconfig.CoinSelectionRemoteConfig
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payment.Payment
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.send.PendingTransaction
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.rxjava.IgnorableDefaultObserver
import timber.log.Timber
import java.math.BigInteger
import java.util.ArrayList

data class TransferableFundTransactionList(
    val pendingTransactions: List<PendingTransaction>,
    val totalToSend: BigInteger,
    val totalFee: BigInteger
)

class TransferFundsDataManager(
    private val payloadDataManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val dynamicFeeCache: DynamicFeeCache,
    private val coinSelectionRemoteConfig: CoinSelectionRemoteConfig
) {
    /**
     * Check if there are any spendable legacy funds that need to be sent to a HD wallet. Constructs
     * a list of [PendingTransaction] objects with outputs set to an account defined by it's
     * index in the list of HD accounts.
     *
     * @param addressToReceiveIndex The index of the account to which you want to send the funds
     * @return Returns a Map which bundles together the List of [PendingTransaction] objects,
     * as well as a Pair which contains the total to send and the total fees, in that order.
     */
    fun getTransferableFundTransactionList(
        addressToReceiveIndex: Int
    ): Observable<TransferableFundTransactionList> {
        return Observable.fromCallable {
            val suggestedFeePerKb =
                BigInteger.valueOf(dynamicFeeCache.btcFeeOptions!!.regularFee * 1000)
            val pendingTransactionList: MutableList<PendingTransaction> = ArrayList()
            val legacyAddresses = payloadDataManager.wallet!!.legacyAddressList

            var totalToSend = 0.toBigInteger()
            var totalFee = 0.toBigInteger()

            for (legacyAddress in legacyAddresses) {
                if (!legacyAddress.isWatchOnly &&
                    payloadDataManager.getAddressBalance(legacyAddress.address) > CryptoValue.Companion.ZeroBtc
                ) {
                    val unspentOutputs =
                        sendDataManager.getUnspentBtcOutputs(legacyAddress.address)
                            .blockingFirst()
                    val newCoinSelectionEnabled =
                        coinSelectionRemoteConfig.enabled.toObservable()
                            .blockingFirst()
                    val sweepableCoins =
                        sendDataManager.getMaximumAvailable(
                            CryptoCurrency.BTC,
                            unspentOutputs,
                            suggestedFeePerKb,
                            newCoinSelectionEnabled
                        )
                    val sweepAmount = sweepableCoins.left
                    // Don't sweep if there are still unconfirmed funds in address
                    if (unspentOutputs.notice == null && sweepAmount > Payment.DUST) {
                        val pendingSpend = PendingTransaction()
                        pendingSpend.unspentOutputBundle =
                            sendDataManager.getSpendableCoins(
                                unspentOutputs,
                                CryptoValue.fromMinor(CryptoCurrency.BTC, sweepAmount),
                                suggestedFeePerKb,
                                newCoinSelectionEnabled
                            )
                        var label = legacyAddress.label
                        if (label == null) {
                            label = ""
                        }
                        pendingSpend.sendingObject = ItemAccount(
                            label,
                            null,
                            "",
                            legacyAddress,
                            legacyAddress.address
                        )
                        pendingSpend.bigIntFee =
                            pendingSpend.unspentOutputBundle!!.absoluteFee
                        pendingSpend.bigIntAmount = sweepAmount
                        pendingSpend.addressToReceiveIndex = addressToReceiveIndex
                        totalToSend += pendingSpend.bigIntAmount
                        totalFee += pendingSpend.bigIntFee
                        pendingTransactionList.add(pendingSpend)
                    }
                }
            }
            TransferableFundTransactionList(
                pendingTransactionList,
                totalToSend,
                totalFee
            )
        }
        .doOnError(Timber::e)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Check if there are any spendable legacy funds that need to be sent to a HD wallet. Constructs
     * a list of [PendingTransaction] objects with outputs set to the default HD account.
     *
     * @return Returns a Triple object which bundles together the List of [PendingTransaction]
     * objects, as well as the total to send and the total fees, in that order.
     */
    val transferableFundTransactionListForDefaultAccount: Observable<TransferableFundTransactionList>
        get() = getTransferableFundTransactionList(payloadDataManager.defaultAccountIndex)

    /**
     * Takes a list of [PendingTransaction] objects and transfers them all. Emits a String
     * which is the Tx hash for each successful payment, and calls onCompleted when all
     * PendingTransactions have been finished successfully.
     *
     * @param pendingTransactions A list of [PendingTransaction] objects
     * @param secondPassword The double encryption password if necessary
     * @return An [<]
     */
    fun sendPayment(
        pendingTransactions: List<PendingTransaction>,
        secondPassword: String?
    ): Observable<String> {
        return getPaymentObservable(pendingTransactions, secondPassword)
            .doOnError(Timber::e)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun getPaymentObservable(
        pendingTransactions: List<PendingTransaction>,
        secondPassword: String?
    ): Observable<String> {
        return Observable.create { subscriber: ObservableEmitter<String> ->
            for (i in pendingTransactions.indices) {
                val pendingTransaction = pendingTransactions[i]
                val legacyAddress =
                    pendingTransaction.sendingObject!!.accountObject as LegacyAddress?
                val changeAddress = legacyAddress!!.address
                val receivingAddress =
                    payloadDataManager.getNextReceiveAddress(pendingTransaction.addressToReceiveIndex)
                        .blockingFirst()

                val ecKey = payloadDataManager.getAddressECKey(legacyAddress, secondPassword)
                val keys = mutableListOf(ecKey!!)

                sendDataManager.submitBtcPayment(
                    pendingTransaction.unspentOutputBundle!!,
                    keys,
                    receivingAddress,
                    changeAddress,
                    pendingTransaction.bigIntFee,
                    pendingTransaction.bigIntAmount
                )
                    .blockingSubscribe(
                        { s: String ->
                            if (!subscriber.isDisposed) {
                                subscriber.onNext(s)
                            }
                            // Increment index on receive chain
                            val account =
                                payloadDataManager.wallet
                                    ?.hdWallets?.get(0)
                                    ?.accounts?.get(pendingTransaction.addressToReceiveIndex)
                            payloadDataManager.incrementReceiveAddress(account!!)
                            // Update Balances temporarily rather than wait for sync
                            val spentAmount: Long =
                                pendingTransaction.bigIntAmount.toLong() +
                                    pendingTransaction.bigIntFee.toLong()

                            payloadDataManager.subtractAmountFromAddressBalance(
                                legacyAddress.address,
                                spentAmount
                            )
                            if (i == pendingTransactions.size - 1) { // Sync once transactions are completed
                                payloadDataManager.syncPayloadWithServer()
                                    .subscribe(IgnorableDefaultObserver<Any>())
                                if (!subscriber.isDisposed) {
                                    subscriber.onComplete()
                                }
                            }
                        }
                    ) { throwable: Throwable? ->
                        if (!subscriber.isDisposed) {
                            subscriber.onError(Throwable(throwable))
                        }
                    }
            }
        }
    }
}