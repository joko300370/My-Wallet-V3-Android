package piuk.blockchain.android.coincore.impl

import com.blockchain.extensions.exhaustive
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.SingleAccount

fun filterTokenAccounts(
    asset: CryptoCurrency,
    labels: DefaultLabels,
    accountList: List<SingleAccount>,
    assetFilter: AssetFilter
): AccountGroup =
        when (assetFilter) {
            AssetFilter.All ->
                buildAssetMasterGroup(asset, labels, accountList)
            AssetFilter.NonCustodial ->
                buildNonCustodialGroup(asset, labels, accountList)
            AssetFilter.Custodial ->
                buildCustodialGroup(asset, labels, accountList)
            AssetFilter.Interest ->
                buildInterestGroup(asset, labels, accountList)
        }.exhaustive

private fun buildInterestGroup(
    asset: CryptoCurrency,
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup =
    CryptoAccountCustodialGroup(
        labels.getDefaultInterestWalletLabel(asset),
        accountList.filterIsInstance<CryptoInterestAccount>()
    )

private fun buildCustodialGroup(
    asset: CryptoCurrency,
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup =
    CryptoAccountCustodialGroup(
        labels.getDefaultCustodialWalletLabel(asset),
        accountList.filterIsInstance<CustodialTradingAccount>()
    )

private fun buildNonCustodialGroup(
    asset: CryptoCurrency,
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup =
    CryptoAccountNonCustodialGroup(
        asset,
        labels.getDefaultCustodialWalletLabel(asset),
        accountList.filterIsInstance<CryptoNonCustodialAccount>()
    )

private fun buildAssetMasterGroup(
    asset: CryptoCurrency,
    labels: DefaultLabels,
    accountList: List<SingleAccount>
): AccountGroup =
    CryptoAccountNonCustodialGroup(
        asset,
        labels.getAssetMasterWalletLabel(asset),
        accountList
    )