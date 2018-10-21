package piuk.blockchain.androidcore.data.payload

import com.blockchain.koin.KoinDaggerModule
import com.blockchain.wallet.SeedAccess
import com.blockchain.wallet.Seed
import info.blockchain.wallet.exceptions.HDWalletException
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.androidcore.data.SecondPasswordHandler
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

internal class PayloadDataManagerSeedAccessAdapter(
    private val environmentConfig: EnvironmentConfig,
    private val payloadDataManager: PayloadDataManager
) : SeedAccess {

    override val seed: Maybe<Seed>
        get() {
            return Maybe.concat(
                getSeedWithoutPassword(),
                getSeedWithPassword()
            ).firstElement()
        }

    private fun getSeedWithPassword(): Maybe<Seed> =
        secondPassword()
            .subscribeOn(AndroidSchedulers.mainThread())
            .flatMap { getSeedGivenPassword(it) }

    private fun getSeedWithoutPassword(): Maybe<Seed> =
        getSeedGivenPassword(null)

    private fun getSeedGivenPassword(validatedSecondPassword: String?): Maybe<Seed> {
        try {
            if (validatedSecondPassword != null) {
                payloadDataManager.decryptHDWallet(
                    environmentConfig.bitcoinNetworkParameters,
                    validatedSecondPassword
                )
            }
            val hdWallet = payloadDataManager.wallet?.hdWallets?.get(0)
            val hdSeed = hdWallet?.hdSeed
            val masterKey = hdWallet?.masterKey?.privKeyBytes
            return if (hdSeed == null || masterKey == null) {
                Maybe.empty()
            } else {
                Maybe.just(
                    Seed(
                        hdSeed = hdSeed,
                        masterKey = masterKey
                    )
                )
            }
        } catch (hd: HDWalletException) {
            return Maybe.empty()
        }
    }

    private fun secondPassword(): Maybe<String> {
        val secondPasswordHandler = secondPasswordHandler() ?: return Maybe.empty()

        val password = PublishSubject.create<String>()

        secondPasswordHandler.validate(
            object : SecondPasswordHandler.ResultListener {
                override fun onNoSecondPassword() {
                    password.onComplete()
                }

                override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                    password.onNext(validatedSecondPassword)
                }
            }
        )

        return password.firstElement()
    }

    private fun secondPasswordHandler(): SecondPasswordHandler? {
        return try {
            KoinCheat().get(SecondPasswordHandler::class)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }
}

private class KoinCheat : KoinDaggerModule()
