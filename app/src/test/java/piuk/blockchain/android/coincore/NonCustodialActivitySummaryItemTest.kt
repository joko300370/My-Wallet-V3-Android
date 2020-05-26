package piuk.blockchain.android.coincore

import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not equal`
import org.junit.Test

class NonCustodialActivitySummaryItemTest {

    @Test
    fun `ensure not equal when compared to different type`() {

        val activityItem = TestNonCustodialSummaryItem()
        val objectToCompare = Any()

        activityItem.toString() `should not equal` objectToCompare.toString()
        activityItem.hashCode() `should not equal` objectToCompare.hashCode()
        activityItem `should not equal` objectToCompare
    }

    @Test
    fun `ensure equals, hashCode and toString work correctly with subtly different objects`() {

        val itemOne = TestNonCustodialSummaryItem()
            .apply { note = "note 1" }

        val itemTwo = TestNonCustodialSummaryItem()
            .apply { note = "note 2" }

        itemOne.toString() `should not equal` itemTwo.toString()
        itemOne.hashCode() `should not equal` itemTwo.hashCode()
        itemOne `should not equal` itemTwo
    }

    @Test
    fun `ensure equals, hashCode and toString work correctly with identical objects`() {

        val itemOne = TestNonCustodialSummaryItem()
            .apply { note = "note" }

        val itemTwo = TestNonCustodialSummaryItem()
            .apply { note = "note" }

        itemOne.toString() `should equal` itemTwo.toString()
        itemOne.hashCode() `should equal` itemTwo.hashCode()
        itemOne `should equal` itemTwo
    }
}
