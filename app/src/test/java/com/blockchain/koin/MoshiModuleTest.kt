package com.blockchain.koin

import com.blockchain.koin.modules.moshiModule
import com.blockchain.network.modules.apiModule
import com.blockchain.swap.koin.swapModule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.test.AutoCloseKoinTest

class MoshiModuleTest : AutoCloseKoinTest() {

    @Test
    fun `the moshi module injects at least one of the buy sell adapters`() {
        startKoin {
            modules(listOf(
                swapModule,
                apiModule,
                moshiModule,
                nabuModule
            ))
        }
    }
}
