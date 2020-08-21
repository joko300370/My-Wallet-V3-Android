package piuk.blockchain.android.ui.activity.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.ActivityAnalytics
import com.blockchain.swap.nabu.datamanagers.InterestState
import com.blockchain.ui.urllinks.makeBlockExplorerUrl
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.dialog_activity_details_sheet.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.activity.CryptoAccountType
import piuk.blockchain.android.ui.activity.detail.adapter.ActivityDetailsDelegateAdapter
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.androidcoreui.utils.extensions.visible

class CryptoActivityDetailsBottomSheet :
    MviBottomSheet<ActivityDetailsModel, ActivityDetailsIntents, ActivityDetailState>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onShowBankDetailsSelected()
        fun onShowBankCancelOrder()
    }

    override val host: Host by lazy {
        super.host as? Host
            ?: throw IllegalStateException("Host fragment is not a ActivityDetailsBottomSheet.Host")
    }

    override val layoutResource: Int
        get() = R.layout.dialog_activity_details_sheet

    override val model: ActivityDetailsModel by scopedInject()

    private val listAdapter: ActivityDetailsDelegateAdapter by lazy {
        ActivityDetailsDelegateAdapter(
            onActionItemClicked = { onActionItemClicked() },
            onDescriptionItemUpdated = { onDescriptionItemClicked(it) },
            onCancelActionItemClicked = { onCancelActionItemClicked() }
        )
    }

    private val Bundle?.txId
        get() = this?.getString(ARG_TRANSACTION_HASH) ?: throw IllegalArgumentException(
            "Transaction id should not be null")

    private val Bundle?.cryptoCurrency
        get() = this?.getSerializable(ARG_CRYPTO_CURRENCY) as? CryptoCurrency
            ?: throw IllegalArgumentException("Cryptocurrency should not be null")

    private lateinit var currentState: ActivityDetailState

    private val simpleBuySync: SimpleBuySyncFactory by scopedInject()
    private val compositeDisposable = CompositeDisposable()

    override fun initControls(view: View) {
        view.details_list.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            adapter = listAdapter
        }
    }

    override fun render(newState: ActivityDetailState) {
        currentState = newState
        showDescriptionUpdate(newState.descriptionState)

        dialogView.apply {
            title.text = if (newState.isFeeTransaction) {
                getString(R.string.activity_details_title_fee)
            } else {
                newState.transactionType?.let {
                    mapToAction(newState.transactionType)
                }
            }
            amount.text = newState.amount?.toStringWithSymbol()

            newState.transactionType?.let {
                showTransactionTypeUi(newState, dialogView)

                renderCompletedOrPending(newState.isPending, newState.isPendingExecution,
                    newState.confirmations, newState.totalConfirmations, newState.transactionType,
                    newState.isFeeTransaction)
            }
        }

        if (listAdapter.items != newState.listOfItems) {
            listAdapter.items = newState.listOfItems.toList()
            listAdapter.notifyDataSetChanged()
        }
    }

    private fun showInterestUi(
        newState: ActivityDetailState,
        dialogView: View
    ) {
        if (newState.isPending) {
            dialogView.status.text = getString(
                when (newState.interestState) {
                    InterestState.PENDING -> R.string.activity_details_label_pending
                    InterestState.MANUAL_REVIEW -> R.string.activity_details_label_manual_review
                    InterestState.PROCESSING -> R.string.activity_details_label_processing
                    else -> R.string.empty
                })

            showPendingPill()

            if (newState.transactionType == TransactionSummary.TransactionType.DEPOSIT) {
                showConfirmationUi(newState.confirmations, newState.totalConfirmations)
            }
        } else {
            showCompletePill()
        }
    }

    private fun showTransactionTypeUi(state: ActivityDetailState, view: View) {
        if (state.transactionType == TransactionSummary.TransactionType.BUY) {
            showBuyUi(state, view)
        } else if (state.transactionType == TransactionSummary.TransactionType.INTEREST_EARNED ||
            state.transactionType == TransactionSummary.TransactionType.DEPOSIT ||
            state.transactionType == TransactionSummary.TransactionType.WITHDRAW) {

            showInterestUi(state, dialogView)
        }
    }

    private fun showBuyUi(
        state: ActivityDetailState,
        view: View
    ) {
        if (state.isPending || state.isPendingExecution) {
            view.custodial_tx_button.text =
                getString(R.string.activity_details_view_bank_transfer_details)
            view.custodial_tx_button.setOnClickListener {
                host.onShowBankDetailsSelected()
                dismiss()
            }
        } else {
            view.custodial_tx_button.text =
                getString(R.string.activity_details_buy_again)
            view.custodial_tx_button.setOnClickListener {
                analytics.logEvent(ActivityAnalytics.DETAILS_BUY_PURCHASE_AGAIN)
                compositeDisposable += simpleBuySync.performSync().onErrorComplete().observeOn(
                    AndroidSchedulers.mainThread())
                    .subscribe {
                        startActivity(SimpleBuyActivity.newInstance(requireContext(), true))
                        dismiss()
                    }
            }
        }

        view.custodial_tx_button.visible()
    }

    private fun showDescriptionUpdate(descriptionState: DescriptionState) {
        when (descriptionState) {
            DescriptionState.UPDATE_SUCCESS -> Toast.makeText(requireContext(),
                getString(R.string.activity_details_description_updated), Toast.LENGTH_SHORT).show()
            DescriptionState.UPDATE_ERROR -> Toast.makeText(requireContext(),
                getString(R.string.activity_details_description_not_updated), Toast.LENGTH_SHORT)
                .show()
            DescriptionState.NOT_SET -> {
                // do nothing
            }
        }
    }

    private fun renderCompletedOrPending(
        pending: Boolean,
        pendingExecution: Boolean,
        confirmations: Int,
        totalConfirmations: Int,
        transactionType: TransactionSummary.TransactionType?,
        isFeeTransaction: Boolean
    ) {
        dialogView.apply {
            if (pending || pendingExecution) {
                showConfirmationUi(confirmations, totalConfirmations)

                status.text = getString(when {
                    transactionType == TransactionSummary.TransactionType.SENT ||
                        transactionType == TransactionSummary.TransactionType.TRANSFERRED -> {
                        analytics.logEvent(ActivityAnalytics.DETAILS_SEND_CONFIRMING)
                        R.string.activity_details_label_confirming
                    }
                    isFeeTransaction || transactionType == TransactionSummary.TransactionType.SWAP -> {
                        if (isFeeTransaction) {
                            analytics.logEvent(ActivityAnalytics.DETAILS_FEE_PENDING)
                        } else {
                            analytics.logEvent(ActivityAnalytics.DETAILS_SWAP_PENDING)
                        }
                        R.string.activity_details_label_pending
                    }
                    transactionType == TransactionSummary.TransactionType.BUY ->
                        if (pending && !pendingExecution) {
                            analytics.logEvent(ActivityAnalytics.DETAILS_BUY_AWAITING_FUNDS)
                            R.string.activity_details_label_waiting_on_funds
                        } else {
                            analytics.logEvent(ActivityAnalytics.DETAILS_BUY_PENDING)
                            R.string.activity_details_label_pending_execution
                        }
                    else -> R.string.activity_details_label_confirming
                })
                showPendingPill()
            } else {
                showCompletePill()
                logAnalyticsForComplete(transactionType, isFeeTransaction)
            }
        }
    }

    private fun showConfirmationUi(
        confirmations: Int,
        totalConfirmations: Int
    ) {
        if (confirmations != totalConfirmations) {
            dialogView.apply {
                confirmation_label.text =
                    getString(R.string.activity_details_label_confirmations, confirmations,
                        totalConfirmations)
                confirmation_progress.setProgress(
                    (confirmations / totalConfirmations.toFloat()) * 100)
                confirmation_label.visible()
                confirmation_progress.visible()
            }
        }
    }

    private fun showPendingPill() {
        dialogView.apply {
            status.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bkgd_status_unconfirmed)
            status.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.grey_800))
        }
    }

    private fun showCompletePill() {
        dialogView.apply {
            status.text = getString(R.string.activity_details_label_complete)
            status.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bkgd_status_received)
            status.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.green_600))
        }
    }

    private fun onDescriptionItemClicked(description: String) {
        model.process(
            UpdateDescriptionIntent(arguments.txId, arguments.cryptoCurrency, description))
    }

    private fun onCancelActionItemClicked() {
        analytics.logEvent(ActivityAnalytics.DETAILS_BUY_CANCEL)
        host.onShowBankCancelOrder()
        dismiss()
    }

    private fun onActionItemClicked() {
        val explorerUri = makeBlockExplorerUrl(arguments.cryptoCurrency, arguments.txId)
        logAnalyticsForExplorer()
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(explorerUri)
            startActivity(this)
        }
    }

    private fun mapToAction(transactionType: TransactionSummary.TransactionType?): String =
        when (transactionType) {
            TransactionSummary.TransactionType.TRANSFERRED -> getString(
                R.string.activity_details_title_transferred)
            TransactionSummary.TransactionType.RECEIVED -> getString(
                R.string.activity_details_title_received)
            TransactionSummary.TransactionType.SENT -> getString(R.string.activity_details_title_sent)
            TransactionSummary.TransactionType.BUY -> getString(R.string.activity_details_title_buy)
            TransactionSummary.TransactionType.SELL -> getString(R.string.activity_details_title_sell)
            TransactionSummary.TransactionType.SWAP -> getString(R.string.activity_details_title_swap)
            TransactionSummary.TransactionType.DEPOSIT -> getString(
                R.string.activity_details_title_deposit)
            TransactionSummary.TransactionType.WITHDRAW -> getString(
                R.string.activity_details_title_withdraw)
            TransactionSummary.TransactionType.INTEREST_EARNED -> getString(
                R.string.activity_details_title_interest_earned)
            else -> ""
        }

    private fun logAnalyticsForExplorer() {
        when {
            currentState.isFeeTransaction ->
                analytics.logEvent(ActivityAnalytics.DETAILS_FEE_VIEW_EXPLORER)
            currentState.transactionType == TransactionSummary.TransactionType.SENT ->
                analytics.logEvent(ActivityAnalytics.DETAILS_SEND_VIEW_EXPLORER)
            currentState.transactionType == TransactionSummary.TransactionType.SWAP ->
                analytics.logEvent(ActivityAnalytics.DETAILS_SWAP_VIEW_EXPLORER)
            currentState.transactionType == TransactionSummary.TransactionType.RECEIVED ->
                analytics.logEvent(ActivityAnalytics.DETAILS_RECEIVE_VIEW_EXPLORER)
        }
    }

    private fun logAnalyticsForComplete(
        transactionType: TransactionSummary.TransactionType?,
        isFeeTransaction: Boolean
    ) {
        when {
            isFeeTransaction ->
                analytics.logEvent(ActivityAnalytics.DETAILS_FEE_COMPLETE)
            transactionType == TransactionSummary.TransactionType.SENT ->
                analytics.logEvent(ActivityAnalytics.DETAILS_SEND_COMPLETE)
            transactionType == TransactionSummary.TransactionType.SWAP ->
                analytics.logEvent(ActivityAnalytics.DETAILS_SWAP_COMPLETE)
            transactionType == TransactionSummary.TransactionType.BUY ->
                analytics.logEvent(ActivityAnalytics.DETAILS_BUY_COMPLETE)
            transactionType == TransactionSummary.TransactionType.RECEIVED ->
                analytics.logEvent(ActivityAnalytics.DETAILS_RECEIVE_COMPLETE)
        }
    }

    private fun loadActivityDetails(
        cryptoCurrency: CryptoCurrency,
        txHash: String,
        accountType: CryptoAccountType
    ) {
        model.process(LoadActivityDetailsIntent(cryptoCurrency, txHash, accountType))
    }

    override fun onDestroy() {
        model.destroy()
        super.onDestroy()
    }

    companion object {
        private const val ARG_CRYPTO_CURRENCY = "crypto_currency"
        private const val ARG_TRANSACTION_HASH = "tx_hash"

        fun newInstance(
            cryptoCurrency: CryptoCurrency,
            txHash: String,
            accountType: CryptoAccountType
        ): CryptoActivityDetailsBottomSheet {
            return CryptoActivityDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CRYPTO_CURRENCY, cryptoCurrency)
                    putString(ARG_TRANSACTION_HASH, txHash)
                }

                loadActivityDetails(cryptoCurrency, txHash, accountType)
            }
        }
    }
}