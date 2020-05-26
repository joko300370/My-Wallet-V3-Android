package piuk.blockchain.android.ui.activity.detail

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import java.util.Date

sealed class ActivityDetailsType
data class Created(val date: Date) : ActivityDetailsType()
data class Amount(val cryptoValue: CryptoValue) : ActivityDetailsType()
data class Fee(val feeValue: CryptoValue?) : ActivityDetailsType()
data class Value(val fiatAtExecution: FiatValue?) : ActivityDetailsType()
data class From(val fromAddress: String?) : ActivityDetailsType()
data class FeeForTransaction(
    val direction: TransactionSummary.Direction,
    val cryptoValue: CryptoValue
) : ActivityDetailsType()
data class To(val toAddress: String?) : ActivityDetailsType()
data class Description(val description: String? = null) : ActivityDetailsType()
data class Action(val action: String = "") : ActivityDetailsType()
data class CancelAction(val cancelAction: String = "") : ActivityDetailsType()
data class BuyFee(val feeValue: FiatValue) : ActivityDetailsType()
data class BuyPurchaseAmount(val fundedFiat: FiatValue) : ActivityDetailsType()
data class BuyTransactionId(val txId: String) : ActivityDetailsType()
data class BuyCryptoWallet(val crypto: CryptoCurrency) : ActivityDetailsType()
data class BuyPaymentMethod(val paymentDetails: PaymentDetails) : ActivityDetailsType()

data class PaymentDetails(val paymentMethodId: String, val label: String?, val endDigits: String?)

enum class DescriptionState {
    NOT_SET,
    UPDATE_SUCCESS,
    UPDATE_ERROR
}

data class ActivityDetailState(
    val direction: TransactionSummary.Direction? = null,
    val amount: CryptoValue? = null,
    val isPending: Boolean = false,
    val isPendingExecution: Boolean = false,
    val isFeeTransaction: Boolean = false,
    val confirmations: Int = 0,
    val totalConfirmations: Int = 0,
    val listOfItems: Set<ActivityDetailsType> = emptySet(),
    val isError: Boolean = false,
    val descriptionState: DescriptionState = DescriptionState.NOT_SET
) : MviState

class ActivityDetailsModel(
    initialState: ActivityDetailState,
    mainScheduler: Scheduler,
    private val interactor: ActivityDetailsInteractor
) : MviModel<ActivityDetailState, ActivityDetailsIntents>(initialState, mainScheduler) {

    override fun performAction(
        previousState: ActivityDetailState,
        intent: ActivityDetailsIntents
    ): Disposable? {
        return when (intent) {
            is LoadActivityDetailsIntent -> {
                if (intent.isCustodial) {
                    loadCustodialActivityDetails(intent)
                } else {
                    loadNonCustodialActivityDetails(intent)
                }
                null
            }
            is UpdateDescriptionIntent ->
                interactor.updateItemDescription(intent.txId, intent.cryptoCurrency,
                    intent.description)
                    .subscribeBy(
                        onComplete = {
                            process(DescriptionUpdatedIntent)
                        },
                        onError = {
                            process(DescriptionUpdateFailedIntent)
                        })
            is LoadNonCustodialCreationDateIntent -> {
                val activityDate =
                    interactor.loadCreationDate(intent.nonCustodialActivitySummaryItem)
                activityDate?.let {
                    process(CreationDateLoadedIntent(activityDate))

                    val nonCustodialActivitySummaryItem = intent.nonCustodialActivitySummaryItem
                    loadListDetailsForDirection(nonCustodialActivitySummaryItem)
                } ?: process(CreationDateLoadFailedIntent)
                null
            }
            is DescriptionUpdatedIntent,
            is DescriptionUpdateFailedIntent,
            is ListItemsFailedToLoadIntent,
            is ListItemsLoadedIntent,
            is CreationDateLoadedIntent,
            is CreationDateLoadFailedIntent,
            is ActivityDetailsLoadFailedIntent,
            is LoadCustodialHeaderDataIntent,
            is LoadNonCustodialHeaderDataIntent -> null
        }
    }

    private fun loadListDetailsForDirection(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
    ) {
        val direction = nonCustodialActivitySummaryItem.direction
        when {
            nonCustodialActivitySummaryItem.isFeeTransaction ->
                loadFeeTransactionItems(nonCustodialActivitySummaryItem)
            direction == TransactionSummary.Direction.TRANSFERRED ->
                loadTransferItems(nonCustodialActivitySummaryItem)
            direction == TransactionSummary.Direction.RECEIVED ->
                loadReceivedItems(nonCustodialActivitySummaryItem)
            direction == TransactionSummary.Direction.SENT -> {
                loadSentItems(nonCustodialActivitySummaryItem)
            }
            direction == TransactionSummary.Direction.BUY ||
            direction == TransactionSummary.Direction.SELL -> {
                // do nothing BUY & SELL are a custodial transaction
            }
            direction == TransactionSummary.Direction.SWAP -> TODO()
        }
    }

    private fun loadNonCustodialActivityDetails(intent: LoadActivityDetailsIntent) =
        interactor.getNonCustodialActivityDetails(
            cryptoCurrency = intent.cryptoCurrency,
            txHash = intent.txHash)?.let {
            process(LoadNonCustodialCreationDateIntent(it))
            process(LoadNonCustodialHeaderDataIntent(it))
        } ?: process(ActivityDetailsLoadFailedIntent)

    private fun loadCustodialActivityDetails(intent: LoadActivityDetailsIntent) =
        interactor.getCustodialActivityDetails(cryptoCurrency = intent.cryptoCurrency,
            txHash = intent.txHash)?.let {
            process(LoadCustodialHeaderDataIntent(it))
            interactor.loadCustodialItems(it).subscribeBy(
                onSuccess = { activityList ->
                    process(ListItemsLoadedIntent(activityList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                })
        } ?: process(ActivityDetailsLoadFailedIntent)

    private fun loadFeeTransactionItems(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
    ) =
        interactor.loadFeeItems(nonCustodialActivitySummaryItem)
            .subscribeBy(
                onSuccess = { activityItemList ->
                    process(ListItemsLoadedIntent(activityItemList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                }
            )

    private fun loadReceivedItems(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
    ) =
        interactor.loadReceivedItems(nonCustodialActivitySummaryItem)
            .subscribeBy(
                onSuccess = { activityItemList ->
                    process(ListItemsLoadedIntent(activityItemList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                }
            )

    private fun loadTransferItems(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
    ) =
        interactor.loadTransferItems(nonCustodialActivitySummaryItem)
            .subscribeBy(
                onSuccess = { activityItemList ->
                    process(ListItemsLoadedIntent(activityItemList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                }
            )

    private fun loadSentItems(nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem) =
        if (nonCustodialActivitySummaryItem.isConfirmed) {
            interactor.loadConfirmedSentItems(
                nonCustodialActivitySummaryItem
            ).subscribeBy(
                onSuccess = { activityItemsList ->
                    process(ListItemsLoadedIntent(activityItemsList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                }
            )
        } else {
            interactor.loadUnconfirmedSentItems(
                nonCustodialActivitySummaryItem
            ).subscribeBy(
                onSuccess = { activityItemsList ->
                    process(ListItemsLoadedIntent(activityItemsList))
                },
                onError = {
                    process(ListItemsFailedToLoadIntent)
                })
        }
}