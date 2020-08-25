package piuk.blockchain.android.ui.dashboard.transfer

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.notifications.analytics.WithdrawScreenClicked
import com.blockchain.notifications.analytics.WithdrawScreenShown
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.SimpleBuyError
import com.blockchain.ui.urllinks.URL_SUPPORT_BALANCE_LOCKED
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Maybes
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_basic_transfer_to_wallet.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.android.util.setImageDrawable
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.setOnClickListenerDebounced
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber

class BasicTransferToWallet : SlidingModalBottomDialog() {

    interface Host : SlidingModalBottomDialog.Host {
        fun abortTransferFunds()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException("Host fragment is not a ForceBackupForSendSheet.Host")
    }

    private val cryptoCurrency: CryptoCurrency by lazy {
        arguments?.getSerializable(ARG_CRYPTO_CURRENCY) as? CryptoCurrency
            ?: throw IllegalArgumentException("No cryptoCurrency specified")
    }

    private val coincore: Coincore by scopedInject()
    private val stringUtils: StringUtils by scopedInject()

    private val token: CryptoAsset by lazy {
        coincore[cryptoCurrency]
    }

    private val custodialWallet: CustodialWalletManager by scopedInject()
    private val disposables = CompositeDisposable()
    private val uiScheduler = AndroidSchedulers.mainThread()

    override val layoutResource: Int = R.layout.dialog_basic_transfer_to_wallet

    // Hold on to the address and crypto value; we'll need them for the API
    private var valueToSend: CryptoValue? = null
    private var addressToSend: String? = null

    override fun initControls(view: View) {
        with(view) {
            analytics.logEvent(WithdrawScreenShown(cryptoCurrency))

            cta_button.setOnClickListenerDebounced {
                analytics.logEvent(WithdrawScreenClicked(cryptoCurrency))
                onCtaClick()
            }

            complete_title.text = getString(R.string.basic_transfer_complete_title, cryptoCurrency.displayTicker)

            image.setCoinIcon(cryptoCurrency)

            disposables += Maybes.zip(
                token.exchangeRate().toMaybe(),
                token.accountGroup(AssetFilter.Custodial).flatMap { it.balance.toMaybe() }
            ) { fiatPrice, custodialBalance ->
                val custodialFiat = fiatPrice.convert(custodialBalance)
                Pair(custodialBalance, custodialFiat)
            }
            .doOnComplete { throw IllegalStateException("Empty Account Group") }
            .observeOn(uiScheduler)
            .doOnSubscribe { cta_button.isEnabled = false }
            .subscribeBy(
                onSuccess = { (crypto, fiat) ->
                    valueToSend = crypto as CryptoValue
                    amount_crypto.text = crypto.toStringWithSymbol()
                    amount_fiat.text = fiat.toStringWithSymbol()
                    checkCtaEnable()
                },
                onError = {
                    Timber.e(it)
                    dismiss()
                }
            )

            disposables += token.defaultAccount()
                .flatMap { account ->
                    account.receiveAddress
                        .map { address -> Pair(address, account.label) }
                }
                .observeOn(uiScheduler)
                .subscribeBy(
                    onSuccess = { (address, label) ->
                        require(address is CryptoAddress)
                        addressToSend = address.address
                        title.text = getString(R.string.basic_transfer_title, label)
                        checkCtaEnable()
                    },
                    onError = {
                        Timber.e(it)
                        dismiss()
                    }
                )
        }
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }

    private fun onCtaClick() {
        when (currentView) {
            VIEW_CONFIRM -> onDoConfirm()
            VIEW_COMPLETE -> onDoComplete()
            else -> {
            }
        }
    }

    private fun onDoConfirm() {
        val amount = valueToSend
        val address = addressToSend

        requireNotNull(amount)
        requireNotNull(address)

        disposables += custodialWallet.transferFundsToWallet(amount, address)
            .observeOn(uiScheduler)
            .doOnSubscribe { updateTransferInProgress() }
            .subscribeBy(
                onError = { updateTransferError(it) },
                onSuccess = { updateTransferDone() }
            )
    }

    private fun onDoComplete() {
        dismiss()
    }

    private fun updateTransferInProgress() {
        with(dialogView) {
            image.gone()
            progress.visible()

            cta_button.isEnabled = false
        }
        isCancelable = false
    }

    private fun updateTransferDone() {
        analytics.logEvent(SimpleBuyAnalytics.WITHDRAW_WALLET_SCREEN_SUCCESS)
        with(dialogView) {
            image.setImageDrawable(R.drawable.ic_success_check)

            switchView(VIEW_COMPLETE)
        }
        isCancelable = true
    }

    private fun updateTransferError(t: Throwable) {

        analytics.logEvent(SimpleBuyAnalytics.WITHDRAW_WALLET_SCREEN_FAILURE)

        with(dialogView) {
            image.setImageDrawable(R.drawable.vector_pit_request_failure)
            when (t) {
                is SimpleBuyError.WithdrawalInsufficientFunds -> {
                    complete_title.text = getString(R.string.basic_transfer_error_insufficient_funds_title)
                    complete_message.text = getString(R.string.basic_transfer_error_insufficient_funds)
                }
                is SimpleBuyError.WithdrawalAlreadyPending -> {
                    complete_title.text = getString(R.string.basic_transfer_error_in_progress_title)
                    complete_message.text = getString(R.string.basic_transfer_error_in_progress_body)
                }
                is SimpleBuyError.WithdrawalBalanceLocked -> {
                    complete_title.text = getString(R.string.basic_transfer_error_locked_funds_title)

                    val msg = stringUtils.getStringWithMappedLinks(
                        R.string.basic_transfer_error_locked_funds,
                        mapOf("balance_lock" to Uri.parse(URL_SUPPORT_BALANCE_LOCKED)),
                        requireActivity()
                    )
                    complete_message.text = msg
                }
                else -> {
                    complete_title.text = getString(R.string.basic_transfer_error_title)
                    complete_message.text = getString(R.string.basic_transfer_error_body)
                }
            }

            switchView(VIEW_COMPLETE)
        }
        isCancelable = true
    }

    private fun checkCtaEnable() {
        val v = valueToSend
        val a = addressToSend

        if (v != null && a != null) {
            dialogView.cta_button.isEnabled = true
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        host.abortTransferFunds()
        disposables.clear()
    }

    private val currentView: Int
        get() = dialogView.switcher.displayedChild

    private fun switchView(@Suppress("SameParameterValue") displayView: Int) {
        with(dialogView) {
            if (currentView != displayView) {
                when (displayView) {
                    VIEW_CONFIRM -> {
                    }
                    VIEW_COMPLETE -> {
                        title.invisible()
                        image.visible()
                        progress.gone()

                        cta_button.isEnabled = true
                        cta_button.text = getString(R.string.basic_transfer_complete_cta)
                    }
                }
                switcher.displayedChild = displayView
            }
        }
    }

    companion object {
        private const val ARG_CRYPTO_CURRENCY = "crypto"

        private const val VIEW_CONFIRM = 0
        private const val VIEW_COMPLETE = 1

        fun newInstance(cryptoCurrency: CryptoCurrency): BasicTransferToWallet {
            return BasicTransferToWallet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CRYPTO_CURRENCY, cryptoCurrency)
                }
            }
        }
    }
}
