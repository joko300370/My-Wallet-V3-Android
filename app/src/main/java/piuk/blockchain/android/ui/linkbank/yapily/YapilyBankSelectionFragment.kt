package piuk.blockchain.android.ui.linkbank.yapily

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.nabu.models.data.YapilyAttributes
import com.blockchain.notifications.analytics.Analytics
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.FragmentSimpleBuyYapilyBankSelectBinding
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.linkbank.BankAuthAnalytics
import piuk.blockchain.android.ui.linkbank.BankAuthFlowNavigator
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.bankAuthEvent
import piuk.blockchain.android.ui.linkbank.yapily.adapters.YapilyBanksDelegateAdapter
import piuk.blockchain.android.util.AfterTextChangedWatcher

class YapilyBankSelectionFragment : Fragment() {

    private var _binding: FragmentSimpleBuyYapilyBankSelectBinding? = null
    private val binding: FragmentSimpleBuyYapilyBankSelectBinding
        get() = _binding!!

    private val attributes: YapilyAttributes by lazy {
        arguments?.getSerializable(ATTRS_KEY) as YapilyAttributes
    }

    private val authSource: BankAuthSource by lazy {
        arguments?.getSerializable(LAUNCH_SOURCE) as BankAuthSource
    }

    private val analytics: Analytics by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSimpleBuyYapilyBankSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val banksAdapter = YapilyBanksDelegateAdapter(
            onBankItemClicked = {
                analytics.logEvent(bankAuthEvent(BankAuthAnalytics.SELECT_BANK, authSource))
                navigator().yapilyInstitutionSelected(it, attributes.entity)
            },
            onAddNewBankClicked = {
                analytics.logEvent(bankAuthEvent(BankAuthAnalytics.SELECT_TRANSFER_DETAILS, authSource))
                navigator().showTransferDetails()
            }
        )

        binding.run {
            yapilyBankList.apply {
                adapter = banksAdapter
                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(BlockchainListDividerDecor(requireContext()))
            }
            banksAdapter.items = attributes.institutionList
            yapilySearch.addTextChangedListener(
                object : AfterTextChangedWatcher() {
                    override fun afterTextChanged(s: Editable) {
                        banksAdapter.items =
                            attributes.institutionList.filter {
                                s.isBlank() || it.name.toLowerCase().contains(s.toString().toLowerCase())
                            }
                    }
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun navigator(): BankAuthFlowNavigator =
        (activity as? BankAuthFlowNavigator)
            ?: throw IllegalStateException("Parent must implement BankAuthFlowNavigator")

    companion object {
        private const val ATTRS_KEY: String = "ATTRS_KEY"
        private const val LAUNCH_SOURCE: String = "LAUNCH_SOURCE"

        fun newInstance(attributes: YapilyAttributes, authSource: BankAuthSource): YapilyBankSelectionFragment =
            YapilyBankSelectionFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ATTRS_KEY, attributes)
                    putSerializable(LAUNCH_SOURCE, authSource)
                }
            }
    }
}