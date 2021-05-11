package piuk.blockchain.android.ui.linkbank

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.models.data.BankPartner
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.nabu.models.data.YapilyAttributes
import com.blockchain.nabu.models.data.YapilyInstitution
import com.blockchain.nabu.models.data.YodleeAttributes
import com.blockchain.preferences.BankLinkingPrefs
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.ErrorState
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.linkbank.yapily.YapilyBankSelectionFragment
import piuk.blockchain.android.ui.linkbank.yapily.YapilyPermissionFragment
import piuk.blockchain.android.ui.linkbank.yodlee.YodleeSplashFragment
import piuk.blockchain.android.ui.linkbank.yodlee.YodleeWebViewFragment
import piuk.blockchain.androidcore.utils.helperfunctions.consume

class BankAuthActivity : BlockchainActivity(), BankAuthFlowNavigator,
    SlidingModalBottomDialog.Host {

    private val linkBankTransfer: LinkBankTransfer
        get() = intent.getSerializableExtra(LINK_BANK_TRANSFER_KEY) as LinkBankTransfer

    private val approvalDetails: BankPaymentApproval?
        get() = intent.getSerializableExtra(LINK_BANK_APPROVAL) as? BankPaymentApproval

    private val errorState: ErrorState?
        get() = intent.getSerializableExtra(ERROR_STATE) as? ErrorState

    private val authSource: BankAuthSource
        get() = intent.getSerializableExtra(LINK_BANK_SOURCE) as BankAuthSource

    private val linkingId: String
        get() = intent.getStringExtra(LINK_BANK_ID) ?: ""

    private val isFromDeepLink: Boolean
        get() = intent.getBooleanExtra(LAUNCHED_FROM_DEEP_LINK, false)

    private val bankLinkingPrefs: BankLinkingPrefs by scopedInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_activity)
        setSupportActionBar(toolbar_general)

        if (savedInstanceState == null) {
            when {
                isFromDeepLink -> {
                    setupToolbar(R.string.link_a_bank)
                    checkBankLinkingState(linkingId)
                }
                approvalDetails != null -> {
                    approvalDetails?.let {
                        setupToolbar(R.string.approve_payment)
                        launchYapilyApproval(it)
                    } ?: launchBankLinkingWithError(ErrorState.GenericError)
                }
                errorState != null ->
                    errorState?.let {
                        launchBankLinkingWithError(it)
                    }

                else -> {
                    setupToolbar(R.string.link_a_bank)
                    checkPartnerAndLaunchFlow(linkBankTransfer)
                }
            }
        }
    }

    private fun checkBankLinkingState(linkingId: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, BankAuthFragment.newInstance(linkingId, authSource))
            .commitAllowingStateLoss()
    }

    private fun checkPartnerAndLaunchFlow(linkBankTransfer: LinkBankTransfer) {
        when (linkBankTransfer.partner) {
            BankPartner.YODLEE -> {
                val attributes = linkBankTransfer.attributes as YodleeAttributes
                launchYodleeSplash(attributes, linkBankTransfer.id)
            }
            BankPartner.YAPILY -> {
                launchYapilyBankSelection(linkBankTransfer.attributes as YapilyAttributes)
            }
        }
    }

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun launchYapilyBankSelection(attributes: YapilyAttributes) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, YapilyBankSelectionFragment.newInstance(attributes, authSource))
            .commitAllowingStateLoss()
    }

    override fun showTransferDetails() {
        showBottomSheet(WireTransferAccountDetailsBottomSheet.newInstance())
    }

    override fun yapilyInstitutionSelected(institution: YapilyInstitution, entity: String) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame, YapilyPermissionFragment.newInstance(
                    institution = institution,
                    entity = entity,
                    authSource = authSource
                )
            )
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    private fun launchYapilyApproval(approvalDetails: BankPaymentApproval) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame, YapilyPermissionFragment.newInstance(approvalDetails, authSource)
            )
            .commitAllowingStateLoss()
    }

    override fun yapilyAgreementAccepted(institution: YapilyInstitution) {
        launchBankLinking(
            accountProviderId = "",
            accountId = institution.id,
            bankId = linkBankTransfer.id
        )
    }

    override fun yapilyApprovalAccepted(approvalDetails: BankPaymentApproval) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame,
                BankAuthFragment.newInstance(approvalDetails, authSource)
            )
            .commitAllowingStateLoss()
    }

    override fun yapilyAgreementCancelled(isFromApproval: Boolean) =
        if (isFromApproval) {
            resetLocalState()
        } else {
            supportFragmentManager.popBackStack()
        }

    override fun onBackPressed() =
        if (approvalDetails != null) {
            resetLocalState()
        } else {
            super.onBackPressed()
        }

    private fun resetLocalState() {
        bankLinkingPrefs.setBankLinkingState(BankAuthDeepLinkState().toPreferencesValue())
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun launchYodleeSplash(attributes: YodleeAttributes, bankId: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, YodleeSplashFragment.newInstance(attributes, bankId))
            .commitAllowingStateLoss()
    }

    override fun launchYodleeWebview(attributes: YodleeAttributes, bankId: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, YodleeWebViewFragment.newInstance(attributes, bankId))
            .addToBackStack(YodleeWebViewFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun launchBankLinking(accountProviderId: String, accountId: String, bankId: String) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame,
                BankAuthFragment.newInstance(
                    accountProviderId = accountProviderId,
                    accountId = accountId,
                    linkingBankId = bankId,
                    linkBankTransfer = linkBankTransfer,
                    authSource = authSource
                )
            )
            .addToBackStack(BankAuthFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    private fun launchBankLinkingWithError(errorState: ErrorState) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, BankAuthFragment.newInstance(errorState, authSource))
            .addToBackStack(BankAuthFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun retry() {
        when {
            isFromDeepLink -> {
                checkBankLinkingState(linkingId)
            }
            approvalDetails != null -> {
                approvalDetails?.let {
                    launchYapilyApproval(it)
                }
            }
            else -> onBackPressed()
        }
    }

    override fun bankLinkingFinished(bankId: String, currency: String) {
        val data = Intent()
        data.putExtra(LINKED_BANK_ID_KEY, bankId)
        data.putExtra(LINKED_BANK_CURRENCY, currency)
        setResult(RESULT_OK, data)
        finish()
    }

    override fun bankAuthCancelled() {
        resetLocalState()
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressed()
    }

    override fun onSheetClosed() {
        // do nothing
    }

    companion object {
        private const val LINK_BANK_TRANSFER_KEY = "LINK_BANK_TRANSFER_KEY"
        private const val LINK_BANK_ID = "LINK_BANK_TRANSFER_KEY"
        private const val LINK_BANK_SOURCE = "LINK_BANK_SOURCE"
        private const val LINK_BANK_APPROVAL = "LINK_BANK_APPROVAL"
        private const val LAUNCHED_FROM_DEEP_LINK = "LAUNCHED_FROM_DEEP_LINK"
        private const val ERROR_STATE = "ERROR_STATE"

        const val LINK_BANK_REQUEST_CODE = 999
        const val LINKED_BANK_ID_KEY = "LINKED_BANK_ID"
        const val LINKED_BANK_CURRENCY = "LINKED_BANK_CURRENCY"

        fun newInstance(
            linkBankTransfer: LinkBankTransfer,
            authSource: BankAuthSource,
            context: Context
        ): Intent {
            val intent = Intent(context, BankAuthActivity::class.java)
            intent.putExtra(LINK_BANK_TRANSFER_KEY, linkBankTransfer)
            intent.putExtra(LINK_BANK_SOURCE, authSource)
            return intent
        }

        fun newInstance(linkingId: String, authSource: BankAuthSource, context: Context): Intent {
            val intent = Intent(context, BankAuthActivity::class.java)
            intent.putExtra(LINK_BANK_ID, linkingId)
            intent.putExtra(LAUNCHED_FROM_DEEP_LINK, true)
            intent.putExtra(LINK_BANK_SOURCE, authSource)
            return intent
        }

        fun newInstance(approvalData: BankPaymentApproval, authSource: BankAuthSource, context: Context): Intent {
            val intent = Intent(context, BankAuthActivity::class.java)
            intent.putExtra(LINK_BANK_APPROVAL, approvalData)
            intent.putExtra(LINK_BANK_SOURCE, authSource)
            return intent
        }

        fun newInstance(errorState: ErrorState, authSource: BankAuthSource, context: Context): Intent {
            val intent = Intent(context, BankAuthActivity::class.java)
            intent.putExtra(ERROR_STATE, errorState)
            intent.putExtra(LINK_BANK_SOURCE, authSource)
            return intent
        }
    }
}