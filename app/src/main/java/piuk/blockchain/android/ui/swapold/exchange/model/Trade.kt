package piuk.blockchain.android.ui.swapold.exchange.model

import android.os.Parcelable
import com.blockchain.swap.common.trade.MorphTrade
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Trade(
    val id: String,
    val state: MorphTrade.Status,
    val currency: String,
    val price: String,
    val fee: String,
    val pair: String,
    val quantity: String,
    val createdAt: String,
    val depositQuantity: String
) : Parcelable {

    fun approximateValue(): Boolean =
        state == MorphTrade.Status.IN_PROGRESS

    fun refunding(): Boolean =
        state == MorphTrade.Status.REFUND_IN_PROGRESS

    fun refunded(): Boolean =
        state == MorphTrade.Status.REFUNDED

    fun expired(): Boolean =
        state == MorphTrade.Status.EXPIRED

    fun failed(): Boolean =
        state == MorphTrade.Status.FAILED
}