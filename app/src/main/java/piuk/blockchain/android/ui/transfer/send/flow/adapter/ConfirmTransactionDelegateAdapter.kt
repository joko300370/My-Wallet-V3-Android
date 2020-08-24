package piuk.blockchain.android.ui.transfer.send.flow.adapter

import android.app.Activity
import android.net.Uri
import android.text.SpannableStringBuilder
import androidx.annotation.StringRes
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.transfer.send.SendModel
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.util.StringUtils

class ConfirmTransactionDelegateAdapter(
    stringUtils: StringUtils,
    activityContext: Activity,
    model: SendModel
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(ConfirmInfoItemDelegate())
            addAdapterDelegate(ConfirmNoteItemDelegate(model))
            addAdapterDelegate(ConfirmAgreementWithTAndCsItemDelegate(
                model,
                stringUtils,
                activityContext))
            addAdapterDelegate(ConfirmAgreementToTransferItemDelegate(model))
        }
    }
}

sealed class ConfirmItemType

class ConfirmInfoItem(val title: String, val label: String) : ConfirmItemType()
class ConfirmNoteItem(
    val hint: String,
    val state: SendState
) : ConfirmItemType()

class ConfirmAgreementWithLinksItem(
    val uriMap: Map<String, Uri>,
    @StringRes val mappedString: Int,
    val state: SendState
) : ConfirmItemType()

class ConfirmAgreementTextItem(
    val agreementText: SpannableStringBuilder,
    val state: SendState
) : ConfirmItemType()