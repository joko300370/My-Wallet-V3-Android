package piuk.blockchain.android.ui.login

import android.app.Activity
import com.google.android.gms.recaptcha.Recaptcha
import com.google.android.gms.recaptcha.RecaptchaAction
import com.google.android.gms.recaptcha.RecaptchaActionType
import com.google.android.gms.recaptcha.RecaptchaHandle
import com.google.android.gms.recaptcha.RecaptchaResultData
import piuk.blockchain.android.BuildConfig
import timber.log.Timber
import java.lang.Exception

class GoogleReCaptchaClient(private val activity: Activity) {
    private lateinit var recaptchaHandle: RecaptchaHandle

    fun initReCaptcha() {
        Recaptcha.getClient(activity)
            .init(BuildConfig.RECAPTCHA_SITE_KEY)
            .addOnSuccessListener { handle ->
                recaptchaHandle = handle
            }
            .addOnFailureListener { exception ->
                Timber.e(exception)
            }
    }

    fun close() {
        Recaptcha.getClient(activity).close(recaptchaHandle)
    }

    fun verifyForLogin(onSuccess: (RecaptchaResultData) -> Unit, onError: (Exception) -> Unit) {
        Recaptcha.getClient(activity)
            .execute(recaptchaHandle, RecaptchaAction(RecaptchaActionType(RecaptchaActionType.LOGIN)))
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onError)
    }
}