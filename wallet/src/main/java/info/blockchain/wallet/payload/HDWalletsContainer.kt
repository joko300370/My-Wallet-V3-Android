package info.blockchain.wallet.payload

import info.blockchain.wallet.bip44.HDAccount
import info.blockchain.wallet.bip44.HDWallet
import info.blockchain.wallet.bip44.HDWalletFactory
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.Derivation
import info.blockchain.wallet.payload.data.legacyXpubAddresses
import info.blockchain.wallet.payload.data.segwitXpubAddresses
import info.blockchain.wallet.stx.STXAccount
import java.lang.IllegalStateException
import java.security.SecureRandom

class HDWalletsContainer {
    private var legacy: HDWallet? = null
    private var segwitBech32: HDWallet? = null

    val seedHex
        get() = legacy?.seedHex

    val masterKey
        get() = legacy?.masterKey

    val seed
        get() = legacy?.seed

    val hdSeed
        get() = legacy?.hdSeed

    val passphrase
        get() = legacy?.passphrase

    val mnemonic: List<String>?
        get() = legacy?.mnemonic

    val isInstantiated
        get() = legacy != null

    val isDecrypted
        get() = legacy?.getAccount(0)?.xPriv != null

    fun getHDWallet(purpose: Int): HDWallet? = when (purpose) {
        Derivation.LEGACY_PURPOSE -> legacy
        Derivation.SEGWIT_BECH32_PURPOSE -> segwitBech32
        else -> null
    }

    fun addAccount(): Int {
        val legacy = legacy ?: throw HDWalletException("HDWallet not instantiated")

        val account = legacy.addAccount()
        segwitBech32?.addAccount()

        return legacy.accounts.indexOf(account)
    }

    val legacyAccounts: List<HDAccount>?
        get() = legacy?.accounts

    fun getLegacyAccount(index: Int): HDAccount? {
        return legacy?.getAccount(index)
    }

    val segwitAccounts: List<HDAccount>?
        get() = segwitBech32?.accounts

    fun getSegwitAccount(index: Int): HDAccount? {
        return segwitBech32?.getAccount(index)
    }

    fun createWallets(
        language: HDWalletFactory.Language,
        _nbWords: Int,
        _passphrase: String,
        nbAccounts: Int
    ) {
        val nbWords = if (_nbWords % 3 != 0 || _nbWords < 12 || _nbWords > 24) {
            12
        } else _nbWords

        // len == 16 (12 words), len == 24 (18 words), len == 32 (24 words)
        val len = nbWords / 3 * 4

        val passphrase = _passphrase ?: ""

        val random = SecureRandom()
        val seed = ByteArray(len)
        random.nextBytes(seed)

        legacy = HDWalletFactory.createWallet(
            language,
            passphrase,
            nbAccounts,
            Derivation.LEGACY_PURPOSE,
            seed
        )
        segwitBech32 = HDWalletFactory.createWallet(
            language,
            passphrase,
            nbAccounts,
            Derivation.SEGWIT_BECH32_PURPOSE,
            seed
        )
    }

    fun restoreWallets(
        language: HDWalletFactory.Language,
        data: String,
        passphrase: String,
        nbAccounts: Int
    ) {
        legacy = HDWalletFactory.restoreWallet(
            language,
            data,
            passphrase,
            nbAccounts,
            Derivation.LEGACY_PURPOSE
        )
        segwitBech32 = HDWalletFactory.restoreWallet(
            language,
            data,
            passphrase,
            nbAccounts,
            Derivation.SEGWIT_BECH32_PURPOSE
        )
    }

    fun restoreWatchOnly(
        accounts: List<Account>
    ) {
        val xpubsLegacy = accounts.map { it.xpubs }.legacyXpubAddresses()
        val xpubsSegwit = accounts.map { it.xpubs }.segwitXpubAddresses()

        legacy = HDWalletFactory.restoreWatchOnlyWallet(xpubsLegacy)

        if (xpubsSegwit.isNotEmpty()) {
            segwitBech32 = HDWalletFactory.restoreWatchOnlyWallet(xpubsSegwit)
        }
    }

    fun getStxAccount(): STXAccount =
        legacy?.stxAccount ?: throw IllegalStateException("Legacy account hasn't created/restored yet")
}
