package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.models.nabu.KycTierState
import com.blockchain.swap.nabu.service.TierService
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.tiers

class SimpleBuyFlowNavigatorTest {

    private val simpleBuyModel: SimpleBuyModel = mock()
    private val tierService: TierService = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()

    private lateinit var subject: SimpleBuyFlowNavigator

    @Before
    fun setUp() {
        subject = SimpleBuyFlowNavigator(
            simpleBuyModel, tierService, currencyPrefs, simpleBuyPrefs, custodialWalletManager
        )
    }

    @Test
    fun `if currency is not supported, state should be cleared`() {
        mockCurrencyIsSupported(false)

        subject.navigateTo(startedFromKycResume = false, startedFromDashboard = false).test()
        verify(simpleBuyPrefs).clearState()
    }

    @Test
    fun `if currency is  supported, state should not be cleared`() {
        mockCurrencyIsSupported(true)

        subject.navigateTo(startedFromKycResume = false, startedFromDashboard = false).test()
        verify(simpleBuyPrefs, never()).clearState()
    }

    @Test
    fun `if currency is not  supported  and startedFromDashboard then screen should be currency selector`() {
        mockCurrencyIsSupported(false)
        whenever(simpleBuyModel.state).thenReturn(Observable.just(SimpleBuyState()))

        val test =
            subject.navigateTo(startedFromKycResume = false, startedFromDashboard = true).test()
        test.assertValueAt(0, FlowScreen.CURRENCY_SELECTOR)
    }

    @Test
    fun `if currency is not  supported  and not startedFromDashboard then screen should be intro`() {
        mockCurrencyIsSupported(true)
        whenever(simpleBuyModel.state).thenReturn(Observable.just(SimpleBuyState()))

        val test =
            subject.navigateTo(startedFromKycResume = false, startedFromDashboard = false).test()
        test.assertValueAt(0, FlowScreen.INTRO)
    }

    @Test
    fun `if currency is  supported and state is clear and startedFromDashboard then screen should be enter amount`() {
        mockCurrencyIsSupported(true)
        whenever(simpleBuyModel.state).thenReturn(Observable.just(SimpleBuyState()))

        val test =
            subject.navigateTo(startedFromKycResume = false, startedFromDashboard = true).test()
        test.assertValueAt(0, FlowScreen.ENTER_AMOUNT)
    }

    // KYC tests
    @Test
    fun `if  current is screen is KYC and tier 2 approved then screen should be checkout`() {
        mockCurrencyIsSupported(true)
        whenever(simpleBuyModel.state)
            .thenReturn(Observable.just(SimpleBuyState().copy(currentScreen = FlowScreen.KYC)))
        whenever(tierService.tiers()).thenReturn(Single.just(tiers(KycTierState.Verified, KycTierState.Verified)))

        val test =
            subject.navigateTo(startedFromKycResume = false, startedFromDashboard = true).test()
        test.assertValueAt(0, FlowScreen.CHECKOUT)
    }

    @Test
    fun `if  current is screen is KYC and tier 2 is pending then screen should be kyc verification`() {
        mockCurrencyIsSupported(true)
        whenever(simpleBuyModel.state)
            .thenReturn(Observable.just(SimpleBuyState().copy(currentScreen = FlowScreen.KYC)))
        whenever(tierService.tiers()).thenReturn(Single.just(tiers(KycTierState.Verified, KycTierState.Pending)))

        val test =
            subject.navigateTo(startedFromKycResume = false, startedFromDashboard = true).test()
        test.assertValueAt(0, FlowScreen.KYC_VERIFICATION)
    }

    @Test
    fun `if  current is screen is KYC and tier 2 is none then screen should be kyc`() {
        mockCurrencyIsSupported(true)
        whenever(simpleBuyModel.state)
            .thenReturn(Observable.just(SimpleBuyState().copy(currentScreen = FlowScreen.KYC)))
        whenever(tierService.tiers()).thenReturn(Single.just(tiers(KycTierState.Verified, KycTierState.None)))

        val test =
            subject.navigateTo(startedFromKycResume = false, startedFromDashboard = true).test()
        test.assertValueAt(0, FlowScreen.KYC)
    }

    private fun mockCurrencyIsSupported(supported: Boolean) {
        whenever(custodialWalletManager
            .isCurrencySupportedForSimpleBuy("USD")).thenReturn(Single.just(supported))
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn(("USD"))
    }
}