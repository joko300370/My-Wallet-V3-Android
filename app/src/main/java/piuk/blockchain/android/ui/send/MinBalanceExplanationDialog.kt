package piuk.blockchain.android.ui.send

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.blockchain.account.DefaultAccountDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible

class MinBalanceExplanationDialog : DialogFragment() {

    private val compositeDisposable = CompositeDisposable()

    init {
        setStyle(STYLE_NO_FRAME, R.style.FullscreenDialog)
    }

    private val xlmDefaultAccountManager: DefaultAccountDataManager by scopedInject()
    private val prefs: CurrencyPrefs by inject()
    private val exchangeRates: ExchangeRateDataManager by scopedInject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.dialog_min_balance_explainer,
        container,
        false
    ).apply {
        isFocusableInTouchMode = true
        requestFocus()
        dialog?.window?.setWindowAnimations(R.style.DialogNoAnimations)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar_general)
        toolbar.setTitle(R.string.minimum_balance_explanation_title)
        toolbar.setNavigationOnClickListener { dismiss() }

        view.findViewById<Button>(R.id.button_continue).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.minimum_balance_url))))
        }
    }

    override fun onResume() {
        super.onResume()
        val progressBar = view?.findViewById<View>(R.id.progress_bar_funds)!!
        progressBar.visible()
        compositeDisposable += xlmDefaultAccountManager.getBalanceAndMin()
            .map {
                Values(
                    it.minimumBalance,
                    it.balance,
                    CryptoValue.lumensFromStroop(100.toBigInteger()) // Tech debt AND-1663 Repeated Hardcoded fee
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                Values(
                    CryptoValue.ZeroXlm,
                    CryptoValue.ZeroXlm,
                    CryptoValue.ZeroXlm
                )
            }
            .doFinally { progressBar.invisible() }
            .subscribeBy {
                view?.run {
                    updateText(R.id.textview_balance, it.balance)
                    updateText(R.id.textview_reserve, it.min)
                    updateText(R.id.textview_fee, it.fee)
                    updateText(R.id.textview_spendable, it.spendable)
                    findViewById<View>(R.id.linearLayout_funds).visible()
                }
            }
    }

    private fun View.updateText(@IdRes textViewId: Int, value: Money) {
        findViewById<TextView>(textViewId).text = formatWithFiat(value)
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private fun formatWithFiat(
        value: Money
    ) = value.toStringWithSymbol() + " " +
            value.toFiat(exchangeRates, prefs.selectedFiatCurrency).toStringWithSymbol()
}

private class Values(val min: CryptoValue, val balance: CryptoValue, val fee: CryptoValue) {
    val spendable: Money = balance - min - fee
}
