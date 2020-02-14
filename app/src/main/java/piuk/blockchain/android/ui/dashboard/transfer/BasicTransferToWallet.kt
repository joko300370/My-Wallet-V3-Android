package piuk.blockchain.android.ui.dashboard.transfer

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.SimpleBuyError
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.formatWithUnit
import info.blockchain.balance.toFiat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_basic_transfer_to_wallet.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
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

    private val assetSelect: AssetTokenLookup by inject()

    private val token: AssetTokens by lazy {
        assetSelect[cryptoCurrency]
    }

    private val custodialWallet: CustodialWalletManager by inject()
    private val disposables = CompositeDisposable()
    private val uiScheduler = AndroidSchedulers.mainThread()

    override val layoutResource: Int = R.layout.dialog_basic_transfer_to_wallet

    // Hold on to the address and crypto value; we'll need them for the API
    private var valueToSend: CryptoValue? = null
    private var addressToSend: AccountReference? = null

    override fun initControls(view: View) {
        with(view) {
            cta_button.setOnClickListenerDebounced { onCtaClick() }

            complete_title.text =
                getString(R.string.basic_transfer_complete_title, cryptoCurrency.symbol)

            image.setCoinIcon(cryptoCurrency)

            disposables += Singles.zip(
                token.exchangeRate(),
                token.totalBalance(AssetFilter.Custodial)
            ) { fiatPrice, custodialBalance ->
                val custodialFiat = custodialBalance.toFiat(fiatPrice)
                Pair(custodialBalance, custodialFiat)
            }
                .observeOn(uiScheduler)
                .subscribeBy(
                    onSuccess = { (crypto, fiat) ->
                        valueToSend = crypto
                        amount_crypto.text = crypto.formatWithUnit()
                        amount_fiat.text = fiat.toStringWithSymbol()
                        checkCtaEnable()
                    },
                    onError = {
                        Timber.e(it)
                        dismiss()
                    }
                )

            disposables += token.defaultAccount()
                .observeOn(uiScheduler)
                .subscribeBy(
                    onSuccess = { account ->
                        addressToSend = account
                        title.text = getString(R.string.basic_transfer_title, account.label)
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

        disposables += custodialWallet.transferFundsToWallet(amount, address.receiveAddress)
            .observeOn(uiScheduler)
            .doOnSubscribe { updateTransferInProgress() }
            .subscribeBy(
                onError = { updateTransferError(it) },
                onComplete = { updateTransferDone() }
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
        with(dialogView) {
            image.setImageDrawable(R.drawable.ic_success_check)

            switchView(VIEW_COMPLETE)
        }
        isCancelable = true
    }

    private fun updateTransferError(t: Throwable) {
        with(dialogView) {
            image.setImageDrawable(R.drawable.vector_pit_request_failure)

            if (t is SimpleBuyError.WithdrawlInsufficientFunds || t is SimpleBuyError.WithdrawlAlreadyPending) {
                complete_title.text = getString(R.string.basic_transfer_error_in_progress_title)
                complete_message.text = getString(R.string.basic_transfer_error_in_progress_body)
            } else {
                complete_title.text = getString(R.string.basic_transfer_error_title)
                complete_message.text = getString(R.string.basic_transfer_error_body)
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

            if (v.currency != a.cryptoCurrency) {
                throw IllegalStateException("Crypto currency mismatch. Aborting!")
            }
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
