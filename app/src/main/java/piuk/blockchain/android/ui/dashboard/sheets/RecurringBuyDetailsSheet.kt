package piuk.blockchain.android.ui.dashboard.sheets

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyState
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetRecurringBuyInfoBinding
import piuk.blockchain.android.simplebuy.CheckoutAdapterDelegate
import piuk.blockchain.android.simplebuy.SimpleBuyCheckoutItem
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringBuy
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringDate
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsError
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsIntent
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsModel
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsState
import piuk.blockchain.android.ui.dashboard.assetdetails.ClearSelectedRecurringBuy
import piuk.blockchain.android.ui.dashboard.assetdetails.DeleteRecurringBuy
import piuk.blockchain.android.ui.dashboard.assetdetails.ReturnToPreviousStep
import com.blockchain.utils.toFormattedDate

class RecurringBuyDetailsSheet : MviBottomSheet<AssetDetailsModel,
    AssetDetailsIntent, AssetDetailsState, DialogSheetRecurringBuyInfoBinding>() {

    private val listAdapter: CheckoutAdapterDelegate by lazy {
        CheckoutAdapterDelegate()
    }

    override val model: AssetDetailsModel by scopedInject()

    override fun initControls(binding: DialogSheetRecurringBuyInfoBinding) {
        with(binding) {
            with(rbSheetItems) {
                adapter = listAdapter
                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(BlockchainListDividerDecor(requireContext()))
            }

            rbSheetBack.setOnClickListener {
                returnToPreviousSheet()
            }

            rbSheetCancel.setOnClickListener {
                // TODO stopgap check while design make their mind up
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.settings_bank_remove_check_title)
                    .setMessage(R.string.recurring_buy_cancel_dialog_desc)
                    .setPositiveButton(R.string.common_ok) { di, _ ->
                        di.dismiss()
                        model.process(DeleteRecurringBuy)
                    }
                    .setNegativeButton(R.string.common_cancel) { di, _ ->
                        di.dismiss()
                    }.show()
            }
        }
    }

    override fun render(newState: AssetDetailsState) {
        newState.selectedRecurringBuy?.let {
            when {
                it.state == RecurringBuyState.NOT_ACTIVE -> {
                    ToastCustom.makeText(
                        requireContext(), getString(R.string.recurring_buy_cancelled_toast), Toast.LENGTH_LONG,
                        ToastCustom.TYPE_OK
                    )
                    returnToPreviousSheet()
                }
                newState.errorState == AssetDetailsError.RECURRING_BUY_DELETE -> {
                    ToastCustom.makeText(
                        requireContext(), getString(R.string.recurring_buy_cancelled_error_toast), Toast.LENGTH_LONG,
                        ToastCustom.TYPE_ERROR
                    )
                }
                else ->
                    with(binding) {
                        rbSheetTitle.text = getString(R.string.recurring_buy_sheet_title, it.asset.displayTicker)
                        rbSheetHeader.setDetails(
                            it.amount.toStringWithSymbol(),
                            ""
                        )
                        it.renderListItems()
                    }
            }
        }
    }

    private fun returnToPreviousSheet() {
        model.process(ClearSelectedRecurringBuy)
        model.process(ReturnToPreviousStep)
    }

    private fun RecurringBuy.renderListItems() {
        listAdapter.items = listOf(
            SimpleBuyCheckoutItem.ComplexCheckoutItem(
                getString(R.string.payment_method),
                // TODO when the BE gets updated with payment method info we can update this
                paymentMethodType.toString(),
                paymentMethodType.toString()
            ),
            SimpleBuyCheckoutItem.ComplexCheckoutItem(
                getString(R.string.recurring_buy_frequency_label),
                recurringBuyFrequency.toHumanReadableRecurringBuy(requireContext()),
                recurringBuyFrequency.toHumanReadableRecurringDate(requireContext())
            ),
            SimpleBuyCheckoutItem.SimpleCheckoutItem(
                getString(R.string.recurring_buy_info_purchase_label),
                nextPaymentDate.toFormattedDate()
            ),
            SimpleBuyCheckoutItem.SimpleCheckoutItem(
                getString(R.string.common_total),
                amount.toStringWithSymbol(),
                true
            )
        )
        listAdapter.notifyDataSetChanged()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetRecurringBuyInfoBinding =
        DialogSheetRecurringBuyInfoBinding.inflate(layoutInflater, container, false)

    companion object {
        fun newInstance(): RecurringBuyDetailsSheet = RecurringBuyDetailsSheet()
    }
}