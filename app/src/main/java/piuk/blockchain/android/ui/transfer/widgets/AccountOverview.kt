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
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.NullAccount
import piuk.blockchain.android.util.setCoinIcon

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

    var account: CryptoSingleAccount = NullAccount
        set(value) {
            field = value
            updateView(value)
        }

    private fun updateView(account: CryptoSingleAccount) {
        disposable.clear()

        coin_icon.setCoinIcon(account.asset)
        label.text = account.label
        value.text = ""

        disposable += account.balance
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    value.text = it.toStringWithSymbol()
                }
            )
    }
}
