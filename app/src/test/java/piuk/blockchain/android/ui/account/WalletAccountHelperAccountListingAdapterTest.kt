package piuk.blockchain.android.ui.account

import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import com.blockchain.testutils.lumens
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.account.chooser.AccountChooserItem
import piuk.blockchain.android.ui.account.chooser.AccountListing
import piuk.blockchain.android.ui.chooser.WalletAccountHelper
import java.util.Locale

class WalletAccountHelperAccountListingAdapterTest {

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `BTC accounts`() {
        val account = mock<JsonSerializableAccount>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getHdAccounts() } `it returns` listOf(
                ItemAccount(
                    label = "Acc1",
                    balance = 123.bitcoin(),
                    accountObject = account
                )
            )
        }
        givenAccountListing(walletAccountHelper)
            .accountList(CryptoCurrency.BTC)
            .assertSingleAccountSummary {
                label `should equal` "Acc1"
                displayBalance `should equal` "123.0 BTC"
                accountObject `should be` account
            }
    }

    @Test
    fun `BCH accounts`() {
        val account = mock<JsonSerializableAccount>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getHdBchAccounts() } `it returns` listOf(
                ItemAccount(
                    label = "Acc2",
                    balance = 456.bitcoinCash(),
                    accountObject = account
                )
            )
        }
        givenAccountListing(walletAccountHelper)
            .accountList(CryptoCurrency.BCH)
            .assertSingleAccountSummary {
                label `should equal` "Acc2"
                displayBalance `should equal` "456.0 BCH"
                accountObject `should be` account
            }
    }

    @Test
    fun `ETH accounts`() {
        val account = mock<JsonSerializableAccount>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getEthAccount() } `it returns` listOf(
                ItemAccount(
                    label = "Acc3",
                    balance = 99.ether(),
                    accountObject = account
                )
            )
        }
        givenAccountListing(walletAccountHelper)
            .accountList(CryptoCurrency.ETHER)
            .assertSingleAccountSummary {
                label `should equal` "Acc3"
                displayBalance `should equal` "99.0 ETH"
                accountObject `should be` account
            }
    }

    @Test
    fun `XLM accounts`() {
        val account = mock<JsonSerializableAccount>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getXlmAccount() } `it returns` Single.just(
                listOf(
                    ItemAccount(
                        label = "Acc4",
                        balance = 99.lumens(),
                        accountObject = account
                    )
                )
            )
        }
        givenAccountListing(walletAccountHelper)
            .accountList(CryptoCurrency.XLM)
            .assertSingleAccountSummary {
                label `should equal` "Acc4"
                displayBalance `should equal` "99.0 XLM"
                accountObject `should be` account
            }
    }

    @Test
    fun `BTC imported (legacy) addresses`() {
        val account = mock<LegacyAddress>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getLegacyBtcAddresses() } `it returns` listOf(
                ItemAccount(
                    label = "My address",
                    address = "mhPgaJ366MXe7SNGeaCBBsWAhkM2JfB5Cm",
                    balance = 7.bitcoin(),
                    accountObject = account
                )
            )
        }
        givenAccountListing(walletAccountHelper)
            .importedList(CryptoCurrency.BTC)
            .assertSingleLegacyAddress {
                label `should equal` "My address"
                address `should equal` "mhPgaJ366MXe7SNGeaCBBsWAhkM2JfB5Cm"
                displayBalance `should equal` "7.0 BTC"
                accountObject `should be` account
            }
    }

    @Test
    fun `BCH imported (legacy) addresses`() {
        val account = mock<LegacyAddress>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getLegacyBchAddresses() } `it returns` listOf(
                ItemAccount(
                    label = "My address 2",
                    address = "mpE7PuLdFQaKfHsFSFqM9FbTvLczB3j1QV",
                    balance = 8.bitcoinCash(),
                    accountObject = account
                )
            )
        }
        givenAccountListing(walletAccountHelper)
            .importedList(CryptoCurrency.BCH)
            .assertSingleLegacyAddress {
                label `should equal` "My address 2"
                address `should equal` "mpE7PuLdFQaKfHsFSFqM9FbTvLczB3j1QV"
                displayBalance `should equal` "8.0 BCH"
                accountObject `should be` account
            }
    }

    @Test
    fun `BTC imported (legacy) addresses - null address when not a legacy`() {
        val account = mock<JsonSerializableAccount>()
        val walletAccountHelper = mock<WalletAccountHelper> {
            on { getLegacyBtcAddresses() } `it returns` listOf(
                ItemAccount(
                    address = "mwfJF7GdsShHBtLCAhWUjymTwAwtf1E5LE",
                    accountObject = account
                )
            )
        }
        givenAccountListing(walletAccountHelper)
            .importedList(CryptoCurrency.BTC)
            .assertSingleLegacyAddress {
                address `should be` null
            }
    }

    @Test
    fun `ETH imported`() {
        givenAccountListing(mock())
            .importedList(CryptoCurrency.ETHER)
            .test()
            .values().single() `should equal` emptyList()
    }

    @Test
    fun `XLM imported`() {
        givenAccountListing(mock())
            .importedList(CryptoCurrency.XLM)
            .test()
            .values().single() `should equal` emptyList()
    }

    private fun givenAccountListing(walletAccountHelper: WalletAccountHelper): AccountListing =
        WalletAccountHelperAccountListingAdapter(
            walletAccountHelper
        )
}

private fun Single<List<AccountChooserItem>>.assertSingleAccountSummary(
    assertBlock: AccountChooserItem.AccountSummary.() -> Unit
) = assertSingle().single().apply {
    assertBlock(this as? AccountChooserItem.AccountSummary
        ?: throw Exception("Wrong type"))
}

private fun Single<List<AccountChooserItem>>.assertSingleLegacyAddress(
    assertBlock: AccountChooserItem.LegacyAddress.() -> Unit
) = assertSingle().single().apply {
    assertBlock(this as? AccountChooserItem.LegacyAddress
        ?: throw Exception("Wrong type"))
}

fun <T> Single<T>.assertSingle(): T =
    test().values().single()
