package piuk.blockchain.android.ui.auth.newlogin

import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.BrowserIdentity
import com.blockchain.preferences.SecureChannelPrefs
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.crypto.ECDHUtil
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import piuk.blockchain.androidcore.utils.pubKeyHash
import timber.log.Timber
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class SecureChannelManager(
    private val secureChannelPrefs: SecureChannelPrefs,
    private val authPrefs: AuthPrefs,
    private val payloadManager: PayloadManager,
    private val walletApi: WalletApi
) {

    private val compositeDisposable = CompositeDisposable()

    fun sendErrorMessage(channelId: String, pubKeyHash: String) {
        sendMessage(SecureChannelMessage.Empty, channelId, pubKeyHash, false)
        secureChannelPrefs.removeBrowserIdentity(pubKeyHash)
    }

    fun sendHandshake(json: String) {
        val secureChannelPairingCode = jsonBuilder.decodeFromString<SecureChannelPairingCode>(json)

        val browserIdentity = BrowserIdentity(secureChannelPairingCode.pubkey)
        secureChannelPrefs.addBrowserIdentity(browserIdentity)

        val handshake = SecureChannelMessage.PairingHandshake(
            authPrefs.walletGuid,
            secureChannelPairingCode.pubkey
        )

        sendMessage(handshake, secureChannelPairingCode.channelId, browserIdentity.pubKeyHash(), true)
    }

    fun sendLoginMessage(channelId: String, pubKeyHash: String) {
        val loginMessage = SecureChannelMessage.Login(
            authPrefs.walletGuid,
            payloadManager.tempPassword,
            authPrefs.sharedKey,
            true
        )

        sendMessage(loginMessage, channelId, pubKeyHash, true)
        secureChannelPrefs.updateBrowserIdentityUsedTimestamp(pubKeyHash)
    }

    fun decryptMessage(pubKeyHash: String, messageEncrypted: String): SecureChannelBrowserMessage? {
        val identity = secureChannelPrefs.getBrowserIdentity(pubKeyHash)
            ?: return null

        val json = ECDHUtil.getDecryptedMessage(
            getDeviceKey(),
            identity,
            messageEncrypted
        ).toString(Charset.defaultCharset())
        val message = jsonBuilder.decodeFromString<SecureChannelBrowserMessage>(json)

        if (System.currentTimeMillis() - message.timestamp > TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES)) {
            return null
        }

        return message
    }

    private fun sendMessage(
        msg: SecureChannelMessage,
        channelId: String,
        pubKeyHash: String,
        success: Boolean
    ) {
        val browserIdentity = secureChannelPrefs.getBrowserIdentity(pubKeyHash)
            ?: throw RuntimeException() // If we get here, die
        val signingKey = getDeviceKey()

        val response = SecureChannelPairingResponse(
            channelId = channelId,
            pubkey = ECDHUtil.getPublicKeyAsHexString(signingKey),
            success = success,
            message = ECDHUtil.getEncryptedMessage(
                signingKey = signingKey,
                browserIdentity = browserIdentity,
                serializedMessage = jsonBuilder.encodeToString(msg)
            )
        )
        compositeDisposable +=
            walletApi.sendSecureChannel(
                jsonBuilder.encodeToString(response)
            )
                .ignoreElements()
                .subscribeOn(Schedulers.io())
                .subscribe({ /*no-op*/ }, { Timber.e(it) })
    }

    private fun getDeviceKey() = SigningKey.createSigningKeyFromPrivateKey(secureChannelPrefs.deviceKey)

    companion object {
        const val TIME_OUT_IN_MINUTES: Long = 10
        val jsonBuilder = Json {
            classDiscriminator = "class"
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}

@Serializable
sealed class SecureChannelMessage {

    object Empty : SecureChannelMessage()

    @Serializable
    data class PairingHandshake(
        val guid: String,
        val pubkey: String,
        val type: String = "handshake"
    ) : SecureChannelMessage()

    @Serializable
    data class Login(
        val guid: String,
        val password: String,
        val sharedKey: String,
        val remember: Boolean,
        val type: String = "login_wallet"
    ) : SecureChannelMessage()
}

@Serializable
data class SecureChannelPairingCode(
    val pubkey: String,
    val channelId: String
)

@Serializable
data class SecureChannelPairingResponse(
    val channelId: String,
    val pubkey: String,
    val success: Boolean,
    val message: String
)

@Serializable
data class SecureChannelBrowserMessage(
    val type: String,
    val channelId: String,
    val timestamp: Long
)
