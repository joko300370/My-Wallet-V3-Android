package piuk.blockchain.android.ui.start

import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

val startupUiModule = applicationContext {

    context("Payload") {
        factory {
            LandingPresenter(
                environmentSettings = get(),
                prefs = get(),
                rootUtil = get()
            )
        }

        factory {
            LoginPresenter(
                _payloadDataManager = lazy { get<PayloadDataManager>() },
                appUtil = get(),
                analytics = get(),
                prefs = get()
            )
        }

        factory {
            ManualPairingPresenter(
                appUtil = get(),
                authDataManager = get(),
                payloadDataManager = get(),
                prefs = get(),
                analytics = get()
            )
        }

        factory {
            PasswordRequiredPresenter(
                appUtil = get(),
                prefs = get(),
                authDataManager = get(),
                payloadDataManager = get()
            )
        }
    }
}