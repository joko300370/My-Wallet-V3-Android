package piuk.blockchain.android.ui.account

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Single
import piuk.blockchain.android.ui.account.chooser.AccountChooserItem
import piuk.blockchain.android.ui.account.chooser.AccountListing
import piuk.blockchain.android.ui.chooser.WalletAccountHelper

class WalletAccountHelperAccountListingAdapter(
    private val walletAccountHelper: WalletAccountHelper
) : AccountListing {

    override fun accountList(cryptoCurrency: CryptoCurrency): Single<List<AccountChooserItem>> =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> Single.just(walletAccountHelper.getHdAccounts())
            CryptoCurrency.BCH -> Single.just(walletAccountHelper.getHdBchAccounts())
            CryptoCurrency.ETHER -> Single.just(walletAccountHelper.getEthAccount())
            CryptoCurrency.XLM -> walletAccountHelper.getXlmAccount()
            CryptoCurrency.PAX -> Single.just(walletAccountHelper.getEthAccount())
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> TODO("STUB: ALGO NOT IMPLEMENTED")
            CryptoCurrency.USDT -> TODO("STUB: USDT NOT IMPLEMENTED")
        }.map {
            it.map { account -> mapAccountSummary(account) }
        }

    override fun importedList(cryptoCurrency: CryptoCurrency): Single<List<AccountChooserItem>> =
        Single.just(
            when (cryptoCurrency) {
                CryptoCurrency.BTC -> walletAccountHelper.getLegacyBtcAddresses()
                CryptoCurrency.BCH -> walletAccountHelper.getLegacyBchAddresses()
                CryptoCurrency.ETHER,
                CryptoCurrency.XLM,
                CryptoCurrency.PAX,
                CryptoCurrency.STX,
                CryptoCurrency.ALGO,
                CryptoCurrency.USDT -> emptyList()
            }.map(this::mapLegacyAddress)
        )

    private fun mapAccountSummary(itemAccount: ItemAccount): AccountChooserItem =
        AccountChooserItem.AccountSummary(
            label = itemAccount.label,
            displayBalance = itemAccount.formatDisplayBalance(),
            accountObject = itemAccount.accountObject
        )

    private fun mapLegacyAddress(itemAccount: ItemAccount): AccountChooserItem {
        val legacyAddress = itemAccount.accountObject as? LegacyAddress

        return AccountChooserItem.LegacyAddress(
            label = itemAccount.label,
            address = if (legacyAddress == null) null else itemAccount.address,
            displayBalance = itemAccount.formatDisplayBalance(),
            accountObject = itemAccount.accountObject
        )
    }
}
