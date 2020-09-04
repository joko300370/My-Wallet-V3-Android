package piuk.blockchain.android.ui.transfer.send.flow

import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.ExchangeRates
import kotlinx.android.synthetic.main.dialog_transaction_confirm.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.FlowInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmTransactionDelegateAdapter
import piuk.blockchain.android.util.StringUtils
import timber.log.Timber

class ConfirmTransactionSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {
    override val layoutResource: Int = R.layout.dialog_transaction_confirm

    private val stringUtils: StringUtils by inject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val prefs: CurrencyPrefs by scopedInject()
    private val mapper: TxConfirmReadOnlyMapper by scopedInject()
    private val customiser: SendFlowCustomiser by inject()

    private val listAdapter: ConfirmTransactionDelegateAdapter by lazy {
        ConfirmTransactionDelegateAdapter(
            model = model,
            stringUtils = stringUtils,
            activityContext = requireActivity(),
            mapper = mapper,
            selectedCurrency = prefs.selectedFiatCurrency,
            exchangeRates = exchangeRates
        )
    }

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! ConfirmTransactionSheet")
        require(newState.currentStep == SendStep.CONFIRM_DETAIL)

        // We _should_ always have a pending Tx when we get here
        require(newState.pendingTx != null)

        listAdapter.items = newState.pendingTx.options.toList()
        listAdapter.notifyDataSetChanged()
        dialogView.amount.text = newState.pendingTx.amount.toStringWithSymbol()

        dialogView.confirm_cta_button.text = customiser.confirmCtaText(newState)
        dialogView.confirm_sheet_title.text = customiser.confirmTitle(newState)
        dialogView.confirm_cta_button.isEnabled = newState.nextEnabled
    }

    override fun initControls(view: View) {
        view.confirm_cta_button.setOnClickListener { onCtaClick() }

        with(view.confirm_details_list) {
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )

            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = listAdapter
        }

        view.confirm_sheet_back.setOnClickListener {
            model.process(SendIntent.ReturnToPreviousStep)
        }

        model.process(SendIntent.ValidateTransaction)
    }

    private fun onCtaClick() {
        model.process(SendIntent.ExecuteTransaction)
    }
}
