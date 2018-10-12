package info.blockchain.balance

import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not equal`
import org.junit.Before
import org.junit.Test
import java.util.Locale

class FiatValueFromStringTest {

    @Before
    fun setUs() {
        Locale.setDefault(Locale.US)
    }

    @Before
    fun clearOther() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `empty string`() {
        FiatValue.fromMajor("USD", "") `should equal` 0.usd()
    }

    @Test
    fun `bad string`() {
        FiatValue.fromMajor("GBP", "a") `should equal` 0.gbp()
    }

    @Test
    fun `one dollar`() {
        FiatValue.fromMajor("USD", "1") `should equal` 1.usd()
    }

    @Test
    fun `2 dp dollars`() {
        FiatValue.fromMajor("USD", "1.23") `should equal` 1.23.usd()
    }

    @Test
    fun `French input`() {
        Locale.setDefault(Locale.FRANCE)
        FiatValue.fromMajor("EUR", "1,12") `should equal` 1.12.eur()
    }

    @Test
    fun `UK input`() {
        Locale.setDefault(Locale.UK)
        FiatValue.fromMajor("EUR", "1,12") `should equal` 112.eur()
    }

    @Test
    fun `Override locale input`() {
        Locale.setDefault(Locale.UK)
        FiatValue.fromMajor("EUR", "1,12", Locale.FRANCE) `should equal` 1.12.eur()
    }

    @Test
    fun `rounds down below midway`() {
        FiatValue.fromMajor("GBP", "1.2349") `should equal` 1.23.gbp()
    }

    @Test
    fun `rounds down below midway - JPY`() {
        FiatValue.fromMajor("JPY", "123.49") `should equal` 123.jpy()
    }

    @Test
    fun `rounds up at midway - JPY`() {
        FiatValue.fromMajor("JPY", "123.5") `should equal` 124.jpy()
    }

    @Test
    fun `very large number test - ensures does not go via double`() {
        val major = "1234567890123456.78"
        val largeNumberBigDecimal = major.toBigDecimal()
        FiatValue.fromMajor("USD", major) `should equal`
            largeNumberBigDecimal.usd()
        FiatValue.fromMajor("USD", major) `should not equal`
            largeNumberBigDecimal.toDouble().usd()
    }
}
