package com.blockchain.koin.modules

import com.blockchain.koin.payloadScope
import com.blockchain.koin.walletModule
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.api.BitcoinApi
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.PayloadManagerWiper
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should not be`
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest

class PayloadManagerWipingTest : AutoCloseKoinTest() {

    @Test
    fun `After wiping the payload manager, a new request for a payload manager gets a distinct instance`() {
        startKoin {
            modules(listOf(
                walletModule,
                module {
                    single { mock<WalletApi>() }
                    single { mock<BitcoinApi>() }
                }
            ))
        }

        val firstPayloadManager: PayloadManager = payloadScope.get()
        val secondPayloadManager: PayloadManager = payloadScope.get()

        firstPayloadManager `should be` secondPayloadManager

        val wiper: PayloadManagerWiper = payloadScope.get()

        wiper.wipe()

        val thirdPayloadManager: PayloadManager by payloadScope.inject()

        thirdPayloadManager `should not be` secondPayloadManager
    }
}