package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_account_crypto_overview_archived.view.asset_account_icon
import kotlinx.android.synthetic.main.view_account_crypto_overview_archived.view.container
import kotlinx.android.synthetic.main.view_account_crypto_overview_archived.view.icon
import kotlinx.android.synthetic.main.view_account_crypto_overview_archived.view.wallet_name
import org.koin.core.KoinComponent
import org.koin.core.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.setImageDrawable
import piuk.blockchain.android.util.visible

class AccountInfoCryptoArchived @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    private val assetResources: AssetResources by inject()

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_account_crypto_overview_archived, this, true)
    }

    fun updateAccount(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit
    ) = updateView(account, onAccountClicked)

    private fun updateView(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit
    ) {
        val crypto = account.asset
        wallet_name.text = account.label
        icon.setImageDrawable(assetResources.drawableResFilled(crypto))
        icon.visible()

        if (account is TradingAccount) {
            asset_account_icon.setImageResource(R.drawable.ic_custodial_account_indicator)
            asset_account_icon.visible()
        } else {
            asset_account_icon.gone()
        }
        container.alpha = .6f
        setOnClickListener { onAccountClicked(account) }
    }
}
