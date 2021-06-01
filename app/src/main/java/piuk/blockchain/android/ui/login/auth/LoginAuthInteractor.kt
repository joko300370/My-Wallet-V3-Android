package piuk.blockchain.android.ui.login.auth

import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import retrofit2.Response

class LoginAuthInteractor(
    val appUtil: AppUtil,
    val authDataManager: AuthDataManager,
    val payloadDataManager: PayloadDataManager,
    val prefs: PersistentPrefs
) {

    fun getSessionId() = prefs.sessionId

    fun cleaarSessionId() = prefs.clearSessionId()

    fun authorizeApproval(authToken: String, sessionId: String): Single<Response<ResponseBody>> {
        return authDataManager.authorizeSession(authToken, sessionId)
    }

    fun getPayLoad(guid: String, sessionId: String): Single<Response<ResponseBody>> =
        Single.fromObservable(authDataManager.getEncryptedPayload(guid, sessionId))

    fun verifyPassword(payload: String, password: String): Completable {
        return payloadDataManager.initializeFromPayload(payload, password)
            .doOnComplete {
                payloadDataManager.wallet?.let { wallet ->
                    prefs.sharedKey = wallet.sharedKey
                    prefs.walletGuid = wallet.guid
                }
                prefs.setValue(PersistentPrefs.KEY_EMAIL_VERIFIED, true)
                prefs.pinId = ""
            }
    }

    fun submitCode(
        guid: String,
        sessionId: String,
        code: String,
        payloadJson: String
    ): Single<ResponseBody> {
        return Single.fromObservable(
            authDataManager.submitTwoFactorCode(sessionId, guid, code).map { response ->
                val responseObject = JSONObject(payloadJson).apply {
                    put(LoginAuthIntents.PAYLOAD, response.string())
                }
                responseObject.toString()
                    .toResponseBody("application/json".toMediaTypeOrNull())
            }
        )
    }
}