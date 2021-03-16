package piuk.blockchain.android.ui.transactionflow

import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert
import org.junit.Test
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent

class TransactionFlowIntentMapperTest {

    lateinit var subject: TransactionFlowIntentMapper

    @Test
    fun `swap with defined source and target accounts`() {
        val assetAction = AssetAction.Swap
        val passwordRequired = false

        val sourceAccount: CryptoAccount = mock()
        val target: TransactionTarget = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        val result = subject.map(passwordRequired)

        Assert.assertEquals(
            TransactionIntent.InitialiseWithSourceAndPreferredTarget(
                assetAction,
                sourceAccount,
                target,
                passwordRequired
            ), result
        )
    }

    @Test
    fun `swap with no defined source or target accounts`() {
        val assetAction = AssetAction.Swap
        val passwordRequired = false

        val sourceAccount: FiatAccount = mock()
        val target: NullCryptoAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        val result = subject.map(passwordRequired)

        Assert.assertEquals(
            TransactionIntent.InitialiseWithNoSourceOrTargetAccount(
                assetAction,
                passwordRequired
            ), result
        )
    }

    @Test
    fun `swap with defined source and no defined target account`() {
        val assetAction = AssetAction.Swap
        val passwordRequired = false

        val sourceAccount: CryptoAccount = mock()
        val target: NullCryptoAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        val result = subject.map(passwordRequired)

        Assert.assertEquals(
            TransactionIntent.InitialiseWithSourceAccount(
                assetAction,
                sourceAccount,
                passwordRequired
            ), result
        )
    }

    @Test
    fun `interest deposit with defined source and defined target account`() {
        val assetAction = AssetAction.InterestDeposit
        val passwordRequired = false

        val sourceAccount: CryptoAccount = mock()
        val target: CryptoAccount = mock()
        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        val result = subject.map(passwordRequired)

        Assert.assertEquals(
            TransactionIntent.InitialiseWithSourceAndTargetAccount(
                assetAction,
                sourceAccount,
                target,
                passwordRequired
            ), result
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `interest deposit with no defined source or target`() {
        val assetAction = AssetAction.InterestDeposit
        val passwordRequired = false

        val sourceAccount: FiatAccount = mock()
        val target: NullCryptoAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        subject.map(passwordRequired)
    }

    @Test
    fun `send with defined source account`() {
        val assetAction = AssetAction.Send
        val passwordRequired = false

        val sourceAccount: CryptoAccount = mock()
        val target: NullCryptoAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        val result = subject.map(passwordRequired)

        Assert.assertEquals(
            TransactionIntent.InitialiseWithSourceAccount(
                assetAction,
                sourceAccount,
                passwordRequired
            ),
            result
        )
    }

    @Test
    fun `send with defined source and target accounts`() {
        val assetAction = AssetAction.Send
        val passwordRequired = false

        val sourceAccount: CryptoAccount = mock()
        val target: CryptoAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        val result = subject.map(passwordRequired)

        Assert.assertEquals(
            TransactionIntent.InitialiseWithSourceAndTargetAccount(
                assetAction,
                sourceAccount,
                target,
                passwordRequired
            ),
            result
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `send with no defined source account`() {
        val assetAction = AssetAction.Send
        val passwordRequired = false

        val sourceAccount: FiatAccount = mock()
        val target: CryptoAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        subject.map(passwordRequired)
    }

    @Test
    fun `sell with defined source account`() {
        val assetAction = AssetAction.Sell
        val passwordRequired = false

        val sourceAccount: CryptoAccount = mock()
        val target: NullCryptoAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        val result = subject.map(passwordRequired)

        Assert.assertEquals(
            TransactionIntent.InitialiseWithSourceAccount(
                assetAction,
                sourceAccount,
                passwordRequired
            ),
            result
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `sell with no defined source account`() {

        val assetAction = AssetAction.Sell
        val passwordRequired = false

        val sourceAccount: FiatAccount = mock()
        val target: CryptoAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        subject.map(passwordRequired)
    }

    @Test(expected = IllegalStateException::class)
    fun `fiat deposit with no defined target`() {
        val assetAction = AssetAction.FiatDeposit
        val passwordRequired = false

        val sourceAccount: CryptoAccount = mock()
        val target: NullCryptoAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        subject.map(passwordRequired)
    }

    @Test
    fun `fiat deposit with defined source and defined target account`() {
        val assetAction = AssetAction.FiatDeposit
        val passwordRequired = false

        val sourceAccount: FiatAccount = mock()
        val target: FiatAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        val result = subject.map(passwordRequired)

        Assert.assertEquals(
            TransactionIntent.InitialiseWithSourceAndTargetAccount(
                assetAction,
                sourceAccount,
                target,
                passwordRequired
            ), result
        )
    }

    @Test
    fun `fiat deposit with no source and defined target account`() {
        val assetAction = AssetAction.FiatDeposit
        val passwordRequired = false

        val sourceAccount: CryptoAccount = mock()
        val target: FiatAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        val result = subject.map(passwordRequired)

        Assert.assertEquals(
            TransactionIntent.InitialiseWithTargetAndNoSource(
                assetAction,
                target,
                passwordRequired
            ), result
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `fiat withdraw with no defined source`() {

        val assetAction = AssetAction.Withdraw
        val passwordRequired = false

        val sourceAccount: CryptoAccount = mock()
        val target: FiatAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        subject.map(passwordRequired)
    }

    @Test
    fun `fiat withdraw with defined source and no target account`() {
        val assetAction = AssetAction.Withdraw
        val passwordRequired = false

        val sourceAccount: FiatAccount = mock()
        val target: NullCryptoAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        val result = subject.map(passwordRequired)

        Assert.assertEquals(
            TransactionIntent.InitialiseWithSourceAccount(
                assetAction,
                sourceAccount,
                passwordRequired
            ), result
        )
    }

    @Test
    fun `fiat withdraw with defined source and defined target account`() {
        val assetAction = AssetAction.Withdraw
        val passwordRequired = false

        val sourceAccount: FiatAccount = mock()
        val target: FiatAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        val result = subject.map(passwordRequired)

        Assert.assertEquals(
            TransactionIntent.InitialiseWithSourceAndPreferredTarget(
                assetAction,
                sourceAccount,
                target,
                passwordRequired
            ), result
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `test receive not allowed`() {
        val assetAction = AssetAction.Receive
        val passwordRequired = false

        val sourceAccount: FiatAccount = mock()
        val target: FiatAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        subject.map(passwordRequired)
    }

    @Test(expected = IllegalStateException::class)
    fun `test activity not allowed`() {
        val assetAction = AssetAction.ViewActivity
        val passwordRequired = false

        val sourceAccount: FiatAccount = mock()
        val target: FiatAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        subject.map(passwordRequired)
    }

    @Test(expected = IllegalStateException::class)
    fun `test summary not allowed`() {
        val assetAction = AssetAction.Summary
        val passwordRequired = false

        val sourceAccount: FiatAccount = mock()
        val target: FiatAccount = mock()

        subject = TransactionFlowIntentMapper(sourceAccount, target, assetAction)

        subject.map(passwordRequired)
    }
}