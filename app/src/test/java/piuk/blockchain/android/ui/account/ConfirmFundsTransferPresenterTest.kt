package piuk.blockchain.android.ui.account

import android.annotation.SuppressLint
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.testutils.satoshi
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.Completable
import org.amshove.kluent.any
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.backup.transfer.ConfirmFundsTransferPresenter
import piuk.blockchain.android.ui.backup.transfer.ConfirmFundsTransferView
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.ui.chooser.WalletAccountHelper
import piuk.blockchain.androidcore.data.events.PayloadSyncedEvent
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

class ConfirmFundsTransferPresenterTest {

    private val view: ConfirmFundsTransferView = mock()
    private val walletAccountHelper: WalletAccountHelper = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val stringUtils: StringUtils = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency }.thenReturn("USD")
    }

    private val subject = ConfirmFundsTransferPresenter(
            walletAccountHelper,
            payloadDataManager,
            stringUtils,
            exchangeRates,
            currencyPrefs
        )

    @Before
    fun setUp() {
        subject.initView(view)
        Locale.setDefault(Locale.US)
    }

    @Test
    fun onViewReady() {
        // Arrange
        val mockPayload = mock(Wallet::class.java, RETURNS_DEEP_STUBS)
        whenever(payloadDataManager.wallet).thenReturn(mockPayload)
        whenever(mockPayload.hdWallets[0].defaultAccountIdx).thenReturn(0)

        // Act
        subject.onViewReady()

        // Assert
        assertEquals(0, subject.pendingTransactions.size)
    }

    @SuppressLint("VisibleForTests")
    @Test
    fun updateUi() {
        // Arrange
        val total = 100000000.satoshi()
        val fee = 10000.satoshi()

        whenever(stringUtils.getQuantityString(anyInt(), anyInt())).thenReturn("test string")
        whenever(exchangeRates.getLastPrice(any(), any())).thenReturn(100.0)

        // Act
        subject.updateUi(total, fee)

        // Assert
        verify(view).updateFromLabel("test string")
        verify(view).updateTransferAmountBtc("1.0 BTC")
        verify(view).updateTransferAmountFiat("$100.00")
        verify(view).updateFeeAmountBtc("0.0001 BTC")
        verify(view).updateFeeAmountFiat("$0.01")
        verify(view).onUiUpdated()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun getReceiveToList() {
        // Arrange
        whenever(walletAccountHelper.getHdAccounts()).thenReturn(listOf())
        // Act
        val value = subject.getReceiveToList()
        // Assert
        assertNotNull(value)
        assertTrue(value.isEmpty())
    }

    @Test
    fun getDefaultAccount() {
        // Arrange
        whenever(payloadDataManager.defaultAccountIndex).thenReturn(0)
        whenever(payloadDataManager.getPositionOfAccountFromActiveList(0)).thenReturn(1)
        // Act
        val value = subject.getDefaultAccount()
        // Assert
        assertEquals(0, value.toLong())
    }

    @Test
    fun `archiveAll successful`() {
        // Arrange
        val transaction = PendingTransaction()
        transaction.sendingObject = ItemAccount()
        transaction.sendingObject!!.accountObject = LegacyAddress()
        subject.pendingTransactions.addAll(listOf(transaction))

        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        // Act
        subject.archiveAll()
        // Assert
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(R.string.transfer_archive, ToastCustom.TYPE_OK)
        verify(view).dismissDialog()
        verify(view).sendBroadcast(any<PayloadSyncedEvent>())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `archiveAll unsuccessful`() {
        // Arrange
        val transaction = PendingTransaction()
        transaction.sendingObject = ItemAccount()
        transaction.sendingObject!!.accountObject = LegacyAddress()
        subject.pendingTransactions.addAll(listOf(transaction))

        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.error(Throwable()))
        // Act
        subject.archiveAll()
        // Assert
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
        verify(view).dismissDialog()
        verify(view).sendBroadcast(any<PayloadSyncedEvent>())
        verifyNoMoreInteractions(view)
    }
}