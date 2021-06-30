package piuk.blockchain.android.util

import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.payload.data.WalletBody
import org.amshove.kluent.`should equal`
import org.amshove.kluent.itReturns
import org.amshove.kluent.itThrows
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class BackupWalletUtilTest {

    private lateinit var subject: BackupWalletUtil
    private val payloadDataManager: PayloadDataManager = mock()

    @Before
    fun setUp() {
        subject = BackupWalletUtil(payloadDataManager)
    }

    @Test
    fun getConfirmSequence() {
        // Arrange
        val expectedMnemonic = listOf("one", "two", "three", "four")
        val hdwallet: WalletBody = mock {
            on { mnemonic } itReturns expectedMnemonic
        }

        val wallet: Wallet = mock {
            on { walletBody } itReturns hdwallet
        }

        whenever(payloadDataManager.wallet).thenReturn(wallet)

        // Act
        val result = subject.getConfirmSequence(null)

        // Assert
        verify(payloadDataManager, atLeastOnce()).wallet
        verifyNoMoreInteractions(payloadDataManager)
        result.size `should equal` 3
    }

    @Test
    fun `getMnemonic success`() {
        // Arrange
        val expectedMnemonic = listOf("one", "two", "three", "four")
        val hdwallet: WalletBody = mock {
            on { mnemonic } itReturns expectedMnemonic
        }

        val wallet: Wallet = mock {
            on { walletBody } itReturns hdwallet
        }

        whenever(payloadDataManager.wallet).thenReturn(wallet)

        // Act
        val result = subject.getMnemonic(null)

        // Assert
        verify(payloadDataManager).wallet
        verify(wallet).decryptHDWallet(null)
        verify(wallet).walletBody

        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(wallet)

        result `should equal` expectedMnemonic
    }

    @Test
    fun `getMnemonic error`() {
        // Arrange
        val wallet: Wallet = mock {
            on { decryptHDWallet(null) } itThrows NullPointerException()
        }
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        // Act
        val result = subject.getMnemonic(null)
        // Assert
        verify(payloadDataManager).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(wallet).decryptHDWallet(null)
        verifyNoMoreInteractions(wallet)
        result `should equal` null
    }
}