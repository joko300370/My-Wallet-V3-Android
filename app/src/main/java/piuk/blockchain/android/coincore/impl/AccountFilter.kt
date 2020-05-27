package piuk.blockchain.android.coincore.impl

import com.blockchain.extensions.exhaustive
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.android.coincore.CryptoSingleAccount

fun filterTokenAccounts(
    asset: CryptoCurrency,
    labels: DefaultLabels,
    accountList: List<CryptoSingleAccount>,
    assetFilter: AssetFilter
): CryptoAccountGroup =
        when (assetFilter) {
            AssetFilter.Total ->
                buildAssetMasterGroup(asset, labels, accountList)
            AssetFilter.Wallet ->
                buildNonCustodialGroup(asset, labels, accountList)
            AssetFilter.Custodial ->
                buildCustodialGroup(asset, labels, accountList)
            AssetFilter.Interest ->
                buildInterestGroup(asset, labels, accountList)
        }.exhaustive

private fun buildInterestGroup(
    asset: CryptoCurrency,
    labels: DefaultLabels,
    accountList: List<CryptoSingleAccount>
): CryptoAccountGroup =
    CryptoAccountCustodialGroup(
        labels.getDefaultInterestWalletLabel(asset),
        accountList.filterIsInstance<CryptoInterestAccount>()
    )

private fun buildCustodialGroup(
    asset: CryptoCurrency,
    labels: DefaultLabels,
    accountList: List<CryptoSingleAccount>
): CryptoAccountGroup =
    CryptoAccountCustodialGroup(
        labels.getDefaultCustodialWalletLabel(asset),
        accountList.filterIsInstance<CustodialTradingAccount>()
    )

private fun buildNonCustodialGroup(
    asset: CryptoCurrency,
    labels: DefaultLabels,
    accountList: List<CryptoSingleAccount>
): CryptoAccountGroup =
    CryptoAccountCompoundGroup(
        asset,
        labels.getDefaultCustodialWalletLabel(asset),
        accountList.filterIsInstance<CryptoSingleAccountNonCustodialBase>()
    )

private fun buildAssetMasterGroup(
    asset: CryptoCurrency,
    labels: DefaultLabels,
    accountList: List<CryptoSingleAccount>
): CryptoAccountGroup =
    CryptoAccountCompoundGroup(
        asset,
        labels.getAssetMasterWalletLabel(asset),
        accountList
    )