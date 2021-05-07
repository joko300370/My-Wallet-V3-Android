package piuk.blockchain.android.ui.linkbank.yapily

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.nabu.models.data.YapilyInstitution
import com.blockchain.notifications.analytics.Analytics
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentYapilyAgreementBinding
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.linkbank.BankAuthAnalytics
import piuk.blockchain.android.ui.linkbank.BankAuthFlowNavigator
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.BankPaymentApproval
import piuk.blockchain.android.ui.linkbank.bankAuthEvent
import piuk.blockchain.android.ui.linkbank.yapily.adapters.YapilyAgreementDelegateAdapter
import piuk.blockchain.android.ui.linkbank.yapily.adapters.YapilyApprovalDelegateAdapter
import piuk.blockchain.android.ui.linkbank.yapily.adapters.YapilyPermissionItem
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomiserImpl.Companion.getEstimatedTransactionCompletionTime

class YapilyPermissionFragment : Fragment() {

    private var _binding: FragmentYapilyAgreementBinding? = null
    private val binding: FragmentYapilyAgreementBinding
        get() = _binding!!

    private val institution: YapilyInstitution by lazy {
        arguments?.getSerializable(INSTITUTION) as YapilyInstitution
    }

    private val approvalDetails: BankPaymentApproval by lazy {
        arguments?.getSerializable(APPROVAL_DETAILS) as BankPaymentApproval
    }

    private val entity: String by lazy {
        arguments?.getString(ENTITY) ?: approvalDetails.linkedBank.entity
    }

    private val authSource: BankAuthSource by lazy {
        arguments?.getSerializable(LAUNCH_SOURCE) as BankAuthSource
    }

    private val isForApproval
        get() = arguments?.getBoolean(IS_APPROVAL, false) ?: false

    private val analytics: Analytics by inject()

    private fun navigator(): BankAuthFlowNavigator =
        (activity as? BankAuthFlowNavigator)
            ?: throw IllegalStateException("Parent must implement BankAuthFlowNavigator")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentYapilyAgreementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemsAdapter = if (isForApproval) {
            val approvalAdapter = YapilyApprovalDelegateAdapter()
            with(approvalAdapter) {
                onExpandableListItemClicked = { position ->
                    (items[position] as YapilyPermissionItem.YapilyExpandableListItem).run {
                        isExpanded = !isExpanded
                        playAnimation = true
                    }
                    notifyItemChanged(position)
                }
                onExpandableItemClicked = { position ->
                    (items[position] as YapilyPermissionItem.YapilyExpandableItem).run {
                        isExpanded = !isExpanded
                        playAnimation = true
                    }
                    notifyItemChanged(position)
                }

                items = buildApprovalItemList()

                initialise()
            }

            approvalAdapter
        } else {
            val agreementAdapter = YapilyAgreementDelegateAdapter()

            with(agreementAdapter) {
                onExpandableItemClicked = { position ->
                    (items[position] as YapilyPermissionItem.YapilyExpandableItem).run {
                        isExpanded = !isExpanded
                        playAnimation = true
                    }
                    notifyItemChanged(position)
                }

                items = buildItemList()

                initialise()
            }
            agreementAdapter
        }

        with(binding) {
            agreementItems.apply {
                adapter = itemsAdapter
                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(BlockchainListDividerDecor(requireContext()))
            }
            agreementApproveCta.setOnClickListener {
                logApproveAnalytics()
                if (isForApproval) {
                    navigator().yapilyApprovalAccepted(approvalDetails)
                } else {
                    navigator().yapilyAgreementAccepted(institution)
                }
            }
            agreementDenyCta.setOnClickListener {
                logDenyAnalytics()
                navigator().yapilyAgreementCancelled(isForApproval)
            }
        }
    }

    private fun buildApprovalItemList(): List<YapilyPermissionItem> =
        listOf(
            YapilyPermissionItem.YapilyHeaderItem(
                icon = R.drawable.ic_safeconnect,
                title = entity
            ),
            YapilyPermissionItem.YapilyInfoItem(
                title = getString(R.string.common_payment),
                info = approvalDetails.orderValue.toStringWithSymbol()
            ),
            YapilyPermissionItem.YapilyExpandableListItem(
                title = R.string.yapily_approval_title,
                items = listOf(
                    YapilyPermissionItem.YapilyInfoItem(
                        getString(R.string.yapily_approval_subtitle_1), approvalDetails.linkedBank.accountName
                    ),
                    getSortCodeOrIban(),
                    getAccountNumberOrSwift()
                )
            ),
            YapilyPermissionItem.YapilyExpandableItem(
                title = R.string.yapily_agreement_title_1,
                blurb = getString(R.string.yapily_agreement_blurb_1_1, entity)
            ),
            YapilyPermissionItem.YapilyExpandableItem(
                title = R.string.yapily_agreement_title_2,
                blurb = getString(R.string.yapily_agreement_blurb_2)
            ),
            YapilyPermissionItem.YapilyExpandableItem(
                title = R.string.yapily_agreement_title_3,
                blurb = getString(R.string.yapily_agreement_blurb_3_1, entity)
            ),
            YapilyPermissionItem.YapilyStaticItem(
                blurb = R.string.yapily_approval_blurb,
                bankName = approvalDetails.linkedBank.accountName
            ),
            YapilyPermissionItem.YapilyExpandableItem(
                title = R.string.yapily_agreement_title_4,
                blurb = getString(R.string.yapily_agreement_blurb_4_2, entity)
            )
        )

    private fun getSortCodeOrIban(): YapilyPermissionItem.YapilyInfoItem {
        return if (approvalDetails.linkedBank.sortCode.isNotEmpty()) {
            YapilyPermissionItem.YapilyInfoItem(
                getString(R.string.yapily_approval_subtitle_2_uk), approvalDetails.linkedBank.sortCode
            )
        } else {
            YapilyPermissionItem.YapilyInfoItem(
                getString(R.string.yapily_approval_subtitle_2_eu), approvalDetails.linkedBank.accountIban
            )
        }
    }

    private fun getAccountNumberOrSwift(): YapilyPermissionItem.YapilyInfoItem {
        return if (approvalDetails.linkedBank.accountNumber.isNotEmpty()) {
            YapilyPermissionItem.YapilyInfoItem(
                getString(R.string.yapily_approval_subtitle_3_uk), approvalDetails.linkedBank.accountNumber
            )
        } else {
            YapilyPermissionItem.YapilyInfoItem(
                getString(R.string.yapily_approval_subtitle_3_eu), approvalDetails.linkedBank.bic
            )
        }
    }

    private fun buildItemList(): List<YapilyPermissionItem> =
        listOf(
            YapilyPermissionItem.YapilyHeaderItem(
                icon = R.drawable.ic_safeconnect,
                title = entity
            ),
            YapilyPermissionItem.YapilyExpandableItem(
                title = R.string.yapily_agreement_title_1,
                blurb = getString(R.string.yapily_agreement_blurb_1_1, entity)
            ),
            YapilyPermissionItem.YapilyExpandableItem(
                title = R.string.yapily_agreement_title_2,
                blurb = getString(R.string.yapily_agreement_blurb_2)
            ),
            YapilyPermissionItem.YapilyExpandableItem(
                title = R.string.yapily_agreement_title_3,
                blurb = getString(R.string.yapily_agreement_blurb_3_1, entity)
            ),
            YapilyPermissionItem.YapilyStaticItem(
                blurb = R.string.yapily_agreement_blurb_5_1,
                bankName = institution.name
            ),
            YapilyPermissionItem.YapilyExpandableItem(
                title = R.string.yapily_agreement_title_4,
                blurb = getString(
                    R.string.yapily_agreement_blurb_4_1, entity, getEstimatedTransactionCompletionTime(90)
                )
            )
        )

    private fun logApproveAnalytics() =
        analytics.logEvent(
            bankAuthEvent(
                if (isForApproval) {
                    BankAuthAnalytics.PIS_PERMISSIONS_APPROVED
                } else {
                    BankAuthAnalytics.AIS_PERMISSIONS_APPROVED
                }, authSource
            )
        )

    private fun logDenyAnalytics() =
        analytics.logEvent(
            bankAuthEvent(
                if (isForApproval) {
                    BankAuthAnalytics.PIS_PERMISSIONS_DENIED
                } else {
                    BankAuthAnalytics.AIS_PERMISSIONS_DENIED
                }, authSource
            )
        )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val INSTITUTION = "INSTITUTION"
        private const val APPROVAL_DETAILS = "APPROVAL_DETAILS"
        private const val IS_APPROVAL = "IS_APPROVAL"
        private const val ENTITY = "ENTITY"
        private const val LAUNCH_SOURCE: String = "LAUNCH_SOURCE"

        fun newInstance(
            institution: YapilyInstitution,
            entity: String,
            authSource: BankAuthSource
        ): YapilyPermissionFragment =
            YapilyPermissionFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(INSTITUTION, institution)
                    putString(ENTITY, entity)
                    putSerializable(LAUNCH_SOURCE, authSource)
                }
            }

        fun newInstance(
            approvalDetails: BankPaymentApproval,
            authSource: BankAuthSource
        ): YapilyPermissionFragment =
            YapilyPermissionFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_APPROVAL, true)
                    putSerializable(APPROVAL_DETAILS, approvalDetails)
                    putSerializable(LAUNCH_SOURCE, authSource)
                }
            }
    }
}