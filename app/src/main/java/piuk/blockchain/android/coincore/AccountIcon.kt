package piuk.blockchain.android.coincore

import androidx.annotation.DrawableRes
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.fiat.FiatAccountGroup
import piuk.blockchain.android.coincore.impl.AllWalletsAccount
import piuk.blockchain.android.coincore.impl.CryptoAccountCustodialGroup
import piuk.blockchain.android.coincore.impl.CryptoAccountNonCustodialGroup
import piuk.blockchain.android.coincore.impl.CryptoExchangeAccount
import piuk.blockchain.android.coincore.impl.CryptoInterestAccount
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount

class AccountIcon(private val account: BlockchainAccount, private val assetResources: AssetResources) {
    val icon: Int
        @DrawableRes get() = when (account) {
            is CryptoAccount -> assetResources.drawableResFilled(account.asset)
            is CryptoInterestAccount -> assetResources.drawableResFilled(account.asset)
            is AccountGroup -> accountGroupIcon(account)
            is FiatAccount -> assetResources.fiatCurrencyIcon(account.fiatCurrency)
            else -> throw IllegalStateException("$account is not supported")
        }

    val indicator: Int?
        @DrawableRes get() = when (account) {
            is CryptoNonCustodialAccount -> R.drawable.ic_non_custodial_account_indicator
            is InterestAccount -> R.drawable.ic_interest_account_indicator
            is TradingAccount -> R.drawable.ic_custodial_account_indicator
            is CryptoExchangeAccount -> R.drawable.ic_exchange_indicator
            else -> null
        }

    private fun accountGroupIcon(account: AccountGroup): Int {
        return when (account) {
            is AllWalletsAccount -> R.drawable.ic_all_wallets_white
            is CryptoAccountCustodialGroup -> assetResources.drawableResFilled(
                (account.accounts[0] as CryptoAccount).asset
            )
            is CryptoAccountNonCustodialGroup -> assetResources.drawableResFilled(account.asset)
            is FiatAccountGroup -> (account.accounts.getOrNull(0) as? FiatAccount)?.let {
                assetResources.fiatCurrencyIcon(it.fiatCurrency)
            } ?: DEFAULT_FIAT_ICON
            else -> throw IllegalArgumentException("$account is not a valid group")
        }
    }
}

private const val DEFAULT_FIAT_ICON = R.drawable.ic_funds_usd