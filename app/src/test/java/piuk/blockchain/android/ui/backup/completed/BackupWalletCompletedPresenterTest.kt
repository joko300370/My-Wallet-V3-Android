package piuk.blockchain.android.ui.backup.completed

import com.blockchain.preferences.WalletStatus
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.account.PendingTransaction
import piuk.blockchain.android.ui.account.TransferFundsDataManager
import piuk.blockchain.android.ui.account.TransferableFundTransactionList

class BackupWalletCompletedPresenterTest {

    private lateinit var subject: BackupWalletCompletedPresenter
    private val view: BackupWalletCompletedView = mock()
    private val transferFundsDataManager: TransferFundsDataManager = mock()
    private val walletStatus: WalletStatus = mock()

    @Before
    fun setUp() {
        subject = BackupWalletCompletedPresenter(transferFundsDataManager, walletStatus)
        subject.initView(view)
    }

    @Test
    fun `onViewReady set backup date`() {
        // Arrange
        val date = 1499181978000L
        whenever(walletStatus.lastBackupTime).thenReturn(date)
        // Act
        subject.onViewReady()
        // Assert
        verify(walletStatus).lastBackupTime
        verifyNoMoreInteractions(walletStatus)
        verify(view).showLastBackupDate(date)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady hide backup date`() {
        // Arrange
        whenever(walletStatus.lastBackupTime).thenReturn(0L)
        // Act
        subject.onViewReady()
        // Assert
        verify(walletStatus).lastBackupTime
        verifyNoMoreInteractions(walletStatus)
        verify(view).hideLastBackupDate()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `checkTransferableFunds success`() {
        // Arrange
        val result = TransferableFundTransactionList(
            pendingTransactions = listOf(PendingTransaction()),
            totalToSend = 0.toBigInteger(),
            totalFee = 0.toBigInteger()
        )

        whenever(transferFundsDataManager.transferableFundTransactionListForDefaultAccount)
            .thenReturn(Observable.just(result))

        // Act
        subject.checkTransferableFunds()
        // Assert
        verify(transferFundsDataManager).transferableFundTransactionListForDefaultAccount
        verifyNoMoreInteractions(transferFundsDataManager)
        verify(view).showTransferFundsPrompt()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `checkTransferableFunds failure`() {
        // Arrange
        whenever(transferFundsDataManager.transferableFundTransactionListForDefaultAccount)
            .thenReturn(Observable.error { Throwable() })
        // Act
        subject.checkTransferableFunds()
        // Assert
        verify(transferFundsDataManager).transferableFundTransactionListForDefaultAccount
        verifyNoMoreInteractions(transferFundsDataManager)
        verifyZeroInteractions(view)
    }
}
