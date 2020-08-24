package piuk.blockchain.android.ui.transfer.send.flow

import android.graphics.Typeface.BOLD
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.ui.urllinks.INTEREST_PRIVACY_POLICY
import com.blockchain.ui.urllinks.INTEREST_TERMS_OF_SERVICE
import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.Money
import kotlinx.android.synthetic.main.dialog_send_confirm.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.FlowInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmAgreementTextItem
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmAgreementWithLinksItem
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmInfoItem
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmItemType
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmNoteItem
import piuk.blockchain.android.ui.transfer.send.flow.adapter.ConfirmTransactionDelegateAdapter
import piuk.blockchain.android.util.StringUtils
import timber.log.Timber

class ConfirmTransactionSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {
    override val layoutResource: Int = R.layout.dialog_send_confirm

    private val stringUtils: StringUtils by inject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val prefs: CurrencyPrefs by scopedInject()

    private val customiser: SendFlowCustomiser by inject()

    private val listAdapter: ConfirmTransactionDelegateAdapter by lazy {
        ConfirmTransactionDelegateAdapter(
            model = model,
            stringUtils = stringUtils,
            activityContext = requireActivity()
        )
    }

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! ConfirmTransactionSheet")
        require(newState.currentStep == SendStep.CONFIRM_DETAIL)

        // We _should_ always have a pending Tx when we get here
        require(newState.pendingTx != null)

        val itemList = mutableListOf<ConfirmItemType>(
            ConfirmInfoItem(customiser.confirmListItemTitle(newState),
                newState.sendAmount.toStringWithSymbol()),
            ConfirmInfoItem(getString(R.string.common_from), newState.sendingAccount.label),
            ConfirmInfoItem(getString(R.string.common_to), newState.sendTarget.label)
        )

        getFeeItem(newState)?.let {
            itemList.add(it)
        }

        itemList.add(ConfirmInfoItem(getString(R.string.common_total), getTotalAmount(newState)))

        updateOptions(newState, itemList)

        listAdapter.items = itemList
        listAdapter.notifyDataSetChanged()

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

    private fun getTotalAmount(newState: SendState): String {
        val fee = newState.feeAmount
        val amount = newState.sendAmount

        return if (amount.symbol == fee.symbol) {
            (amount + fee).toStringWithSymbol()
        } else {
            "${amount.toStringWithSymbol()} (${fee.toStringWithSymbol()})"
        }
    }

    private fun updateOptions(
        state: SendState,
        itemList: MutableList<ConfirmItemType>
    ) {
        val note = state.pendingTx?.getOption<TxOptionValue.TxTextOption>(TxOption.DESCRIPTION)
        note?.let { opt ->
            itemList.add(ConfirmNoteItem(opt.text, state))
        }

        val linkAgreement =
            state.pendingTx?.getOption<TxOptionValue.TxBooleanOption>(
                TxOption.AGREEMENT_INTEREST_T_AND_C)
        linkAgreement?.let {
            setupTosAndPPLinks(itemList, state)
        }

        val textAgreement =
            state.pendingTx?.getOption<TxOptionValue.TxBooleanOption>(
                TxOption.AGREEMENT_INTEREST_TRANSFER)
        textAgreement?.let {
            setupHoldingValues(state.sendAmount, itemList, state)
        }
    }

    private fun getFeeItem(state: SendState): ConfirmInfoItem? {
        state.pendingTx?.let {
            if (it.feeLevel != FeeLevel.None) {
                val feeTitle = getString(
                    R.string.common_spaced_strings,
                    getString(R.string.send_confirmation_fee),
                    getString(R.string.send_confirmation_regular_estimation)
                )
                return ConfirmInfoItem(feeTitle, state.feeAmount.toStringWithSymbol())
            }
        }
        return null
    }

    private fun setupTosAndPPLinks(itemList: MutableList<ConfirmItemType>, state: SendState) {
        val linksMap = mapOf<String, Uri>(
            "interest_tos" to Uri.parse(INTEREST_TERMS_OF_SERVICE),
            "interest_pp" to Uri.parse(INTEREST_PRIVACY_POLICY)
        )

        itemList.add(
            ConfirmAgreementWithLinksItem(linksMap, R.string.send_confirmation_interest_tos_pp, state))
    }

    private fun setupHoldingValues(
        sendAmount: Money,
        itemList: MutableList<ConfirmItemType>,
        state: SendState
    ) {
        val introToHolding = getString(R.string.send_confirmation_interest_holding_period_1)
        val amountInBold =
            sendAmount.toFiat(exchangeRates, prefs.selectedFiatCurrency).toStringWithSymbol()
        val outroToHolding = getString(R.string.send_confirmation_interest_holding_period_2,
            sendAmount.toStringWithSymbol())
        val sb = SpannableStringBuilder()
        sb.append(introToHolding)
        sb.append(amountInBold)
        sb.setSpan(StyleSpan(BOLD), introToHolding.length,
            introToHolding.length + amountInBold.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append(outroToHolding)

        itemList.add(ConfirmAgreementTextItem(sb, state))
    }

    private fun onCtaClick() {
        model.process(SendIntent.ExecuteTransaction)
    }
}
