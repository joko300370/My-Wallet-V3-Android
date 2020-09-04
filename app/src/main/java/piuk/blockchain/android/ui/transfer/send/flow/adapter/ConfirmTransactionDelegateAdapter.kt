package piuk.blockchain.android.ui.transfer.send.flow.adapter

import android.app.Activity
import info.blockchain.balance.ExchangeRates
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.transfer.send.SendModel
import piuk.blockchain.android.ui.transfer.send.flow.TxConfirmReadOnlyMapper
import piuk.blockchain.android.util.StringUtils

class ConfirmTransactionDelegateAdapter(
    stringUtils: StringUtils,
    activityContext: Activity,
    model: SendModel,
    mapper: TxConfirmReadOnlyMapper,
    exchangeRates: ExchangeRates,
    selectedCurrency: String
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {
    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(ConfirmInfoItemDelegate(mapper))
            addAdapterDelegate(ConfirmNoteItemDelegate(model))
            addAdapterDelegate(ConfirmAgreementWithTAndCsItemDelegate(
                model,
                stringUtils,
                activityContext))
            addAdapterDelegate(ConfirmAgreementToTransferItemDelegate(model, exchangeRates, selectedCurrency))
        }
    }
}