package piuk.blockchain.android.ui.transfer.send.flow.adapter

import android.app.Activity
import android.net.Uri
import android.text.SpannableStringBuilder
import androidx.annotation.StringRes
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.util.StringUtils

class ConfirmTransactionDelegateAdapter(
    onAgreementWithLinksActionClicked: (Boolean) -> Unit,
    onAgreementTextActionClicked: (Boolean) -> Unit,
    onNoteItemUpdated: (String) -> Unit,
    stringUtils: StringUtils,
    activityContext: Activity
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(ConfirmInfoItemDelegate())
            addAdapterDelegate(ConfirmNoteItemDelegate(onNoteItemUpdated))
            addAdapterDelegate(ConfirmAgreementWithLinksItemDelegate(
                onAgreementWithLinksActionClicked,
                stringUtils,
                activityContext))
            addAdapterDelegate(ConfirmAgreementTextItemDelegate(onAgreementTextActionClicked))
        }
    }
}

sealed class ConfirmItemType

class ConfirmInfoItem(val title: String, val label: String) : ConfirmItemType()
class ConfirmNoteItem(val hint: String) : ConfirmItemType()
class ConfirmAgreementWithLinksItem(
    val uriMap: Map<String, Uri>,
    @StringRes val mappedString: Int
) : ConfirmItemType()
class ConfirmAgreementTextItem(val agreementText: SpannableStringBuilder) : ConfirmItemType()