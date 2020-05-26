package piuk.blockchain.android.simplebuy

import com.blockchain.remoteconfig.FeatureFlag
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class SimpleBuyAvailabilityTest {

    private val simpleBuyFlag: FeatureFlag = mock()

    private lateinit var simpleBuyAvailability: SimpleBuyAvailability

    @Before
    fun setup() {
        simpleBuyAvailability = SimpleBuyAvailability(
            simpleBuyFlag
        )
    }

    @Test
    fun `should work in the happy scenario`() {
        whenever(simpleBuyFlag.enabled).thenReturn(Single.just(true))
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, true)
    }

    @Test
    fun `should not be available when feature flag is off`() {
        whenever(simpleBuyFlag.enabled).thenReturn(Single.just(false))
        simpleBuyAvailability.isAvailable().test().assertValueAt(0, false)
    }
}