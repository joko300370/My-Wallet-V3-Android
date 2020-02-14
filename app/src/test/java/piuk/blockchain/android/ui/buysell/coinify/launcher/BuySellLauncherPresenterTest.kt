package piuk.blockchain.android.ui.buysell.coinify.launcher

import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherPresenter
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherView
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.services.ExchangeService

class BuySellLauncherPresenterTest : RxTest() {

    private lateinit var subject: BuySellLauncherPresenter

    private val view: BuySellLauncherView = mock()
    private var kycStatusHelper: KycStatusHelper = mock()
    private val exchangeService: ExchangeService = mock()

    @Before
    fun setup() {
        subject = BuySellLauncherPresenter(kycStatusHelper, exchangeService)
        subject.initView(view)
    }

    @Test
    fun `onViewReady user has completed KYC and is Coinify User`() {
        // Assemble
        whenever(kycStatusHelper.getKyc2TierStatus()).thenReturn(Single.just(Kyc2TierState.Tier2Approved))
        whenever(exchangeService.getCoinifyData()).thenReturn(Maybe.just(CoinifyData(123, "asdf")))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayProgressDialog()
        verify(view).onStartCoinifyOverview()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `onViewReady user has completed KYC and is not Coinify User`() {
        // Assemble
        whenever(kycStatusHelper.getKyc2TierStatus()).thenReturn(Single.just(Kyc2TierState.Tier2Approved))
        whenever(exchangeService.getCoinifyData()).thenReturn(Maybe.just(CoinifyData(0, "asdf")))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayProgressDialog()
        verify(view).onStartCoinifyOptIn()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `onViewReady user has not completed KYC`() {
        // Assemble
        whenever(kycStatusHelper.getKyc2TierStatus()).thenReturn(Single.just(Kyc2TierState.Locked))
        whenever(exchangeService.getCoinifyData()).thenReturn(Maybe.just(CoinifyData(123, "asdf")))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayProgressDialog()
        verify(view).onStartCoinifySignUp()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `onViewReady has coinify user KYC not approved`() {
        // Assemble
        whenever(kycStatusHelper.getKyc2TierStatus()).thenReturn(Single.just(Kyc2TierState.Tier2InPending))
        whenever(exchangeService.getCoinifyData()).thenReturn(Maybe.just(CoinifyData(123, "asdf")))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayProgressDialog()
        verify(view).showPendingVerificationView()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `onViewReady network exception`() {
        // Arrange
        whenever(kycStatusHelper.getKyc2TierStatus()).thenReturn(Single.error { Throwable() })
        whenever(exchangeService.getCoinifyData()).thenReturn(Maybe.just(CoinifyData(123, "asdf")))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showErrorToast(any())
        verify(view).finishPage()
    }
}