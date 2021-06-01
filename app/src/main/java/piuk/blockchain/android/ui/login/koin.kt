package piuk.blockchain.android.ui.login

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module
import piuk.blockchain.android.ui.login.auth.LoginAuthInteractor
import piuk.blockchain.android.ui.login.auth.LoginAuthModel
import piuk.blockchain.android.ui.login.auth.LoginAuthState

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

        factory {
            LoginAuthModel(
                initialState = LoginAuthState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get(),
                interactor = get()
            )
        }

        factory {
            LoginAuthInteractor(
                appUtil = get(),
                authDataManager = get(),
                payloadDataManager = get(),
                prefs = get()
            )
        }
    }
}