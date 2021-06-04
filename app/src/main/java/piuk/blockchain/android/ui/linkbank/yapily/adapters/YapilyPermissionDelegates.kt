package piuk.blockchain.android.ui.linkbank.yapily.adapters

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter

class YapilyAgreementDelegateAdapter : DelegationAdapter<YapilyPermissionItem>(AdapterDelegatesManager(), emptyList()) {

    lateinit var onExpandableItemClicked: (Int) -> Unit

    fun initialise() {
        with(delegatesManager) {
            addAdapterDelegate(YapilyExpandableItemDelegate(onExpandableItemClicked))
            addAdapterDelegate(YapilyStaticItemDelegate())
            addAdapterDelegate(YapilyHeaderItemDelegate())
        }
    }
}

class YapilyApprovalDelegateAdapter : DelegationAdapter<YapilyPermissionItem>(AdapterDelegatesManager(), emptyList()) {

    lateinit var onExpandableItemClicked: (Int) -> Unit
    lateinit var onExpandableListItemClicked: (Int) -> Unit

    fun initialise() {
        with(delegatesManager) {
            addAdapterDelegate(YapilyExpandableListItemDelegate(onExpandableListItemClicked))
            addAdapterDelegate(YapilyStaticItemDelegate())
            addAdapterDelegate(YapilyHeaderItemDelegate())
            addAdapterDelegate(YapilyInfoItemDelegate())
            addAdapterDelegate(YapilyExpandableItemDelegate(onExpandableItemClicked))
        }
    }
}

sealed class YapilyPermissionItem {
    data class YapilyExpandableListItem(
        @StringRes val title: Int,
        var isExpanded: Boolean = false,
        var playAnimation: Boolean = false,
        val items: List<YapilyInfoItem>
    ) : YapilyPermissionItem()

    data class YapilyExpandableItem(
        @StringRes val title: Int,
        val blurb: String,
        var isExpanded: Boolean = false,
        var playAnimation: Boolean = false
    ) : YapilyPermissionItem()

    data class YapilyStaticItem(
        @StringRes val blurb: Int,
        val bankName: String
    ) : YapilyPermissionItem()

    data class YapilyHeaderItem(
        @DrawableRes val icon: Int,
        val title: String,
        val shouldShowSubheader: Boolean = false
    ) : YapilyPermissionItem()

    data class YapilyInfoItem(
        val title: String,
        val info: String
    ) : YapilyPermissionItem()
}