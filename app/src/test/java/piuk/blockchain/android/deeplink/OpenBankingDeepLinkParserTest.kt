package piuk.blockchain.android.deeplink

import android.net.Uri
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import kotlin.test.assertNull

@Config(sdk = [23], application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class OpenBankingDeepLinkParserTest {
    private val subject = OpenBankingDeepLinkParser()

    @Test
    fun `Valid linking URI is correctly parsed`() {
        val uri = Uri.parse(VALID_BANK_LINK_TEST_URI)

        val r = subject.mapUri(uri)

        Assert.assertEquals(r, LinkState.OpenBankingLink(OpenBankingLinkType.LINK_BANK, ""))
    }

    @Test
    fun `Valid approval URI is correctly parsed`() {
        val uri = Uri.parse(VALID_BANK_APPROVAL_TEST_URI)

        val r = subject.mapUri(uri)

        Assert.assertEquals(r, LinkState.OpenBankingLink(OpenBankingLinkType.PAYMENT_APPROVAL, ""))
    }

    @Test
    fun `Malformed URI returns unknown`() {
        val uri = Uri.parse(INVALID_TEST_URI)

        val r = subject.mapUri(uri)

        Assert.assertEquals(r, LinkState.OpenBankingLink(OpenBankingLinkType.UNKNOWN, ""))
    }

    @Test
    fun `empty URI returns null`() {
        val uri = Uri.parse("")

        val r = subject.mapUri(uri)

        assertNull(r)
    }

    companion object {
        private const val VALID_BANK_LINK_TEST_URI =
            "https://blockchainwallet.page.link/#/open/ob-bank-link"
        private const val VALID_BANK_APPROVAL_TEST_URI =
            "https://blockchainwallet.page.link/#/open/ob-bank-approval"

        private const val INVALID_TEST_URI =
            "https://blockchainwallet.page.link/#/open/ob-bank-link-error"
    }
}