package piuk.blockchain.android.util

import android.content.res.Resources
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test
import piuk.blockchain.android.R

class ResourceDefaultLabelsTest {

    private val resources: Resources = mock {
        on { getString(R.string.default_crypto_non_custodial_wallet_label) } `it returns` "Private Key"
    }

    private val defaultLabels: DefaultLabels =
        ResourceDefaultLabels(resources)

    @Test
    fun `btc default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel(CryptoCurrency.BTC) `should equal` "Private Key"
    }

    @Test
    fun `ether default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel(CryptoCurrency.ETHER) `should equal` "Private Key"
    }

    @Test
    fun `bch default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel(CryptoCurrency.BCH) `should equal` "Private Key"
    }

    @Test
    fun `xlm default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel(CryptoCurrency.XLM) `should equal` "Private Key"
    }
}
