package piuk.blockchain.android.coincore

import org.koin.dsl.module.applicationContext

val coincoreModule = applicationContext {

    context("Payload") {

        bean {
            STXTokens(
                payloadManager = get()
            )
        }
    }
}
