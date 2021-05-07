package info.blockchain.wallet.bip44

import org.apache.commons.codec.binary.Hex
import org.bitcoinj.crypto.MnemonicException.MnemonicWordException
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.params.MainNetParams
import java.io.InputStream
import java.util.Locale

/**
 * WalletFactory.java : Class for creating/restoring/reading BIP44 HD wallet
 *
 * BIP44 extension of Bitcoinj
 */
object HDWalletFactory {
    /**
     * Create new wallet.
     *
     * @param passphrase optional BIP39 passphrase
     * @param nbAccounts create this number of accounts
     * @param purpose BIP43 purpose
     * @return HDWallet
     */
    @JvmStatic
    fun createWallet(
        language: Language,
        passphrase: String,
        nbAccounts: Int,
        purpose: Int,
        seed: ByteArray
    ): HDWallet {
        val networkParameters = MainNetParams.get()

        getMnemonicWordList(language).use { wis ->
            val mc = MnemonicCode(wis, null)
            return HDWallet(mc, networkParameters, seed, passphrase, nbAccounts, purpose)
        }
    }

    @JvmStatic
    fun restoreWallet(
        language: Language,
        data: String,
        passphrase: String,
        nbAccounts: Int,
        purpose: Int
    ): HDWallet {
        val networkParameters = MainNetParams.get()

        getMnemonicWordList(language).use { wis ->
            val mc = MnemonicCode(wis, null)
            val seed = if (data.length % 4 == 0 && !data.contains(" ")) {
                // Hex seed
                Hex.decodeHex(data.toCharArray())
            } else {
                // only use for BIP39 English
                val words = data.replace("[^a-z]+".toRegex(), " ")
                    .trim { it <= ' ' }
                    .split("\\s+".toRegex())
                mc.toEntropy(words)
            }
            return HDWallet(mc, networkParameters, seed, passphrase, nbAccounts, purpose)
        }
    }

    private fun getMnemonicWordList(
        language: Language
    ): InputStream {
        val locale = getLocale(language)
        return HDWalletFactory::class.java.classLoader
            .getResourceAsStream("wordlist/$locale.txt") ?: throw MnemonicWordException("cannot read BIP39 word list")
    }

    fun restoreWatchOnlyWallet(
        xpubList: List<String>
    ): HDWallet = HDWallet(MainNetParams.get(), xpubList)

    private fun getLocale(language: Language): Locale {
        return when (language) {
            Language.US -> Locale("en", "US")
            Language.ES -> Locale("es", "ES")
            Language.FR -> Locale("fr", "FR")
            Language.JP -> Locale("jp", "JP")
            Language.CN -> Locale("zh", "CN")
            Language.TW -> Locale("zh", "TW")
        }
    }

    enum class Language {
        US, ES, FR, JP, CN, TW
    }
}