package piuk.blockchain.android.ui.login

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module

val loginUiModule = module {

    scope(payloadScopeQualifier) {
        factory {
            LoginModel(
                initialState = LoginState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get(),
                interactor = get()
            )
        }

        factory {
            LoginInteractor(
                authService = get(),
                payloadDataManager = get(),
                prefs = get(),
                appUtil = get()
            )
        }
    }
}