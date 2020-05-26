package piuk.blockchain.android.util

import android.content.Context
import android.content.res.Resources
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R

@ColorRes
fun CryptoCurrency.colorRes(): Int =
    when (this) {
        CryptoCurrency.BTC -> R.color.color_bitcoin_logo
        CryptoCurrency.ETHER -> R.color.color_ether_logo
        CryptoCurrency.BCH -> R.color.color_bitcoin_cash_logo
        CryptoCurrency.XLM -> R.color.color_stellar_logo
        CryptoCurrency.PAX -> R.color.color_pax_logo
        CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
    }

@ColorInt
fun CryptoCurrency.getColor(context: Context) = ContextCompat.getColor(context, colorRes())

@DrawableRes
fun CryptoCurrency.drawableResFilled(): Int =
    when (this) {
        CryptoCurrency.BTC -> R.drawable.vector_bitcoin_colored
        CryptoCurrency.ETHER -> R.drawable.vector_eth_colored
        CryptoCurrency.BCH -> R.drawable.vector_bitcoin_cash_colored
        CryptoCurrency.XLM -> R.drawable.vector_xlm_colored
        CryptoCurrency.PAX -> R.drawable.vector_pax_colored
        CryptoCurrency.STX -> R.drawable.ic_logo_stx
    }

@DrawableRes
fun CryptoCurrency.coinIconWhite(): Int =
    when (this) {
        CryptoCurrency.BTC -> R.drawable.vector_bitcoin_white
        CryptoCurrency.ETHER -> R.drawable.vector_eth_white
        CryptoCurrency.BCH -> R.drawable.vector_bitcoin_cash_white
        CryptoCurrency.XLM -> R.drawable.vector_xlm_white
        CryptoCurrency.PAX -> R.drawable.vector_pax_white
        CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
    }

@DrawableRes
fun CryptoCurrency.maskedAsset(): Int =
    when (this) {
        CryptoCurrency.BTC -> R.drawable.ic_btc_circled_mask
        CryptoCurrency.XLM -> R.drawable.ic_xlm_circled_mask
        CryptoCurrency.ETHER -> R.drawable.ic_eth_circled_mask
        CryptoCurrency.PAX -> R.drawable.ic_usdd_circled_mask
        CryptoCurrency.BCH -> R.drawable.ic_bch_circled_mask
        CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
    }

fun ImageView.setImageDrawable(@DrawableRes res: Int) {
    setImageDrawable(AppCompatResources.getDrawable(context, res))
}

fun ImageView.setCoinIcon(currency: CryptoCurrency) {
    setImageDrawable(currency.drawableResFilled())
}

@DrawableRes
fun CryptoCurrency.errorIcon(): Int =
    when (this) {
        CryptoCurrency.BTC -> R.drawable.vector_btc_error
        CryptoCurrency.BCH -> R.drawable.vector_bch_error
        CryptoCurrency.ETHER -> R.drawable.vector_eth_error
        CryptoCurrency.XLM -> R.drawable.vector_xlm_error
        CryptoCurrency.PAX -> R.drawable.vector_pax_error
        CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
    }

@StringRes
fun CryptoCurrency.assetName() =
    when (this) {
        CryptoCurrency.BTC -> R.string.bitcoin
        CryptoCurrency.ETHER -> R.string.ethereum
        CryptoCurrency.BCH -> R.string.bitcoin_cash
        CryptoCurrency.XLM -> R.string.lumens
        CryptoCurrency.PAX -> R.string.usd_pax_1
        CryptoCurrency.STX -> R.string.stacks_1
    }

internal class ResourceDefaultLabels(
    private val resources: Resources
) : DefaultLabels {

    override fun getDefaultNonCustodialWalletLabel(cryptoCurrency: CryptoCurrency): String =
        resources.getString(
            when (cryptoCurrency) {
                CryptoCurrency.BTC -> R.string.btc_default_wallet_name
                CryptoCurrency.ETHER -> R.string.eth_default_account_label
                CryptoCurrency.BCH -> R.string.bch_default_account_label
                CryptoCurrency.XLM -> R.string.xlm_default_account_label
                CryptoCurrency.PAX -> R.string.pax_default_account_label_1
                CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            }
        )

    override fun getDefaultCustodialWalletLabel(cryptoCurrency: CryptoCurrency): String {
        val asset = resources.getString(cryptoCurrency.assetName())
        return resources.getString(R.string.custodial_wallet_default_label, asset)
    }

    override fun getAssetMasterWalletLabel(cryptoCurrency: CryptoCurrency): String =
        resources.getString(cryptoCurrency.assetName())

    override fun getAllWalletLabel(): String =
        resources.getString(R.string.default_label_all_wallets)

    override fun getDefaultInterestWalletLabel(cryptoCurrency: CryptoCurrency): String =
        resources.getString(R.string.default_label_interest_wallet)
}