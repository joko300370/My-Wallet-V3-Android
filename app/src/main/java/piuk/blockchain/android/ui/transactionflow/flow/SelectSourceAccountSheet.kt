package piuk.blockchain.android.ui.transactionflow.flow

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import io.reactivex.Single
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.databinding.DialogSheetAccountSelectorBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.transactionflow.engine.BankLinkingState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.SourceSelectionCustomisations
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf

class SelectSourceAccountSheet : TransactionFlowSheet<DialogSheetAccountSelectorBinding>() {
    private val customiser: SourceSelectionCustomisations by inject()

    private var availableSources: List<BlockchainAccount>? = null
    private var linkingBankState: BankLinkingState = BankLinkingState.NotStarted

    override fun render(newState: TransactionState) {
        if (availableSources != newState.availableSources)
            updateSources(newState)

        if (newState.linkBankState != BankLinkingState.NotStarted && linkingBankState != newState.linkBankState)
            handleBankLinking(newState)

        availableSources = newState.availableSources
        linkingBankState = newState.linkBankState
    }

    private fun handleBankLinking(
        newState: TransactionState
    ) {
        binding.progress.gone()

        if (newState.linkBankState is BankLinkingState.Success) {
            startActivityForResult(
                BankAuthActivity.newInstance(
                    newState.linkBankState.bankTransferInfo,
                    customiser.getLinkingSourceForAction(newState),
                    requireActivity()
                ),
                BankAuthActivity.LINK_BANK_REQUEST_CODE
            )
        } else {
            ToastCustom.makeText(
                requireContext(), getString(R.string.common_error), Toast.LENGTH_SHORT,
                ToastCustom.TYPE_ERROR
            )
        }
    }

    private fun updateSources(newState: TransactionState) {
        with(binding) {
            accountList.initialise(
                source = Single.just(newState.availableSources.map { it }),
                status = customiser.sourceAccountSelectionStatusDecorator(newState)
            )
            accountListTitle.text = customiser.selectSourceAccountTitle(newState)
            accountListSubtitle.text = customiser.selectSourceAccountSubtitle(newState)
            accountListSubtitle.visible()
            accountListBack.visibleIf { newState.canGoBack }
            addMethod.visibleIf { customiser.selectSourceShouldShowAddNew(newState) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BankAuthActivity.LINK_BANK_REQUEST_CODE && resultCode == RESULT_OK) {
            binding.progress.visible()
            model.process(
                TransactionIntent.RefreshSourceAccounts
            )
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetAccountSelectorBinding =
        DialogSheetAccountSelectorBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetAccountSelectorBinding) {
        binding.apply {
            accountList.onAccountSelected = {
                model.process(TransactionIntent.SourceAccountSelected(it))
                analyticsHooks.onSourceAccountSelected(it, state)
            }
            accountListBack.setOnClickListener {
                model.process(TransactionIntent.ReturnToPreviousStep)
            }
            accountList.onListLoaded = ::doOnListLoaded
            accountList.onLoadError = ::doOnLoadError
            accountList.onListLoading = ::doOnListLoading

            addMethod.setOnClickListener {
                binding.progress.visible()
                model.process(TransactionIntent.StartLinkABank)
            }
        }
    }

    private fun doOnListLoaded(isEmpty: Boolean) {
        binding.accountListEmpty.visibleIf { isEmpty }
        binding.progress.gone()
    }

    private fun doOnLoadError(it: Throwable) {
        binding.accountListEmpty.visible()
        binding.progress.gone()
    }

    private fun doOnListLoading() {
        binding.accountListEmpty.gone()
        binding.progress.visible()
    }
}