package piuk.blockchain.android.ui.customviews

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.ui.urllinks.URL_SUPPORT_BALANCE_LOCKED
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.view_transaction_progress.view.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber

class TransactionProgressView(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs), KoinComponent {

    private val stringUtils: StringUtils by inject()
    private val compositeDisposable = CompositeDisposable()
    private val walletManager: CustodialWalletManager by scopedInject()
    private val currencyPrefs: CurrencyPrefs by scopedInject()

    init {
        inflate(context, R.layout.view_transaction_progress, this)
    }

    fun setAssetIcon(@DrawableRes assetIcon: Int) {
        tx_icon.setImageResource(assetIcon)
    }

    fun onCtaClick(fn: () -> Unit) {
        tx_ok_btn.setOnClickListener {
            fn()
        }
    }

    fun showTxInProgress(title: String, subtitle: String) {
        progress.visible()
        tx_state_indicator.gone()
        tx_ok_btn.gone()
        setText(title, subtitle)
    }

    fun showTxPending(title: String, subtitle: String) {
        progress.gone()
        tx_state_indicator.visible()
        tx_ok_btn.visible()
        tx_state_indicator.setImageResource(R.drawable.ic_pending_clock)
        setText(title, subtitle)
    }

    fun showTxSuccess(
        title: String,
        subtitle: String,
        showLockedFundsInfo: Boolean = false,
        activityContext: Activity? = null
    ) {
        tx_state_indicator.setImageResource(R.drawable.ic_check_circle)
        tx_state_indicator.visible()
        showEndStateUi()
        setText(title, subtitle)
        if (showLockedFundsInfo) {
            require(activityContext != null)

            // if we are showing transaction data, the user must be KYC.GOLD
            compositeDisposable += walletManager.getSupportedFundsFiats(
                currencyPrefs.selectedFiatCurrency, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        showLockedFunds(it, activityContext)
                    },
                    onError = {
                        Timber.e("Error getting supported fiat currencies: $it")
                    }
                )
        }
    }

    private fun showLockedFunds(
        it: List<String>,
        activityContext: Activity
    ) {
        val listOfSupportedCurrencies = it.joinToString("/")
        val intro =
            context.getString(R.string.tx_view_locked_funds, listOfSupportedCurrencies)
        val map = mapOf("learn_more_link" to Uri.parse(URL_SUPPORT_BALANCE_LOCKED))

        val learnLink = stringUtils.getStringWithMappedLinks(
            R.string.common_linked_learn_more,
            map,
            activityContext)

        val sb = SpannableStringBuilder()
        sb.append(intro)
        .append(learnLink)
        .setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue_600)),
            intro.length, intro.length + learnLink.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        tx_locked_funds.run {
            setText(sb, TextView.BufferType.SPANNABLE)
            movementMethod = LinkMovementMethod.getInstance()
            visible()
        }
    }

    fun showTxError(title: String, subtitle: String) {
        tx_icon.setImageResource(R.drawable.ic_alert)
        tx_state_indicator.gone()
        showEndStateUi()
        setText(title, subtitle)
    }

    private fun showEndStateUi() {
        progress.gone()
        tx_ok_btn.visible()
    }

    private fun setText(title: String, subtitle: String) {
        tx_title.text = title
        tx_subtitle.text = subtitle
    }
}