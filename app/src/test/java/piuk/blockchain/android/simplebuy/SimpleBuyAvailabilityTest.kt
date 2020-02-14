package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class SimpleBuyAvailabilityTest {

    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val simpleBuyFlag: FeatureFlag = mock()

    private lateinit var simpleBuyAvailability: SimpleBuyAvailability

    @Before
    fun setup() {
        simpleBuyAvailability = SimpleBuyAvailability(
            simpleBuyPrefs, custodialWalletManager, currencyPrefs, simpleBuyFlag
        )

        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("USD")
        whenever(simpleBuyFlag.enabled).thenReturn(Single.just(true))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(true)

        whenever(custodialWalletManager.isEligibleForSimpleBuy())
            .thenReturn(Single.just(true))
        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy("USD"))
            .thenReturn(Single.just(true))
    }

    @Test
    fun `should work in the happy scenario`() {
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, true)
    }

    @Test
    fun `should not be available when currency is not supported`() {
        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy("USD"))
            .thenReturn(Single.just(false))
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not be available when feature flag is off`() {
        whenever(simpleBuyFlag.enabled).thenReturn(Single.just(false))
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not be available when is not eligible and no local state exists`() {
        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy("USD"))
            .thenReturn(Single.just(false))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(false)

        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when is not eligible and no local state`() {
        whenever(custodialWalletManager.isEligibleForSimpleBuy()).thenReturn(Single.just(false))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(false)
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when eligibility fails and no local state exists`() {
        whenever(custodialWalletManager.isEligibleForSimpleBuy()).thenReturn(Single.error(Throwable()))
        whenever(simpleBuyPrefs.flowStartedAtLeastOnce()).thenReturn(false)

        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }

    @Test
    fun `should not  be available when currency call fails`() {
        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy("USD"))
            .thenReturn(Single.error(Throwable()))
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }
}