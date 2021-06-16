package piuk.blockchain.android.ui.activity.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.InterestState
import com.blockchain.nabu.datamanagers.RecurringBuyErrorState
import com.blockchain.nabu.datamanagers.RecurringBuyTransactionState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.notifications.analytics.ActivityAnalytics
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.ui.urllinks.URL_BLOCKCHAIN_SUPPORT_PORTAL
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.databinding.DialogSheetActivityDetailsBinding
import piuk.blockchain.android.simplebuy.BuySellClicked
import piuk.blockchain.android.simplebuy.BuySellType
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.activity.detail.adapter.ActivityDetailsDelegateAdapter
import piuk.blockchain.android.ui.base.HostedBottomSheet
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class CryptoActivityDetailsBottomSheet : MviBottomSheet<ActivityDetailsModel,
    ActivityDetailsIntents,
    ActivityDetailState,
    DialogSheetActivityDetailsBinding>() {

    interface Host : HostedBottomSheet.Host {
        fun onAddCash(currency: String)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a CryptoActivityDetailsBottomSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetActivityDetailsBinding =
        DialogSheetActivityDetailsBinding.inflate(inflater, container, false)

    override val model: ActivityDetailsModel by scopedInject()
    private val compositeDisposable = CompositeDisposable()

    private val listAdapter: ActivityDetailsDelegateAdapter by lazy {
        ActivityDetailsDelegateAdapter(
            onActionItemClicked = { onActionItemClicked() },
            onDescriptionItemUpdated = { onDescriptionItemClicked(it) }
        )
    }

    private val Bundle?.txId
        get() = this?.getString(ARG_TRANSACTION_HASH) ?: throw IllegalArgumentException(
            "Transaction id should not be null"
        )

    private val Bundle?.cryptoCurrency
        get() = this?.getSerializable(ARG_CRYPTO_CURRENCY) as? CryptoCurrency
            ?: throw IllegalArgumentException("Cryptocurrency should not be null")

    private val Bundle?.activityType
        get() = this?.getSerializable(ARG_ACTIVITY_TYPE) as? CryptoActivityType
            ?: throw IllegalArgumentException("ActivityDetailsType should not be null")

    private lateinit var currentState: ActivityDetailState

    private val simpleBuySync: SimpleBuySyncFactory by scopedInject()

    private val assetResources: AssetResources by scopedInject()

    override fun initControls(binding: DialogSheetActivityDetailsBinding) {
        loadActivityDetails(arguments.cryptoCurrency, arguments.txId, arguments.activityType)
        binding.detailsList.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
            adapter = listAdapter
        }
    }

    override fun render(newState: ActivityDetailState) {
        currentState = newState
        showDescriptionUpdate(newState.descriptionState)

        binding.apply {
            title.text = if (newState.isFeeTransaction) {
                getString(R.string.activity_details_title_fee)
            } else {
                newState.transactionType?.let {
                    mapToAction(newState.transactionType)
                }
            }
            amount.text = newState.amount?.toStringWithSymbol()

            newState.transactionType?.let {
                renderCompletedPendingOrFailed(
                    newState.isPending,
                    newState.isPendingExecution,
                    newState.confirmations,
                    newState.totalConfirmations,
                    newState.transactionType,
                    newState.isFeeTransaction
                )

                showTransactionTypeUi(newState)
            }
        }

        if (newState.isError) {
            showFailedPill()
        }

        if (listAdapter.items != newState.listOfItems) {
            listAdapter.items = newState.listOfItems.toList()
            listAdapter.notifyDataSetChanged()
        }
    }

    private fun showInterestUi(
        newState: ActivityDetailState
    ) {
        if (newState.isPending) {
            binding.status.text = getString(
                when (newState.interestState) {
                    InterestState.PENDING -> R.string.activity_details_label_pending
                    InterestState.MANUAL_REVIEW -> R.string.activity_details_label_manual_review
                    InterestState.PROCESSING -> R.string.activity_details_label_processing
                    else -> R.string.empty
                }
            )
            showPendingPill()

            if (newState.transactionType == TransactionSummary.TransactionType.DEPOSIT) {
                showConfirmationUi(newState.confirmations, newState.totalConfirmations)
            }
        } else if (newState.interestState == InterestState.FAILED) {
            showFailedPill()
        } else {
            showCompletePill()
        }
    }

    private fun showTransactionTypeUi(state: ActivityDetailState) {
        when (state.transactionType) {
            TransactionSummary.TransactionType.BUY -> showBuyUi(state)
            TransactionSummary.TransactionType.RECURRING_BUY -> showRecurringBuyUi(state)
            TransactionSummary.TransactionType.INTEREST_EARNED,
            TransactionSummary.TransactionType.DEPOSIT,
            TransactionSummary.TransactionType.WITHDRAW -> showInterestUi(state)
            else -> { }
        }
    }

    private fun showRecurringBuyUi(state: ActivityDetailState) {
        binding.rbSheetCancel.apply {
            binding.rbSheetCancel.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.settings_bank_remove_check_title)
                    .setMessage(R.string.recurring_buy_cancel_dialog_desc)
                    .setPositiveButton(R.string.common_ok) { di, _ ->
                        di.dismiss()
                        model.process(DeleteRecurringBuy)
                    }
                    .setNegativeButton(R.string.common_cancel) { di, _ ->
                        di.dismiss()
                    }.show()
            }
        }
        if (state.recurringBuyId == null) {
            ToastCustom.makeText(
                requireContext(), getString(R.string.recurring_buy_cancelled_toast), Toast.LENGTH_LONG,
                ToastCustom.TYPE_OK
            )
            dismiss()
        }

        if (state.hasDeleteError) {
            ToastCustom.makeText(
                requireContext(), getString(R.string.recurring_buy_cancelled_error_toast), Toast.LENGTH_LONG,
                ToastCustom.TYPE_ERROR
            )
        }

        if (state.recurringBuyHasFailedAndCanBeFixedByAddingFunds()) {
            binding.custodialTxButton.apply {
                visible()
                text = getString(R.string.activity_details_label_btn_cash)
                setOnClickListener {
                    state.recurringBuyOriginCurrency?.let { launchDepositFlow(it) }
                }
            }
        }
        setErrorMessageAndLinks(state.recurringBuyError, state.recurringBuyState)
    }

    // TODO recurringBuyState needs to be added by BE
    private fun ActivityDetailState.recurringBuyHasFailedAndCanBeFixedByAddingFunds(): Boolean {
        return this.recurringBuyPaymentMethodType == PaymentMethodType.FUNDS &&
            this.recurringBuyError == RecurringBuyErrorState.INSUFFICIENT_FUNDS
        // && this.recurringBuyState == RecurringBuyState.ACTIVE
    }

    private fun launchDepositFlow(originCurrency: String) {
        host.onAddCash(originCurrency)
    }

    private fun setErrorMessageAndLinks(
        errorState: RecurringBuyErrorState,
        transactionState: RecurringBuyTransactionState
    ) {
        val linksMap = mapOf<String, Uri>(
            "contact_support_link" to Uri.parse(URL_BLOCKCHAIN_SUPPORT_PORTAL)
        )

        val errorExplanation = StringUtils.getStringWithMappedAnnotations(
            requireContext(),
            toErrorMessage(errorState, transactionState),
            linksMap
        )
        binding.errorReason.apply {
            visible()
            movementMethod = LinkMovementMethod.getInstance()
            text = errorExplanation
        }
    }

    private fun showBuyUi(
        state: ActivityDetailState
    ) {
        if (state.isPending || state.isPendingExecution) {
            binding.custodialTxButton.gone()
            return
        }
        binding.custodialTxButton.text =
            getString(R.string.activity_details_buy_again)
        binding.custodialTxButton.setOnClickListener {
            analytics.logEvent(ActivityAnalytics.DETAILS_BUY_PURCHASE_AGAIN)
            compositeDisposable += simpleBuySync.performSync().onErrorComplete().observeOn(
                AndroidSchedulers.mainThread()
            )
                .subscribe {
                    analytics.logEvent(
                        BuySellClicked(origin = LaunchOrigin.TRANSACTION_DETAILS, type = BuySellType.BUY)
                    )
                    startActivity(
                        SimpleBuyActivity.newInstance(requireContext(), arguments.cryptoCurrency, true)
                    )
                    dismiss()
                }
        }
        binding.custodialTxButton.visible()
    }

    private fun showDescriptionUpdate(descriptionState: DescriptionState) {
        when (descriptionState) {
            DescriptionState.UPDATE_SUCCESS -> Toast.makeText(
                requireContext(),
                getString(R.string.activity_details_description_updated), Toast.LENGTH_SHORT
            ).show()
            DescriptionState.UPDATE_ERROR -> Toast.makeText(
                requireContext(),
                getString(R.string.activity_details_description_not_updated), Toast.LENGTH_SHORT
            )
                .show()
            DescriptionState.NOT_SET -> {
                // do nothing
            }
        }
    }

    private fun toErrorMessage(
        errorState: RecurringBuyErrorState,
        transactionState: RecurringBuyTransactionState
    ) = when (errorState) {
        RecurringBuyErrorState.INTERNAL_SERVER_ERROR ->
            if (transactionState == RecurringBuyTransactionState.PENDING) {
                // Pending: transaction has failed but will retry after 1 hour
                R.string.recurring_buy_internal_server_error
            } else {
                R.string.recurring_buy_final_attempt_error
            }
        RecurringBuyErrorState.TRADING_LIMITS_EXCEED -> R.string.recurring_buy_limits_exceed_error
        RecurringBuyErrorState.INSUFFICIENT_FUNDS -> R.string.recurring_buy_insufficient_funds_error
        RecurringBuyErrorState.BLOCKED_BENEFICIARY_ID -> R.string.recurring_buy_beneficiary_error
        RecurringBuyErrorState.UNKNOWN -> R.string.recurring_buy_generic_error
    }

    private fun renderCompletedPendingOrFailed(
        pending: Boolean,
        pendingExecution: Boolean,
        confirmations: Int,
        totalConfirmations: Int,
        transactionType: TransactionSummary.TransactionType?,
        isFeeTransaction: Boolean
    ) {
        binding.apply {
            when {
                pending || pendingExecution -> {
                    showConfirmationUi(confirmations, totalConfirmations)
                    status.text = getString(
                        when {
                            transactionType == TransactionSummary.TransactionType.SENT ||
                                transactionType == TransactionSummary.TransactionType.TRANSFERRED -> {
                                analytics.logEvent(ActivityAnalytics.DETAILS_SEND_CONFIRMING)
                                R.string.activity_details_label_confirming
                            }
                            isFeeTransaction || transactionType == TransactionSummary.TransactionType.SWAP ||
                                transactionType == TransactionSummary.TransactionType.SELL -> {
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
                        }
                    )
                    showPendingPill()
                }
                confirmations >= totalConfirmations -> {
                    showCompletePill()
                    logAnalyticsForComplete(transactionType, isFeeTransaction)
                }
                else -> {
                    showFailedPill()
                }
            }
        }
    }

    private fun showConfirmationUi(
        confirmations: Int,
        totalConfirmations: Int
    ) {
        if (confirmations != totalConfirmations) {
            binding.apply {
                confirmationLabel.text =
                    getString(
                        R.string.activity_details_label_confirmations, confirmations,
                        totalConfirmations
                    )
                confirmationProgress.setProgress(
                    (confirmations / totalConfirmations.toFloat()) * 100
                )
                confirmationLabel.visible()
                confirmationProgress.visible()
            }
        }
    }

    private fun showPendingPill() {
        binding.status.apply {
            setBackgroundResource(R.drawable.bkgd_status_unconfirmed)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_800))
        }
    }

    private fun showCompletePill() {
        binding.status.apply {
            text = getString(R.string.activity_details_label_complete)
            setBackgroundResource(R.drawable.bkgd_green_100_rounded)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.green_600))
        }
    }

    private fun showFailedPill() {
        binding.status.apply {
            text = getString(R.string.activity_details_label_failed)
            setBackgroundResource(R.drawable.bkgd_red_100_rounded)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.red_600))
        }
    }

    private fun onDescriptionItemClicked(description: String) {
        model.process(
            UpdateDescriptionIntent(arguments.txId, arguments.cryptoCurrency, description)
        )
    }

    private fun onActionItemClicked() {
        val explorerUri = assetResources.makeBlockExplorerUrl(arguments.cryptoCurrency, arguments.txId)
        logAnalyticsForExplorer()
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(explorerUri)
            startActivity(this)
        }
    }

    private fun mapToAction(transactionType: TransactionSummary.TransactionType?): String =
        when (transactionType) {
            TransactionSummary.TransactionType.TRANSFERRED -> getString(
                R.string.activity_details_title_transferred
            )
            TransactionSummary.TransactionType.RECEIVED -> getString(
                R.string.activity_details_title_received
            )
            TransactionSummary.TransactionType.SENT -> getString(R.string.activity_details_title_sent)
            TransactionSummary.TransactionType.BUY -> getString(R.string.activity_details_title_buy)
            TransactionSummary.TransactionType.SELL -> getString(
                R.string.activity_details_title_sell_1,
                arguments.cryptoCurrency.displayTicker
            )
            TransactionSummary.TransactionType.SWAP -> getString(R.string.activity_details_title_swap)
            TransactionSummary.TransactionType.DEPOSIT -> getString(
                R.string.activity_details_title_deposit
            )
            TransactionSummary.TransactionType.WITHDRAW -> getString(
                R.string.activity_details_title_withdraw
            )
            TransactionSummary.TransactionType.INTEREST_EARNED -> getString(
                R.string.activity_details_title_interest_earned
            )
            TransactionSummary.TransactionType.RECURRING_BUY -> getString(
                R.string.activity_details_title_recurring_buy
            )
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
        activityType: CryptoActivityType
    ) {
        model.process(LoadActivityDetailsIntent(cryptoCurrency, txHash, activityType))
    }

    override fun onDestroy() {
        model.destroy()
        super.onDestroy()
    }

    companion object {
        private const val ARG_CRYPTO_CURRENCY = "crypto_currency"
        private const val ARG_ACTIVITY_TYPE = "activity_type"
        private const val ARG_TRANSACTION_HASH = "tx_hash"

        fun newInstance(
            cryptoCurrency: CryptoCurrency,
            txHash: String,
            activityType: CryptoActivityType
        ): CryptoActivityDetailsBottomSheet {
            return CryptoActivityDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CRYPTO_CURRENCY, cryptoCurrency)
                    putString(ARG_TRANSACTION_HASH, txHash)
                    putSerializable(ARG_ACTIVITY_TYPE, activityType)
                }
            }
        }
    }
}