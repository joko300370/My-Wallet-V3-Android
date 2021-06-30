package com.blockchain.notifications

import android.annotation.SuppressLint
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.NotificationPrefs
import com.google.common.base.Optional
import com.google.firebase.iid.FirebaseInstanceId
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import timber.log.Timber

class NotificationTokenManager(
    private val notificationService: NotificationService,
    private val payloadManager: PayloadManager,
    private val prefs: NotificationPrefs,
    private val firebaseInstanceId: FirebaseInstanceId,
    private val rxBus: RxBus,
    private val crashLogger: CrashLogger
) {

    /**
     * Returns the stored Firebase token, otherwise attempts to trigger a refresh of the token which
     * will be handled appropriately by [InstanceIdService]
     *
     * @return The Firebase token
     */
    private val storedFirebaseToken: Observable<Optional<String>>
        get() {
            val storedToken = prefs.firebaseToken

            return if (storedToken.isNotEmpty()) {
                Observable.just(Optional.of(storedToken))
            } else {
                Observable.create { subscriber ->
                    FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
                        val newToken = instanceIdResult.token
                        prefs.firebaseToken = newToken
                        subscriber.onNext(Optional.of(newToken))
                        subscriber.onComplete()
                    }
                    FirebaseInstanceId.getInstance().instanceId.addOnFailureListener {
                        if (!subscriber.isDisposed)
                            subscriber.onError(it)
                    }
                }
            }
        }

    /**
     * Sends the access token to the update-firebase endpoint once the user is logged in fully.
     *
     * @param token A Firebase access token
     */
    @SuppressLint("CheckResult")
    fun storeAndUpdateToken(token: String) {
        prefs.firebaseToken = token
        if (token.isNotEmpty()) {
            sendFirebaseToken(token)
                .subscribeOn(Schedulers.io())
                .subscribe({ /*no-op*/ }, { Timber.e(it) })
        }
    }

    @SuppressLint("CheckResult")
    fun registerAuthEvent() {
        val loginObservable = rxBus.register(AuthEvent::class.java)

        loginObservable
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { authEvent ->
                if (authEvent == AuthEvent.FORGET) {
                    revokeAccessToken()
                } else {
                    Completable.complete()
                }
            }
            .subscribe({
                // no-op
            }, { Timber.e(it) })
    }

    /**
     * Disables push notifications flag.
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    fun disableNotifications(): Completable {
        prefs.arePushNotificationsEnabled = false
        return revokeAccessToken()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Resets Instance ID and revokes all tokens. Clears stored token if successful
     */
    private fun revokeAccessToken(): Completable {
        return Completable.fromCallable {
            firebaseInstanceId.deleteInstanceId()
            Void.TYPE
        }.andThen(removeNotificationToken())
            .doOnComplete { this.clearStoredToken() }
            .subscribeOn(Schedulers.io())
    }

    /**
     * Enables push notifications flag.
     * Resend stored notification token, or generate and send new token if no stored token exists.
     */
    fun enableNotifications(): Completable {
        prefs.arePushNotificationsEnabled = true
        return resendNotificationToken()
    }

    /**
     * If no stored notification token exists, it will be refreshed
     * and will be handled appropriately by FcmCallbackService
     */
    fun resendNotificationToken(): Completable {
        return storedFirebaseToken
            .flatMapCompletable { optional ->
                if (optional.isPresent) {
                    sendFirebaseToken(optional.get())
                } else {
                    Completable.complete()
                }
            }
            .doOnError { throwable ->
                crashLogger.logException(
                    throwable = throwable,
                    logMsg = "Failed to resend the Firebase token for notifications"
                )
            }
    }

    private fun sendFirebaseToken(refreshedToken: String): Completable {
        return if (prefs.arePushNotificationsEnabled && payloadManager.payload != null) {

            val payload = payloadManager.payload
            val guid = payload!!.guid
            val sharedKey = payload.sharedKey

            // TODO: 09/11/2016 Decide what to do if sending fails, perhaps retry?
            notificationService.sendNotificationToken(refreshedToken, guid, sharedKey)
                .subscribeOn(Schedulers.io())
        } else {
            Completable.complete()
        }
    }

    /**
     * Removes the stored token from Shared Preferences
     */
    private fun clearStoredToken() {
        prefs.firebaseToken = ""
    }

    /**
     * Removes the stored token from back end
     */
    private fun removeNotificationToken(): Completable {

        val token = prefs.firebaseToken

        return if (token.isNotEmpty()) {
            notificationService.removeNotificationToken(token)
        } else {
            Completable.complete()
        }
    }
}
