package piuk.blockchain.android.ui.transfer.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.view_account_overview.view.*
import org.koin.core.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullAccount
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber

class AccountOverview @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    private val disposable = CompositeDisposable()

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_account_overview, this, true)
    }

    var account: CryptoAccount = NullAccount
        set(value) {
            field = value
            updateView(value)
        }

    private fun updateView(account: CryptoAccount) {
        disposable.clear()

        if (account.cryptoCurrencies.size == 1) {
            icon.setCoinIcon(account.cryptoCurrencies.first())
            icon.visible()
        } else {
            // Need a specialised asset, but this is currently only the case for AllWallets, so can come back to
            // this later: TODO
            icon.gone()
        }

        label.text = account.label
        value.text = ""

        disposable += account.balance
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    value.text = it.toStringWithSymbol()
                },
                onError = {
                    Timber.e("Cannot get balance for ${account.label}")
                }
            )
    }
}
