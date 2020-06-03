package piuk.blockchain.android.ui.swap.homebrew.exchange.confirmation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.annotations.CommonCode
import piuk.blockchain.android.util.colorRes
import com.blockchain.koin.scopedInject
import com.blockchain.koin.scopedInjectActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.campaign.CampaignType
import com.blockchain.swap.common.exchange.mvi.ExchangeViewState
import com.blockchain.swap.nabu.service.Quote
import piuk.blockchain.android.ui.swap.homebrew.exchange.ExchangeModel
import piuk.blockchain.android.ui.swap.homebrew.exchange.ExchangeViewModelProvider
import piuk.blockchain.android.ui.swap.homebrew.exchange.host.HomebrewHostActivityListener
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.SwapAnalyticsEvents
import com.blockchain.ui.extensions.sampleThrottledClicks
import com.blockchain.ui.password.SecondPasswordHandler
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_homebrew_trade_confirmation.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.swap.homebrew.exchange.detail.HomebrewTradeDetailActivity
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.SwapErrorDialogContent
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.Trade
import piuk.blockchain.android.ui.swap.ui.SwapErrorBottomDialog
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment
import com.blockchain.ui.dialog.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import timber.log.Timber

class ExchangeConfirmationFragment :
    BaseMvpFragment<ExchangeConfirmationView, ExchangeConfirmationPresenter>(),
    ExchangeConfirmationView {

    private val presenter: ExchangeConfirmationPresenter by scopedInject()
    private val analytics: Analytics by inject()
    private val secondPasswordHandler: SecondPasswordHandler by scopedInjectActivity()
    private val activityListener: HomebrewHostActivityListener by ParentActivityDelegate(this)

    private var progressDialog: MaterialProgressDialog? = null

    override val exchangeViewState: Observable<ExchangeViewState> by unsafeLazy {
        exchangeModel.exchangeViewStates
            .observeOn(AndroidSchedulers.mainThread())
            .sampleThrottledClicks(confirm_button).doOnNext {
                analytics.logEvent(SwapAnalyticsEvents.SwapSummaryConfirmClick)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_homebrew_trade_confirmation)

    private lateinit var exchangeModel: ExchangeModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val provider = (context as? ExchangeViewModelProvider)
            ?: throw Exception("Host activity must support ExchangeViewModelProvider")
        exchangeModel = provider.exchangeViewModel
        Timber.d("The view model is $exchangeModel")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressDialog = MaterialProgressDialog(requireContext()).apply {
            setMessage(piuk.blockchain.androidcoreui.R.string.please_wait)
            setCancelable(false)
        }

        activityListener.setToolbarTitle(R.string.confirm_swap)
        onViewReady()
    }

    private var compositeDisposable = CompositeDisposable()

    override fun onResume() {
        super.onResume()
        compositeDisposable += exchangeModel
            .exchangeViewStates
            .observeOn(AndroidSchedulers.mainThread())
            .filter { it.latestQuote?.rawQuote != null }
            .map {
                ExchangeConfirmationViewModel(
                    fromAccount = it.fromAccount,
                    toAccount = it.toAccount,
                    sending = it.fromCrypto,
                    receiving = it.toCrypto,
                    value = it.toFiat,
                    quote = it.latestQuote!!
                )
            }
            .doOnNext { presenter.updateFee(it.sending, it.fromAccount) }
            .subscribeBy {
                renderUi(it)
            }
    }

    private fun renderUi(viewModel: ExchangeConfirmationViewModel) {
        with(viewModel) {
            from_amount.setBackgroundResource(sending.currency.colorRes())
            from_amount.text = sending.toStringWithSymbol()

            val receivingCryptoValue = receiving.toStringWithSymbol()
            to_amount.setBackgroundResource(receiving.currency.colorRes())
            if (to_amount.text.isNullOrEmpty().not() && receivingCryptoValue != to_amount.text.toString()) {
                analytics.logEvent(SwapAnalyticsEvents.SwapExchangeReceiveChange)
            }
            to_amount.text = receivingCryptoValue

            receive_amount.text = receivingCryptoValue
            fiat_value.text = value.toStringWithSymbol()
            send_to_wallet.text = viewModel.toAccount.label
        }
    }

    override fun onTradeSubmitted(trade: Trade, firstGoldPaxTrade: Boolean) {
        HomebrewTradeDetailActivity.start(requireContext(), trade, showSuccess = true, isFirstPax = firstGoldPaxTrade)
        requireActivity().finish()
    }

    override fun updateFee(cryptoValue: CryptoValue) {
        fees_value.text = cryptoValue.toStringWithSymbol()
    }

    override fun showSecondPasswordDialog() {
        secondPasswordHandler.validate(object : SecondPasswordHandler.ResultListener {
            override fun onNoSecondPassword() = Unit

            override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                presenter.onSecondPasswordValidated(validatedSecondPassword)
            }
        })
    }

    @UiThread
    @CommonCode("Move to base")
    override fun showProgressDialog() {
        if (progressDialog?.isShowing == true) return
        progressDialog?.show()
    }

    @UiThread
    @CommonCode("Move to base")
    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog = null
    }

    @CommonCode("Move to base")
    override fun displayErrorDialog(@StringRes message: Int) {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.execution_error_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showToast(message: Int, type: String) {
        toast(message, type)
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun createPresenter(): ExchangeConfirmationPresenter = presenter

    override fun getMvpView(): ExchangeConfirmationView = this

    override fun displayErrorBottomDialog(swapErrorDialogContent: SwapErrorDialogContent) {
        val bottomSheetDialog = SwapErrorBottomDialog.newInstance(swapErrorDialogContent.content)
        swapErrorDialogContent.ctaClick?.let {
            bottomSheetDialog.onCtaClick = it
        }
        swapErrorDialogContent.dismissClick?.let {
            bottomSheetDialog.onDismissClick = it
        }
        fragmentManager?.let {
            bottomSheetDialog.show(it, "BottomDialog")
        }
    }

    override fun openTiersCard() {
        context?.let {
            KycNavHostActivity.start(it, CampaignType.Swap)
        }
    }

    override fun openMoreInfoLink(link: String) {
        context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    }

    override fun goBack() {
        fragmentManager?.popBackStack()
    }
}

class ExchangeConfirmationViewModel(
    val fromAccount: AccountReference,
    val toAccount: AccountReference,
    val value: FiatValue,
    val sending: CryptoValue,
    val receiving: CryptoValue,
    val quote: Quote
)
