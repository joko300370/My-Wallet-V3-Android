package piuk.blockchain.androidcore.data.payload

import com.blockchain.wallet.Seed
import com.blockchain.wallet.SeedAccessWithoutPrompt
import info.blockchain.wallet.exceptions.HDWalletException
import io.reactivex.Maybe

internal class PayloadDataManagerSeedAccessAdapter(
    private val payloadDataManager: PayloadDataManager
) : SeedAccessWithoutPrompt {

    override fun seed(validatedSecondPassword: String?): Maybe<Seed> {
        return Maybe.concat(
            seed,
            Maybe.defer { getSeedGivenPassword(validatedSecondPassword) }
        ).firstElement()
    }

    override val seed: Maybe<Seed>
        get() {
            return getSeedWithoutPassword()
        }

    private fun getSeedWithoutPassword(): Maybe<Seed> =
        getSeedGivenPassword(null)

    private fun getSeedGivenPassword(validatedSecondPassword: String?): Maybe<Seed> {
        try {
            if (validatedSecondPassword != null) {
                payloadDataManager.decryptHDWallet(
                    validatedSecondPassword
                )
            }
            val hdWallet = payloadDataManager.wallet?.walletBody
            val hdSeed = hdWallet?.hdSeed
            return if (hdSeed == null) {
                Maybe.empty()
            } else {
                Maybe.just(Seed(hdSeed = hdSeed))
            }
        } catch (hd: HDWalletException) {
            return Maybe.empty()
        }
    }
}
