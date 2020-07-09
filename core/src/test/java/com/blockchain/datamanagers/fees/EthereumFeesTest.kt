package com.blockchain.datamanagers.fees

import com.blockchain.testutils.wei
import org.amshove.kluent.`should equal`
import org.junit.Test

class EthereumFeesTest {

    @Test
    fun `given gas price and limit in gwei, should return wei`() {
        // One Gwei = 1000,000,000 wei
        EthereumFees(
            1,
            2,
            1
        ).apply {
            gasPriceRegularInWei `should equal` (1000_000_000).toBigInteger()
            gasPricePriorityInWei `should equal` (2000_000_000).toBigInteger()
            gasLimitInGwei `should equal` 1.toBigInteger()
        }
    }

    @Test
    fun `given gas price and limit, should return absolute fee in wei`() {
        EthereumFees(
            5,
            10,
            5
        ).apply {
            absoluteRegularFeeInWei `should equal` 25_000_000_000.wei()
            absolutePriorityFeeInWei `should equal` 50_000_000_000.wei()
        }
    }
}