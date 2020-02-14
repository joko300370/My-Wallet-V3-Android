package piuk.blockchain.android.simplebuy

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.notifications.analytics.bankDetailsShow
import com.blockchain.notifications.analytics.bankFieldName
import com.blockchain.ui.urllinks.MODULAR_TERMS_AND_CONDITIONS
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_simple_buy_bank_details.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class SimpleBuyBankDetailsFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    SimpleBuyScreen,
    CopyFieldListener {
    override val model: SimpleBuyModel by inject()
    private val stringUtils: StringUtils by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_bank_details)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.process(SimpleBuyIntent.FetchBankAccount)
        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.BANK_DETAILS))
        activity.setupToolbar(R.string.transfer_details, false)
        confirm.setOnClickListener {
            analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_FINISHED)
            navigator().exitSimpleBuyFlow()
        }
    }

    override fun render(newState: SimpleBuyState) {
        if (newState.errorState != null) {
            showErrorState(newState.errorState)
        }

        if (newState.currency == "GBP") {
            val linksMap = mapOf<String, Uri>(
                "modular_terms_and_conditions" to Uri.parse(MODULAR_TERMS_AND_CONDITIONS)
            )
            bank_deposit_instruction.text =
                stringUtils.getStringWithMappedLinks(R.string.recipient_name_must_match_gbp,
                    linksMap,
                    requireActivity()
                )
            bank_deposit_instruction.movementMethod = LinkMovementMethod.getInstance()
        } else {
            bank_deposit_instruction.text = getString(R.string.recipient_name_must_match_eur)
        }

        analytics.logEvent(bankDetailsShow(newState.currency))

        val amount = newState.order.amount
        if (newState.bankAccount != null && amount != null) {
            bank_details_container.initWithBankDetailsAndAmount(
                newState.bankAccount.details,
                amount,
                this
            )
            secure_transfer.text = getString(R.string.simple_buy_securely_transfer,
                newState.order.amount?.currencyCode ?: "")
        }
    }

    private fun showErrorState(errorState: ErrorState) {
        showBottomSheet(ErrorSlidingBottomDialog.newInstance(activity))
    }

    override fun backPressedHandled(): Boolean {
        return true
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    override fun onFieldCopied(field: String) {
        Snackbar.make(
            bank_details_container,
            resources.getString(R.string.simple_buy_copied_to_clipboard, field),
            Snackbar.LENGTH_SHORT
        ).apply {
            view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.simple_buy_snackbar))
            show()
        }
        analytics.logEvent(bankFieldName(field))
    }

    override fun onSheetClosed() {
        super.onSheetClosed()
        model.process(SimpleBuyIntent.ClearError)
        navigator().exitSimpleBuyFlow()
    }
}