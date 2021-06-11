package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.koin.scopedInject
import org.koin.core.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AccountIcon
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.databinding.ViewAssetWithAccountIconBinding
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.setImageDrawable
import piuk.blockchain.android.util.visible

class AssetWithAccountIcon @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    private val assetResources: AssetResources by scopedInject()

    private val binding: ViewAssetWithAccountIconBinding by lazy {
        ViewAssetWithAccountIconBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun updateIcon(account: CryptoAccount) {
        val accountIcon = AccountIcon(account, assetResources)

        binding.assetIcon.setImageDrawable(accountIcon.icon)
        accountIcon.indicator?.let {
            binding.accountIcon.apply {
                visible()
                setAssetIconColours(
                    tintColor = R.color.white,
                    filterColor = assetResources.assetFilter(account.asset)
                )
                setImageResource(it)
            }
        } ?: kotlin.run { binding.accountIcon.gone() }
    }
}