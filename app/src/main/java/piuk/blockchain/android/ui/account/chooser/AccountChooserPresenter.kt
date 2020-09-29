package piuk.blockchain.android.ui.account.chooser

import androidx.annotation.StringRes
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcoreui.R
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber

class AccountChooserPresenter(
    private val accountHelper: AccountListing,
    private val stringUtils: StringUtils
) : BasePresenter<AccountChooserView>() {

    override fun onViewReady() {
        when (val accountMode = view.accountMode) {
            is AccountMode.Exchange -> loadExchangeAccounts()
            is AccountMode.CryptoAccountMode -> loadAccounts(
                accountMode.cryptoCurrency,
                accountMode.hdOnly,
                accountMode.isSend
            )
        }
    }

    private fun loadAccounts(cryptoCurrency: CryptoCurrency, hdOnly: Boolean, isSend: Boolean) {
        if (hdOnly) {
            accountListWithoutHeader(cryptoCurrency).subscribeToUpdateList()
        } else if (cryptoCurrency == CryptoCurrency.BCH) {
            if (isSend) {
                accountListWithHeader(cryptoCurrency, R.string.wallets)
                    .add(importedListWithHeader(cryptoCurrency))
                    .subscribeToUpdateList()
            } else {
                accountListWithHeader(cryptoCurrency, R.string.wallets)
                    .subscribeToUpdateList()
            }
        } else {
            accountListWithHeader(cryptoCurrency, R.string.wallets)
                .add(importedListWithHeader(cryptoCurrency))
                .subscribeToUpdateList()
        }
    }

    private fun loadExchangeAccounts() {
        accountListWithHeader(CryptoCurrency.BTC, R.string.bitcoin)
            .add(accountListWithHeader(CryptoCurrency.ETHER, R.string.ether))
            .add(accountListWithHeader(CryptoCurrency.BCH, R.string.bitcoin_cash))
            .subscribeToUpdateList()
    }

    private fun importedListWithHeader(cryptoCurrency: CryptoCurrency) =
        accountHelper
            .importedList(cryptoCurrency)
            .map {
                if (it.isNotEmpty()) {
                    prefixHeader(R.string.imported_addresses, it)
                } else {
                    it
                }
            }

    private fun accountListWithHeader(
        cryptoCurrency: CryptoCurrency,
        @StringRes headerResourceId: Int
    ) =
        accountListWithoutHeader(cryptoCurrency)
            .map { prefixHeader(headerResourceId, it) }

    private fun accountListWithoutHeader(cryptoCurrency: CryptoCurrency) =
        accountHelper
            .accountList(cryptoCurrency)

    private fun prefixHeader(
        @StringRes stringResourceId: Int,
        items: List<AccountChooserItem>
    ) = listOf(header(stringResourceId)) + items

    private fun header(@StringRes stringResourceId: Int) =
        AccountChooserItem.Header(stringUtils.getString(stringResourceId))

    private fun Single<List<AccountChooserItem>>.subscribeToUpdateList() =
        addToCompositeDisposable(this@AccountChooserPresenter)
            .subscribe(
                {
                    view.updateUi(
                        it.filter { item ->
                            item !is AccountChooserItem.LegacyAddress
                        })
                },
                { Timber.e(it) }
            )
}

private fun <T> Single<List<T>>.add(other: Single<List<T>>) =
    Single.zip(this, other,
        BiFunction<List<T>, List<T>, List<T>> { l1, l2 -> l1 + l2 })
