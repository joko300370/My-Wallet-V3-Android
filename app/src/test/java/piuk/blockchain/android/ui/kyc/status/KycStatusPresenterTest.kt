package piuk.blockchain.android.ui.kyc.status

import com.blockchain.android.testutils.rxInit
import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import com.blockchain.swap.nabu.models.nabu.KycState
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.notifications.NotificationTokenManager
import piuk.blockchain.android.ui.validOfflineToken
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper

class KycStatusPresenterTest {

    private lateinit var subject: KycStatusPresenter
    private val view: KycStatusView = mock()
    private val kycStatusHelper: KycStatusHelper = mock()
    private val nabuToken: NabuToken = mock()
    private val notificationTokenManager: NotificationTokenManager = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycStatusPresenter(
            nabuToken,
            kycStatusHelper,
            notificationTokenManager
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady exception thrown, finish page`() {
        // Arrange
        whenever(kycStatusHelper.getKyc2TierStatus())
            .thenReturn(Single.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).finishPage()
    }

    @Test
    fun `onViewReady user loaded`() {
        // Arrange
        val kycState = KycState.Pending
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(kycStatusHelper.getKyc2TierStatus())
            .thenReturn(Single.just(Kyc2TierState.Tier2InPending))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).renderUi(kycState)
    }

    @Test
    fun `onClickNotifyUser fails, should display toast`() {
        // Arrange
        whenever(notificationTokenManager.enableNotifications())
            .thenReturn(Completable.error { Throwable() })
        // Act
        subject.onClickNotifyUser()
        // Assert
        verify(notificationTokenManager).enableNotifications()
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showToast(any())
    }

    @Test
    fun `onClickNotifyUser successful, should display dialog`() {
        // Arrange
        whenever(notificationTokenManager.enableNotifications())
            .thenReturn(Completable.complete())
        // Act
        subject.onClickNotifyUser()
        // Assert
        verify(notificationTokenManager).enableNotifications()
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showNotificationsEnabledDialog()
    }

    @Test
    fun `onClickContinue should start exchange activity`() {
        // Act
        subject.onClickContinue()
        // Assert
        verify(view).startExchange()
    }
}