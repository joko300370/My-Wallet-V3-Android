package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.app.Activity
import info.blockchain.balance.ExchangeRates
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapper
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperNewCheckout
import piuk.blockchain.android.util.StringUtils

class ConfirmTransactionDelegateAdapter(
    stringUtils: StringUtils,
    activityContext: Activity,
    model: TransactionModel,
    analytics: TxFlowAnalytics,
    mapper: TxConfirmReadOnlyMapper,
    mapperNewCheckout: TxConfirmReadOnlyMapperNewCheckout,
    exchangeRates: ExchangeRates,
    selectedCurrency: String,
    assetResources: AssetResources
) : DelegationAdapter<TxConfirmationValue>(AdapterDelegatesManager(), emptyList()) {
    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            // New checkout screens:
            addAdapterDelegate(SimpleConfirmationCheckoutDelegate(mapperNewCheckout))
            addAdapterDelegate(ComplexConfirmationCheckoutDelegate(mapperNewCheckout))
            addAdapterDelegate(ExpandableSimpleConfirmationCheckout(mapperNewCheckout))
            addAdapterDelegate(ExpandableComplexConfirmationCheckout(mapperNewCheckout))
            addAdapterDelegate(ConfirmInfoItemDelegate(mapper))
            addAdapterDelegate(ConfirmNoteItemDelegate(model))
            addAdapterDelegate(ConfirmXlmMemoItemDelegate(model, stringUtils, activityContext))
            addAdapterDelegate(ConfirmAgreementWithTAndCsItemDelegate(model, stringUtils, activityContext))
            addAdapterDelegate(
                ConfirmAgreementToTransferItemDelegate(
                    model,
                    exchangeRates,
                    selectedCurrency,
                    assetResources
                )
            )
            addAdapterDelegate(LargeTransactionWarningItemDelegate(model))
            addAdapterDelegate(InvoiceCountdownTimerDelegate())
            addAdapterDelegate(ConfirmInfoItemValidationStatusDelegate())
            addAdapterDelegate(ConfirmInfoItemFeeOptionDelegate(model, analytics, stringUtils, assetResources))
            addAdapterDelegate(ConfirmNetworkFeeItemDelegate(stringUtils, assetResources))
        }
    }
}