package piuk.blockchain.androidcore.data.payload

import com.blockchain.wallet.SeedAccessWithoutPrompt
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import info.blockchain.wallet.payload.data.WalletBody
import info.blockchain.wallet.payload.data.Wallet
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.junit.Test

class PayloadDataManagerSeedAccessAdapterTest {

    @Test
    fun `extracts seed from the first HDWallet`() {
        val theSeed = byteArrayOf(1, 2, 3)
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(
            givenADecodedPayload(theSeed)
        )

        seedAccess.seed
            .test()
            .values()
            .single()
            .apply {
                hdSeed `should be` theSeed
            }
    }

    @Test
    fun `if the HD wallet throws HD Exception, returns empty`() {
        val theSeed = byteArrayOf(1, 2, 3)
        val hdWallet = mock<WalletBody> {
            on { hdSeed } `it returns` theSeed
        }
        val wallet = mock<Wallet> {
            on { walletBodies } `it returns` listOf(hdWallet)
        }
        val payloadDataManager = mock<PayloadDataManager> {
            on { this.wallet } `it returns` wallet
        }
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(payloadDataManager)
        seedAccess.seed
            .test()
            .assertComplete()
            .assertValueCount(0)
    }

    @Test
    fun `if the list is null, returns empty`() {
        val wallet = mock<Wallet> {
            on { walletBody } `it returns` null
        }
        val payloadDataManager = mock<PayloadDataManager> {
            on { this.wallet } `it returns` wallet
        }
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(payloadDataManager)
        seedAccess.seed
            .test()
            .assertComplete()
            .assertValueCount(0)
    }

    @Test
    fun `if the wallet is null, returns empty`() {
        val payloadDataManager = mock<PayloadDataManager> {
            on { this.wallet } `it returns` null
        }
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(payloadDataManager)
        seedAccess.seed
            .test()
            .assertComplete()
            .assertValueCount(0)
    }

    @Test
    fun `extracts seed from the first HDWallet without decrypting - when already decoded`() {
        val theSeed = byteArrayOf(1, 2, 3)
        val payloadDataManager = givenADecodedPayload(theSeed)
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(
            payloadDataManager
        )
        seedAccess.seed("PASSWORD").test().values().single()
            .apply {
                hdSeed `should be` theSeed
            }
        verify(payloadDataManager, never()).decryptHDWallet(any())
    }

    @Test
    fun `decrypts if required`() {
        val payloadDataManager = mock<PayloadDataManager> {
            on { this.wallet } `it returns` null
        }
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(
            payloadDataManager
        )
        seedAccess.seed("PASSWORD").test()
        verify(payloadDataManager).decryptHDWallet("PASSWORD")
    }
}

private fun givenADecodedPayload(
    theSeed: ByteArray
): PayloadDataManager {
    val hdwallet = mock<WalletBody> {
        on { hdSeed } `it returns` theSeed
    }
    val wallet = mock<Wallet> {
        on { walletBody } `it returns` hdwallet
    }
    return mock {
        on { this.wallet } `it returns` wallet
    }
}
