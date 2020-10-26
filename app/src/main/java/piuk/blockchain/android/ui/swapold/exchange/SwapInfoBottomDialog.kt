package piuk.blockchain.android.ui.swapold.exchange

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.swap.nabu.StartKyc
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.SwapAnalyticsEvents
import com.blockchain.ui.extensions.throttledClicks
import com.blockchain.ui.urllinks.URL_BLOCKCHAIN_RAISE_SUPPORT_TICKET
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_swap_info.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.swapold.exchange.history.TradeHistoryActivity

class SwapInfoBottomDialog : BottomSheetDialogFragment() {

    private val analytics: Analytics by inject()
    private val startKyc: StartKyc by inject()

    private val clicksDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics.logEvent(AnalyticsEvents.SwapInfoDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)

        return themedInflater.inflate(R.layout.dialog_swap_info, container, false)
    }

    override fun onResume() {
        super.onResume()

        clicksDisposable += btn_history.throttledClicks()
            .subscribeBy(onNext = {
                analytics.logEvent(SwapAnalyticsEvents.SwapViewHistoryButtonClick)
                TradeHistoryActivity.start(requireContext())
                dismiss()
            })
        clicksDisposable += btn_limits.throttledClicks()
            .subscribeBy(onNext = {
                analytics.logEvent(AnalyticsEvents.SwapInfoDialogSwapLimits)
                startKyc.startKycActivity(requireContext())
                dismiss()
            })

        clicksDisposable += btn_support.throttledClicks()
            .subscribeBy(onNext = {
                analytics.logEvent(AnalyticsEvents.SwapInfoDialogSupport)
                context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_BLOCKCHAIN_RAISE_SUPPORT_TICKET)))
                dismiss()
            })
    }

    override fun onPause() {
        clicksDisposable.clear()
        super.onPause()
    }

    companion object {
        fun newInstance() = SwapInfoBottomDialog()
    }
}
