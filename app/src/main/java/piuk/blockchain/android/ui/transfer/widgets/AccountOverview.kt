package piuk.blockchain.android.ui.transfer.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import org.koin.core.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.NullAccount

class AccountOverview @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
): ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_account_overview, this, true)
    }

    var account: CryptoSingleAccount = NullAccount

//    override fun onFinishInflate() {
//        super.onFinishInflate()
//        setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE)
//        initListener()
//    }
}

