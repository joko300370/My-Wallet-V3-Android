package piuk.blockchain.android.ui.activity.detail

import com.blockchain.swap.nabu.datamanagers.OrderState
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.android.coincore.CustodialActivitySummaryItem
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.ui.base.mvi.MviIntent
import java.util.Date

sealed class ActivityDetailsIntents : MviIntent<ActivityDetailState>

class LoadActivityDetailsIntent(
    val cryptoCurrency: CryptoCurrency,
    val txHash: String,
    val isCustodial: Boolean
) : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState
    }
}

class LoadNonCustodialCreationDateIntent(
    val nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
) : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState
    }
}

object ActivityDetailsLoadFailedIntent : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState.copy(
            isError = true
        )
    }
}

class LoadNonCustodialHeaderDataIntent(
    private val nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
) : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState.copy(
            direction = nonCustodialActivitySummaryItem.direction,
            amount = nonCustodialActivitySummaryItem.value,
            isPending = !nonCustodialActivitySummaryItem.isConfirmed,
            isFeeTransaction = nonCustodialActivitySummaryItem.isFeeTransaction,
            confirmations = nonCustodialActivitySummaryItem.confirmations,
            totalConfirmations = nonCustodialActivitySummaryItem.cryptoCurrency.requiredConfirmations
        )
    }
}

class LoadCustodialHeaderDataIntent(
    private val custodialActivitySummaryItem: CustodialActivitySummaryItem
) : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState.copy(
            direction = TransactionSummary.Direction.BUY,
            amount = custodialActivitySummaryItem.value,
            isPending = custodialActivitySummaryItem.status == OrderState.AWAITING_FUNDS,
            isPendingExecution = custodialActivitySummaryItem.status == OrderState.PENDING_EXECUTION,
            isFeeTransaction = false,
            confirmations = 0,
            totalConfirmations = 0
        )
    }
}

class ListItemsLoadedIntent(
    private val list: List<ActivityDetailsType>
) : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        val currentList = oldState.listOfItems.toMutableSet()
        currentList.addAll(list.toSet())
        return oldState.copy(
            listOfItems = currentList,
            descriptionState = DescriptionState.NOT_SET
        )
    }
}

object ListItemsFailedToLoadIntent : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState.copy(
            isError = true
        )
    }
}

object CreationDateLoadFailedIntent : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState.copy(
            isError = true
        )
    }
}

object DescriptionUpdatedIntent : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState.copy(
            descriptionState = DescriptionState.UPDATE_SUCCESS
        )
    }
} object DescriptionUpdateFailedIntent : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState.copy(
            descriptionState = DescriptionState.UPDATE_ERROR
        )
    }
}

class CreationDateLoadedIntent(private val createdDate: Date) : ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        val list = oldState.listOfItems.toMutableSet()
        list.add(Created(createdDate))
        return oldState.copy(
            listOfItems = list
        )
    }
}

class UpdateDescriptionIntent(
    val txId: String,
    val cryptoCurrency: CryptoCurrency,
    val description: String
) :
    ActivityDetailsIntents() {
    override fun reduce(oldState: ActivityDetailState): ActivityDetailState {
        return oldState
    }
}
