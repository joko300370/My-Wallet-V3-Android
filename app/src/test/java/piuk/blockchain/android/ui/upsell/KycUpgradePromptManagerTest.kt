package piuk.blockchain.android.ui.upsell

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.nhaarman.mockito_kotlin.any
import io.reactivex.Single
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.identity.UserIdentity

class KycUpgradePromptManagerTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val identity: UserIdentity = mock()
    private val subject = KycUpgradePromptManager(
        identity = identity
    )

    @Test
    fun `custodial receive when not silver shows upgrade prompt`() {
        // Arrange
        val account: CustodialTradingAccount = mock()
        whenever(identity.isVerifiedFor(any()))
            .thenReturn(Single.just(false))

        // Act
        subject.queryUpsell(AssetAction.Receive, account)
            .test()
            .assertValue { it == KycUpgradePromptManager.Type.CUSTODIAL_RECEIVE }
    }

    @Test
    fun `custodial receive when gold does not shows upgrade prompt`() {
        // Arrange
        val account: CustodialTradingAccount = mock()
        whenever(identity.isVerifiedFor(any()))
            .thenReturn(Single.just(true))

        // Act
        subject.queryUpsell(AssetAction.Receive, account)
            .test()
            .assertValue { it == KycUpgradePromptManager.Type.NONE }
    }

    @Test
    fun `non custodial receive when not silver does not shows upgrade prompt`() {
        // Arrange
        val account: CryptoNonCustodialAccount = mock()
        whenever(identity.isVerifiedFor(any()))
            .thenReturn(Single.just(false))

        // Act
        subject.queryUpsell(AssetAction.Receive, account)
            .test()
            .assertValue { it == KycUpgradePromptManager.Type.NONE }
    }

    @Test
    fun `custodial send when not silver shows upgrade prompt`() {
        // Arrange
        val account: CustodialTradingAccount = mock()
        whenever(identity.isVerifiedFor(any()))
            .thenReturn(Single.just(false))

        // Act
        subject.queryUpsell(AssetAction.Send, account)
            .test()
            .assertValue { it == KycUpgradePromptManager.Type.NONE }
    }
}
