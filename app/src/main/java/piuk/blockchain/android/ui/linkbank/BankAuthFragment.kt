package piuk.blockchain.android.ui.linkbank

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.models.data.BankPartner
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.nabu.models.data.LinkedBank
import com.blockchain.nabu.models.data.YapilyAttributes
import com.blockchain.ui.urllinks.URL_YODLEE_SUPPORT_LEARN_MORE
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentLinkABankBinding
import piuk.blockchain.android.simplebuy.ErrorState
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable

class BankAuthFragment : MviFragment<BankAuthModel, BankAuthIntent, BankAuthState>() {

    override val model: BankAuthModel by scopedInject()
    private val stringUtils: StringUtils by inject()

    private var _binding: FragmentLinkABankBinding? = null
    private val binding: FragmentLinkABankBinding
        get() = _binding!!

    private val isFromDeepLink: Boolean by lazy {
        arguments?.getBoolean(FROM_DEEP_LINK, false) ?: false
    }

    private val isForApproval: Boolean by lazy {
        arguments?.getBoolean(FOR_APPROVAL, false) ?: false
    }

    private val accountProviderId: String by lazy {
        arguments?.getString(ACCOUNT_PROVIDER_ID) ?: ""
    }

    private val accountId: String by lazy {
        arguments?.getString(ACCOUNT_ID) ?: ""
    }

    private val approvalData: BankPaymentApproval? by unsafeLazy {
        arguments?.getSerializable(APPROVAL_DATA) as? BankPaymentApproval
    }

    private val linkingBankId: String by lazy {
        arguments?.getString(LINKING_BANK_ID) ?: ""
    }

    private val errorState: ErrorState? by unsafeLazy {
        arguments?.getSerializable(ERROR_STATE) as? ErrorState
    }

    private val authSource: BankAuthSource by lazy {
        arguments?.getSerializable(LINK_BANK_SOURCE) as BankAuthSource
    }

    private val linkBankTransfer: LinkBankTransfer? by unsafeLazy {
        arguments?.getSerializable(LINK_BANK_TRANSFER) as? LinkBankTransfer
    }

    private var hasChosenExternalApp: Boolean = false
    private var hasExternalLinkingLaunched: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLinkABankBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.setupToolbar(
            if (isForApproval) {
                R.string.approve_payment
            } else {
                R.string.link_a_bank
            }, false
        )

        if (savedInstanceState == null) {
            when {
                isForApproval -> {
                    approvalData?.let {
                        model.process(BankAuthIntent.UpdateForApproval(it.authorisationUrl))
                    }
                }
                isFromDeepLink -> {
                    model.process(BankAuthIntent.GetLinkedBankState(linkingBankId, isFromDeepLink))
                }
                else -> {
                    startBankAuthentication()
                }
            }
        }

        with(binding) {
            linkBankRetry.setOnClickListener {
                logRetryLaunchAnalytics()
                linkBankRetry.gone()
                linkBankCancel.gone()
                startBankAuthentication()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasExternalLinkingLaunched && !hasChosenExternalApp) {
            showAppOpeningRetry()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun render(newState: BankAuthState) {
        when (newState.bankLinkingProcessState) {
            BankLinkingProcessState.CANCELED -> {
                navigator().bankAuthCancelled()
                return
            }
            BankLinkingProcessState.LINKING -> showLinkingInProgress(newState.linkBankTransfer)
            BankLinkingProcessState.ACTIVATING -> showActivationInProgress()
            BankLinkingProcessState.IN_EXTERNAL_FLOW -> {
                newState.linkBankTransfer?.attributes?.let {
                    showExternalFlow(it as YapilyAttributes)
                }
            }
            BankLinkingProcessState.APPROVAL -> {
                approvalData?.let {
                    showApprovalInProgress(it.linkedBank)
                }
            }
            BankLinkingProcessState.APPROVAL_WAIT -> showBankApproval()
            BankLinkingProcessState.LINKING_SUCCESS -> processLinkingSuccess(newState)
            BankLinkingProcessState.NONE -> {
                // do nothing
            }
        }.exhaustive

        if (!newState.linkBankUrl.isNullOrEmpty()) {
            handleExternalLinking(newState)
        }

        val error = newState.errorState ?: errorState
        error?.let { e ->
            showErrorState(e, newState.linkBankTransfer?.partner)
        }
    }

    private fun processLinkingSuccess(state: BankAuthState) {
        if (!isForApproval) {
            state.linkedBank?.let {
                showLinkingSuccess(
                    label = it.name,
                    id = it.id,
                    partner = it.partner,
                    currency = it.currency
                )
            }
        }
    }

    private fun startBankAuthentication() {
        if (isForApproval) {
            approvalData?.let {
                model.process(BankAuthIntent.UpdateForApproval(it.authorisationUrl))
            }
        } else {
            linkBankTransfer?.let {
                model.process(
                    BankAuthIntent.UpdateAccountProvider(accountProviderId, accountId, linkingBankId, it, authSource)
                )
            }
        }
    }

    private fun showAppOpeningRetry() {
        with(binding) {
            linkBankRetry.visible()
            linkBankProgress.gone()
            linkBankStateIndicator.visible()
            linkBankTitle.text = getString(R.string.yapily_bank_link_choice_error_title)
            linkBankSubtitle.text = getString(R.string.yapily_bank_link_choice_error_subtitle)
            linkBankStateIndicator.setImageResource(R.drawable.ic_alert_white_bkgd)
        }
        model.process(BankAuthIntent.ResetBankLinking)
        hasExternalLinkingLaunched = false
    }

    private fun handleExternalLinking(newState: BankAuthState) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newState.linkBankUrl))

        val receiver = ChooserReceiver()
        receiver.registerListener(object : ChooserCallback {
            override fun onItemChosen() {
                hasChosenExternalApp = true
                if (isForApproval) {
                    model.process(BankAuthIntent.StartBankApproval)
                } else {
                    model.process(BankAuthIntent.StartBankLinking)
                }
            }
        })
        val receiverIntent = Intent(context, receiver.javaClass)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        try {
            startActivity(
                Intent.createChooser(
                    intent, getString(R.string.yapily_bank_link_app_choice_title), pendingIntent.intentSender
                )
            )
            hasExternalLinkingLaunched = true
            model.process(BankAuthIntent.ClearBankLinkingUrl)
        } catch (e: ActivityNotFoundException) {
            model.process(BankAuthIntent.ErrorIntent())
            ToastCustom.makeText(
                requireContext(), getString(R.string.yapily_bank_link_no_apps), Toast.LENGTH_LONG,
                ToastCustom.TYPE_ERROR
            )
        }
    }

    private fun showBankApproval() {
        showLoading()
        setTitleAndSubtitle(
            getString(
                R.string.yapily_linking_in_progress_title,
                approvalData?.linkedBank?.name ?: getString(R.string.yapily_linking_default_bank)
            ),
            getString(R.string.yapily_approval_in_progress_subtitle)
        )

        with(binding) {
            linkBankRetry.visible()
            linkBankCancel.apply {
                visible()
                background = requireContext().getResolvedDrawable(R.drawable.bkgd_button_no_bkgd_red_selection_selector)
                setTextColor(resources.getColorStateList(R.color.button_red_text_states, null))
                text = getString(R.string.cancel_order)
                setOnClickListener {
                    model.process(BankAuthIntent.CancelOrder)
                }
                analytics.logEvent(bankAuthEvent(BankAuthAnalytics.PIS_EXTERNAL_FLOW_CANCEL, authSource))
            }
        }
    }

    private fun showActivationInProgress() {
        showLoading()
        setTitleAndSubtitle(
            getString(R.string.yapily_activating_title),
            getString(R.string.yapily_activating_subtitle)
        )
    }

    private fun showApprovalInProgress(linkedBank: LinkedBank) {
        showLoading()
        setTitleAndSubtitle(
            getString(R.string.yapily_approving_title, linkedBank.name),
            getString(R.string.yapily_approving_subtitle)
        )
        model.process(BankAuthIntent.ClearApprovalState)
    }

    private fun showExternalFlow(attrs: YapilyAttributes) {
        showLoading()
        hasChosenExternalApp = false
        setTitleAndSubtitle(
            getString(
                R.string.yapily_linking_in_progress_title,
                attrs.institutionList.find { institution -> institution.id == accountId }?.name
                    ?: getString(R.string.yapily_linking_default_bank)
            ),
            getString(R.string.yapily_linking_in_progress_subtitle)
        )
        model.process(BankAuthIntent.GetLinkedBankState(linkingBankId))
    }

    private fun setTitleAndSubtitle(title: String, subtitle: String) {
        with(binding) {
            linkBankTitle.text = title
            linkBankSubtitle.text = subtitle
        }
    }

    private fun showLoading() {
        with(binding) {
            linkBankProgress.visible()
            linkBankStateIndicator.gone()
        }
    }

    private fun showErrorState(state: ErrorState, partner: BankPartner?) {
        when (state) {
            ErrorState.BankLinkingTimeout -> {
                logAnalytics(BankAuthAnalytics.GENERIC_ERROR, partner)
                setTitleAndSubtitle(
                    getString(R.string.bank_linking_generic_error_title),
                    getString(R.string.bank_linking_timeout_error_subtitle)
                )

                binding.linkBankBtn.text = getString(R.string.common_try_again)
            }
            ErrorState.LinkedBankAlreadyLinked -> {
                logAnalytics(BankAuthAnalytics.ALREADY_LINKED, partner)
                setTitleAndSubtitle(
                    getString(R.string.bank_linking_generic_error_title),
                    getString(R.string.bank_linking_already_linked_error_subtitle)
                )

                binding.linkBankBtn.text = getString(R.string.bank_linking_try_different_account)
            }
            ErrorState.LinkedBankAccountUnsupported -> {
                logAnalytics(BankAuthAnalytics.INCORRECT_ACCOUNT, partner)
                setTitleAndSubtitle(
                    getString(R.string.bank_linking_checking_error_title),
                    getString(R.string.bank_linking_checking_error_subtitle)
                )

                binding.linkBankBtn.text = getString(R.string.bank_linking_try_different_bank)
            }
            ErrorState.LinkedBankNamesMismatched -> {
                logAnalytics(BankAuthAnalytics.ACCOUNT_MISMATCH, partner)

                with(binding) {
                    linkBankBtn.text = getString(R.string.bank_linking_try_different_bank)
                    linkBankTitle.text = getString(R.string.bank_linking_is_this_your_bank)

                    val linksMap = mapOf<String, Uri>(
                        "yodlee_names_dont_match_learn_more" to Uri.parse(URL_YODLEE_SUPPORT_LEARN_MORE)
                    )

                    val text = stringUtils.getStringWithMappedAnnotations(
                        R.string.bank_linking_already_linked_error_subtitle,
                        linksMap,
                        activity
                    )

                    linkBankSubtitle.text = text
                    linkBankSubtitle.movementMethod = LinkMovementMethod.getInstance()
                }
            }
            ErrorState.LinkedBankRejected -> {
                setTitleAndSubtitle(
                    getString(R.string.bank_linking_rejected_title),
                    getString(R.string.bank_linking_rejected_subtitle)
                )
                binding.linkBankBtn.text = getString(R.string.common_try_again)
            }
            ErrorState.LinkedBankExpired -> {
                setTitleAndSubtitle(
                    getString(R.string.bank_linking_expired_title),
                    getString(R.string.bank_linking_expired_subtitle)
                )
                binding.linkBankBtn.text = getString(R.string.common_try_again)
            }
            ErrorState.LinkedBankFailure -> {
                setTitleAndSubtitle(
                    getString(R.string.bank_linking_failure_title),
                    getString(R.string.bank_linking_failure_subtitle)
                )
                binding.linkBankBtn.text = getString(R.string.common_try_again)
            }
            else -> {
                logAnalytics(BankAuthAnalytics.GENERIC_ERROR, partner)
                setTitleAndSubtitle(
                    getString(R.string.bank_linking_generic_error_title),
                    getString(R.string.bank_linking_generic_error_subtitle)
                )

                binding.linkBankBtn.text = getString(R.string.common_try_again)
            }
        }

        with(binding) {
            linkBankIcon.setImageResource(
                if (state == ErrorState.LinkedBankNamesMismatched) {
                    R.drawable.ic_bank_user
                } else {
                    R.drawable.ic_bank_details_big
                }
            )
            linkBankProgress.gone()
            linkBankStateIndicator.setImageResource(R.drawable.ic_alert_white_bkgd)
            linkBankStateIndicator.visible()
            linkBankBtn.visible()
            linkBankBtn.setOnClickListener {
                logRetryAnalytics(state, partner)
                when {
                    isForApproval -> {
                        approvalData?.let {
                            model.process(BankAuthIntent.UpdateForApproval(it.authorisationUrl))
                        } ?: navigator().retry()
                    }
                    else -> {
                        navigator().retry()
                    }
                }
            }
            linkBankCancel.visible()
            linkBankCancel.setOnClickListener {
                logCancelAnalytics(state, partner)
                navigator().bankAuthCancelled()
            }
        }
    }

    private fun logRetryAnalytics(state: ErrorState, partner: BankPartner?) =
        logAnalytics(
            when (state) {
                ErrorState.LinkedBankAlreadyLinked -> BankAuthAnalytics.ALREADY_LINKED_RETRY
                ErrorState.LinkedBankAccountUnsupported -> BankAuthAnalytics.INCORRECT_ACCOUNT_RETRY
                ErrorState.LinkedBankNamesMismatched -> BankAuthAnalytics.ACCOUNT_MISMATCH_RETRY
                else -> BankAuthAnalytics.GENERIC_ERROR_RETRY
            }, partner
        )

    private fun logCancelAnalytics(state: ErrorState, partner: BankPartner?) =
        logAnalytics(
            when (state) {
                ErrorState.LinkedBankAlreadyLinked -> BankAuthAnalytics.ALREADY_LINKED_CANCEL
                ErrorState.LinkedBankAccountUnsupported -> BankAuthAnalytics.INCORRECT_ACCOUNT_CANCEL
                ErrorState.LinkedBankNamesMismatched -> BankAuthAnalytics.ACCOUNT_MISMATCH_CANCEL
                else -> BankAuthAnalytics.GENERIC_ERROR_CANCEL
            }, partner
        )

    private fun logAnalytics(event: BankAuthAnalytics, partner: BankPartner?) {
        partner?.let {
            analytics.logEvent(bankAuthEvent(event, it))
        }
    }

    private fun showLinkingInProgress(linkBank: LinkBankTransfer?) {
        with(binding) {
            linkBankIcon.setImageResource(R.drawable.ic_blockchain_blue_circle)
            linkBankProgress.visible()
            linkBankStateIndicator.gone()
            linkBankBtn.gone()
            linkBank?.partner?.let {
                when (it) {
                    BankPartner.YAPILY -> {
                        val attrs = linkBank.attributes as YapilyAttributes
                        setTitleAndSubtitle(
                            getString(
                                R.string.yapily_linking_pending_title,
                                attrs.institutionList.find { institution -> institution.id == accountId }?.name
                                    ?: getString(R.string.yapily_linking_default_bank)
                            ),
                            getString(R.string.yapily_linking_pending_subtitle)
                        )
                    }
                    BankPartner.YODLEE -> {
                        setTitleAndSubtitle(
                            getString(R.string.yodlee_linking_title),
                            getString(R.string.yodlee_linking_subtitle)
                        )
                    }
                }.exhaustive
            }
        }
    }

    private fun showLinkingSuccess(label: String, id: String, partner: BankPartner?, currency: String) {
        logAnalytics(BankAuthAnalytics.SUCCESS, partner)

        with(binding) {
            linkBankIcon.setImageResource(R.drawable.ic_bank_details_big)
            linkBankProgress.gone()
            linkBankStateIndicator.setImageResource(R.drawable.ic_check_circle)
            linkBankStateIndicator.visible()
            linkBankBtn.visible()
            linkBankBtn.setOnClickListener {
                navigator().bankLinkingFinished(id, currency)
            }
            setTitleAndSubtitle(
                getString(R.string.bank_linking_success_title),
                getString(R.string.bank_linking_success_subtitle, label)
            )
        }
    }

    private fun logRetryLaunchAnalytics() =
        analytics.logEvent(
            bankAuthEvent(
                if (isForApproval) {
                    BankAuthAnalytics.PIS_EXTERNAL_FLOW_RETRY
                } else {
                    BankAuthAnalytics.AIS_EXTERNAL_FLOW_RETRY
                }, authSource
            )
        )

    private fun navigator(): BankAuthFlowNavigator =
        (activity as? BankAuthFlowNavigator)
            ?: throw IllegalStateException("Parent must implement BankAuthFlowNavigator")

    companion object {
        private const val ACCOUNT_PROVIDER_ID = "ACCOUNT_PROVIDER_ID"
        private const val ACCOUNT_ID = "ACCOUNT_ID"
        private const val LINKING_BANK_ID = "LINKING_BANK_ID"
        private const val LINK_BANK_TRANSFER = "LINK_BANK_TRANSFER"
        private const val LINK_BANK_SOURCE = "LINK_BANK_SOURCE"
        private const val ERROR_STATE = "ERROR_STATE"
        private const val FROM_DEEP_LINK = "FROM_DEEP_LINK"
        private const val FOR_APPROVAL = "FOR_APPROVAL"
        private const val APPROVAL_DATA = "APPROVAL_DATA"

        fun newInstance(
            accountProviderId: String,
            accountId: String,
            linkingBankId: String,
            linkBankTransfer: LinkBankTransfer? = null,
            authSource: BankAuthSource
        ) = BankAuthFragment().apply {
            arguments = Bundle().apply {
                putString(ACCOUNT_PROVIDER_ID, accountProviderId)
                putString(ACCOUNT_ID, accountId)
                putString(LINKING_BANK_ID, linkingBankId)
                putSerializable(LINK_BANK_TRANSFER, linkBankTransfer)
                putSerializable(LINK_BANK_SOURCE, authSource)
            }
        }

        fun newInstance(
            errorState: ErrorState,
            authSource: BankAuthSource
        ) = BankAuthFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ERROR_STATE, errorState)
                putSerializable(LINK_BANK_SOURCE, authSource)
            }
        }

        fun newInstance(
            approvalData: BankPaymentApproval,
            authSource: BankAuthSource
        ) = BankAuthFragment().apply {
            arguments = Bundle().apply {
                putSerializable(APPROVAL_DATA, approvalData)
                putBoolean(FOR_APPROVAL, true)
                putSerializable(LINK_BANK_SOURCE, authSource)
            }
        }

        fun newInstance(
            linkingId: String,
            authSource: BankAuthSource,
            fromDeepLink: Boolean = true
        ) = BankAuthFragment().apply {
            arguments = Bundle().apply {
                putBoolean(FROM_DEEP_LINK, fromDeepLink)
                putString(LINKING_BANK_ID, linkingId)
                putSerializable(LINK_BANK_SOURCE, authSource)
            }
        }
    }
}

interface ChooserCallback {
    fun onItemChosen()
}

class ChooserReceiver : BroadcastReceiver() {
    companion object {
        // must be a static variable for inter process communication
        var listener: ChooserCallback? = null
    }

    fun registerListener(callback: ChooserCallback) {
        listener = callback
    }

    override fun onReceive(context: Context, intent: Intent) {
        // we only get this onReceive if an app has been selected, so notify regardless of which app was chosen
        listener?.run {
            onItemChosen()
        }
    }
}