package piuk.blockchain.android.ui.transactionflow.flow

import android.view.LayoutInflater
import android.view.ViewGroup
import io.reactivex.Single
import org.koin.android.ext.android.inject
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.databinding.DialogSheetAccountSelectorBinding
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf

class SelectSourceAccountSheet : TransactionFlowSheet<DialogSheetAccountSelectorBinding>() {

    private val customiser: TransactionFlowCustomiser by inject()
    private var availableSources = emptyList<CryptoAccount>()
    override fun render(newState: TransactionState) {
        if (availableSources == newState.availableSources) {
            // If list displays already the same accounts return so to avoid the annoying flickering
            return
        }
        with(binding) {
            accountList.initialise(
                source = Single.just(newState.availableSources.map { it }),
                status = customiser.sourceAccountSelectionStatusDecorator(newState)
            )
            accountListTitle.text = customiser.selectSourceAccountTitle(newState)
            accountListSubtitle.text = customiser.selectSourceAccountSubtitle(newState)
            accountListSubtitle.visible()
            accountListBack.visibleIf { newState.canGoBack }
        }
        availableSources = newState.availableSources
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetAccountSelectorBinding =
        DialogSheetAccountSelectorBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetAccountSelectorBinding) {
        binding.apply {
            accountList.onAccountSelected = {
                require(it is CryptoAccount)
                model.process(TransactionIntent.SourceAccountSelected(it))
            }
            accountListBack.setOnClickListener {
                model.process(TransactionIntent.ReturnToPreviousStep)
            }
            accountList.onListLoaded = ::doOnListLoaded
            accountList.onLoadError = ::doOnLoadError
            accountList.onListLoading = ::doOnListLoading
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