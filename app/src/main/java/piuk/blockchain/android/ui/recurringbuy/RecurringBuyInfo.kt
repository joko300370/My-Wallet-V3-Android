package piuk.blockchain.android.ui.recurringbuy

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RecurringBuyInfo(
    val title: String,
    val subtitle1: String,
    val subtitle2: String? = null,
    val hasImage: Boolean
) : Parcelable