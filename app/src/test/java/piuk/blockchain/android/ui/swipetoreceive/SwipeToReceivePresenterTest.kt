package piuk.blockchain.android.ui.swipetoreceive

import android.graphics.Bitmap
import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import piuk.blockchain.android.scan.QrCodeDataManager
import piuk.blockchain.androidcoreui.ui.base.UiState

class SwipeToReceivePresenterTest {

    private lateinit var subject: SwipeToReceivePresenter
    private val activity: SwipeToReceiveView = mock()
    private val swipeToReceiveHelper: SwipeToReceiveHelper = mock()
    private val qrCodeDataManager: QrCodeDataManager = mock()

    @get:Rule
    val initSchedulers = rxInit {
        computationTrampoline()
        mainTrampoline()
    }

    @Before
    fun setUp() {
        subject = SwipeToReceivePresenter(qrCodeDataManager, swipeToReceiveHelper)
        subject.initView(activity)
    }

    @Test
    fun `onViewReady no addresses`() {
        // Arrange
        whenever(swipeToReceiveHelper.getBitcoinReceiveAddresses()).thenReturn(emptyList())
        whenever(swipeToReceiveHelper.getNextAvailableBitcoinAddressSingle())
            .thenReturn(Single.just(""))
        whenever(swipeToReceiveHelper.getBitcoinAccountName()).thenReturn("Bitcoin account")

        // Act
        subject.onViewReady()
        // Assert
        verify(activity).displayCoinType(CryptoCurrency.BTC)
        verify(activity).displayReceiveAccount("Bitcoin account")
        verify(activity).setUiState(UiState.LOADING)
        verify(activity).setUiState(UiState.EMPTY)
    }

    @Test
    fun `onViewReady address returned is empty`() {
        // Arrange
        val addresses = listOf("adrr0", "addr1", "addr2", "addr3", "addr4")
        whenever(swipeToReceiveHelper.getBitcoinReceiveAddresses()).thenReturn(addresses)
        whenever(swipeToReceiveHelper.getBitcoinAccountName()).thenReturn("Account")
        whenever(swipeToReceiveHelper.getNextAvailableBitcoinAddressSingle())
            .thenReturn(Single.just(""))

        // Act
        subject.onViewReady()
        // Assert
        verify(activity).setUiState(UiState.LOADING)
        verify(activity).displayCoinType(CryptoCurrency.BTC)
        verify(activity).displayReceiveAccount("Account")
        verify(activity).setUiState(UiState.FAILURE)
    }

    @Test
    fun `onView ready address returned BTC`() {
        // Arrange
        val bitmap: Bitmap = mock()
        val addresses = listOf("adrr0", "addr1", "addr2", "addr3", "addr4")
        whenever(swipeToReceiveHelper.getBitcoinReceiveAddresses()).thenReturn(addresses)
        whenever(swipeToReceiveHelper.getBitcoinAccountName()).thenReturn("Account")
        whenever(swipeToReceiveHelper.getNextAvailableBitcoinAddressSingle())
            .thenReturn(Single.just("addr0"))
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt())).thenReturn(Observable.just(bitmap))

        // Act
        subject.onViewReady()
        // Assert
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(activity).setUiState(UiState.LOADING)
        verify(activity).displayCoinType(CryptoCurrency.BTC)
        verify(activity).displayReceiveAccount("Account")
        verify(activity).displayQrCode(bitmap)
        verify(activity).setUiState(UiState.CONTENT)
        verify(activity).displayReceiveAddress("addr0")
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun `address returned ETH`() {
        // Arrange
        val address = "addr0"
        val bitmap: Bitmap = mock()

        whenever(swipeToReceiveHelper.getEthReceiveAddress()).thenReturn(address)
        whenever(swipeToReceiveHelper.getEthAccountName()).thenReturn("Account")
        whenever(swipeToReceiveHelper.getEthReceiveAddressSingle()).thenReturn(Single.just(address))
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt())).thenReturn(Observable.just(bitmap))

        // Act
        subject.currencyPosition = 1

        // Assert
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(activity).setUiState(UiState.LOADING)
        verify(activity).displayCoinType(CryptoCurrency.ETHER)
        verify(activity).displayReceiveAccount("Account")
        verify(activity).displayQrCode(bitmap)
        verify(activity).setUiState(UiState.CONTENT)
        verify(activity).displayReceiveAddress("addr0")
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun `onView ready address returned BCH`() {
        // Arrange
        val bitmap: Bitmap = mock()
        val addresses = listOf("adrr0", "addr1", "addr2", "addr3", "addr4")

        whenever(swipeToReceiveHelper.getBitcoinCashReceiveAddresses()).thenReturn(addresses)
        whenever(swipeToReceiveHelper.getBitcoinCashAccountName()).thenReturn("Account")
        whenever(swipeToReceiveHelper.getNextAvailableBitcoinCashAddressSingle()).thenReturn(Single.just("addr0"))
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt())).thenReturn(Observable.just(bitmap))

        // Act
        subject.currencyPosition = 2

        // Assert
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(activity).setUiState(UiState.LOADING)
        verify(activity).displayCoinType(CryptoCurrency.BCH)
        verify(activity).displayReceiveAccount("Account")
        verify(activity).displayQrCode(bitmap)
        verify(activity).setUiState(UiState.CONTENT)
        verify(activity).displayReceiveAddress("addr0")
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun `address returned XLM`() {
        // Arrange
        val uri = "web+stellar:pay?destination=GCALNQQBXAPZ2WIRSDDBMSTAKCUH5SG6U76YBFLQLIXJTF7FE5AX7AOO"
        val bitmap: Bitmap = mock()
        whenever(swipeToReceiveHelper.getXlmReceiveAddress()).thenReturn(uri)
        whenever(swipeToReceiveHelper.getXlmAccountName()).thenReturn("Account")
        whenever(swipeToReceiveHelper.getXlmReceiveAddressSingle()).thenReturn(Single.just(uri))
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
            .thenReturn(Observable.just(bitmap))

        // Act
        subject.currencyPosition = 3

        // Assert
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(activity).setUiState(UiState.LOADING)
        verify(activity).displayCoinType(CryptoCurrency.XLM)
        verify(activity).displayReceiveAccount("Account")
        verify(activity).displayQrCode(bitmap)
        verify(activity).setUiState(UiState.CONTENT)
        verify(activity).displayReceiveAddress("GCALNQQBXAPZ2WIRSDDBMSTAKCUH5SG6U76YBFLQLIXJTF7FE5AX7AOO")
        verifyNoMoreInteractions(activity)
    }
}