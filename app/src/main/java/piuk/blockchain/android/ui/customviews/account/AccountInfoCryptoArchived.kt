package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import org.koin.core.KoinComponent
import org.koin.core.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.databinding.ViewAccountCryptoOverviewArchivedBinding
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.setImageDrawable
import piuk.blockchain.android.util.visible

class AccountInfoCryptoArchived @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    private val binding: ViewAccountCryptoOverviewArchivedBinding = ViewAccountCryptoOverviewArchivedBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val assetResources: AssetResources by inject()

    fun updateAccount(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit
    ) = updateView(account, onAccountClicked)

    private fun updateView(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit
    ) {
        val crypto = account.asset
        with(binding) {
            walletName.text = account.label
            icon.setImageDrawable(assetResources.drawableResFilled(crypto))
            icon.visible()

            if (account is TradingAccount) {
                assetAccountIcon.setImageResource(R.drawable.ic_custodial_account_indicator)
                assetAccountIcon.visible()
            } else {
                assetAccountIcon.gone()
            }
            container.alpha = .6f
            setOnClickListener { onAccountClicked(account) }
        }
    }
}
