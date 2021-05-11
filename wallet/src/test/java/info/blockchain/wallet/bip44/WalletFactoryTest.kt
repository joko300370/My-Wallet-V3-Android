package info.blockchain.wallet.bip44

import info.blockchain.wallet.bip44.HDWalletFactory.Language
import info.blockchain.wallet.payload.data.Derivation

import org.bitcoinj.params.MainNetParams
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

import org.spongycastle.util.encoders.Hex.toHexString
import java.security.SecureRandom

class WalletFactoryTest {

    @Test
    fun testCreateWallet() {

        val passphrase = "passphrase"
        val mnemonicLength = 12
        val path = "M/44H"

        // len == 16 (12 words), len == 24 (18 words), len == 32 (24 words)
        val len = mnemonicLength / 3 * 4
        val random = SecureRandom()
        val seed = ByteArray(len)
        random.nextBytes(seed)

        val wallet = HDWalletFactory
            .createWallet(
                Language.US,
                passphrase,
                1,
                Derivation.LEGACY_PURPOSE,
                seed
            )

        assertEquals(
            mnemonicLength.toLong(),
            wallet.mnemonic
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
                .size
                .toLong()
        )
        assertEquals(passphrase, wallet.passphrase)
        assertEquals(1, wallet.accounts.size.toLong())
        assertEquals(path, wallet.path)
    }

    @Test
    fun testRestoreWallet_mnemonic() {

        val mnemonic = "all all all all all all all all all all all all"
        val passphrase = "myPassPhrase"
        val accountListSize = 4

        val wallet = HDWalletFactory
            .restoreWallet(
                Language.US,
                mnemonic,
                passphrase,
                accountListSize,
                Derivation.LEGACY_PURPOSE
            )

        // HDWallet
        assertNotNull(wallet)
        assertEquals(
            "edb3e309910eafe85e03c9067b82a04d59e040523810c92bac3aca8252d461d5",
            wallet.masterKey.toDeterministicKey().privateKeyAsHex
        )
        assertEquals(
            "50f4cb14e1cebfccf865118487bb3a6264f51eb410894e1458a9b" +
                    "c9cb241886fbce65249611c4822dc957b2da47674dbddd02e003f0507bc757c20e835b0992f",
            toHexString(wallet.hdSeed)
        )
        assertEquals(16, wallet.seed.size.toLong())
        assertEquals("0660cc198330660cc198330660cc1983", wallet.seedHex)
        assertEquals("M/44H", wallet.path)
        assertEquals(mnemonic, wallet.mnemonic.joinToString(" "))
        assertEquals(passphrase, wallet.passphrase)

        // HDAccount
        assertEquals(accountListSize.toLong(), wallet.accounts.size.toLong())
        wallet.addAccount()
        assertEquals((accountListSize + 1).toLong(), wallet.accounts.size.toLong())
    }

    @Test
    fun testSTXAddressDerivationFromHDWallet() {
        val mnemonic =
            "one remember hint unlock finger reform utility acid speed cushion split" +
                    " client bitter myself protect actor frame forward rather better mercy clay card awesome"

        val wallet = HDWalletFactory
            .restoreWallet(
                Language.US,
                mnemonic,
                "",
                1,
                Derivation.LEGACY_PURPOSE
            )

        assertEquals(
            "1LJepqGsKKLPxFumnzFndsWTWsaCfkSDTp",
            wallet.stxAccount.bitcoinSerializedBase58Address
        )
        assertEquals(
            "M/44H/5757H/0H/0/0",
            wallet.stxAccount.address.path
        )
    }

    @Test(expected = java.lang.Exception::class)
    fun testRestoreWallet_badMnemonic_fail() {

        HDWalletFactory
            .restoreWallet(
                Language.US,
                "all all all all all all all all all all all all bogus",
                "",
                1,
                Derivation.LEGACY_PURPOSE
            )
    }

    @Test
    fun testRestoreWallet_seedHex() {

        val hexSeed = "0660cc198330660cc198330660cc1983"
        val passphrase = "myPassPhrase"
        val accountListSize = 4

        val wallet = HDWalletFactory
            .restoreWallet(
                Language.US,
                hexSeed,
                passphrase,
                accountListSize, Derivation.LEGACY_PURPOSE
            )

        assertNotNull(wallet)
        assertEquals(hexSeed, wallet.seedHex)
        assertEquals(accountListSize.toLong(), wallet.accounts.size.toLong())
        assertEquals(passphrase, wallet.passphrase)
    }

    @Test
    fun testRestoredWallet_addressChains_withSamePassphrase_shouldBeSame() {
        val passphrase1 = "passphrase1"

        val restoredWallet1 = HDWalletFactory.restoreWallet(
            Language.US,
        "all all all all all all all all all all all all",
            passphrase1,
            1,
            Derivation.LEGACY_PURPOSE
        )

        val restoredWallet2 = HDWalletFactory.restoreWallet(
            Language.US,
        "all all all all all all all all all all all all",
            passphrase1,
            1, Derivation.LEGACY_PURPOSE
        )

        assertEquals(
            restoredWallet2.getAccount(0).receive.getAddressAt(
                0, Derivation.LEGACY_PURPOSE).formattedAddress,
            restoredWallet1.getAccount(0).receive.getAddressAt(
                0, Derivation.LEGACY_PURPOSE).formattedAddress
        )

        assertEquals(
            restoredWallet2.getAccount(0).change.getAddressAt(
                0, Derivation.LEGACY_PURPOSE).formattedAddress,
            restoredWallet1.getAccount(0).change.getAddressAt(
                0, Derivation.LEGACY_PURPOSE).formattedAddress
        )
    }

    @Test
    fun testRestoredWallet_addressChains_withDifferentPassphrase_shouldBeDifferent() {

        val passphrase1 = "passphrase1"
        val passphrase2 = "passphrase2"

        val wallet1 = HDWalletFactory.restoreWallet(
            Language.US,
            "all all all all all all all all all all all all",
            passphrase1,
            1,
            Derivation.LEGACY_PURPOSE
        )

        val wallet2 = HDWalletFactory.restoreWallet(
            Language.US,
            "all all all all all all all all all all all all",
            passphrase2,
            1,
            Derivation.LEGACY_PURPOSE
        )

        Assert.assertNotEquals(
            wallet2.getAccount(0)
                .receive.getAddressAt(0, Derivation.LEGACY_PURPOSE).formattedAddress,
            wallet1.getAccount(0)
                .receive.getAddressAt(0, Derivation.LEGACY_PURPOSE).formattedAddress
        )

        Assert.assertNotEquals(
            wallet2.getAccount(0)
                .change.getAddressAt(0, Derivation.LEGACY_PURPOSE).formattedAddress,
            wallet1.getAccount(0)
                .change.getAddressAt(0, Derivation.LEGACY_PURPOSE).formattedAddress
        )
    }

    @Test
    fun testAccount() {
        var account = HDAccount(
            MainNetParams.get(),
            "xpub6CbTPgFYkRqMQZiX2WYEiVHWGJUjAsZAvSvMq3z52KczYQr" +
                    "ZPQ9DjKwHQBmAMJVY3kLeBQ4T818MBf2cTiGkJSkmS8CDT1Wp7Dw4vFMygEV",
            1
        )

        assertEquals(
            "xpub6CbTPgFYkRqMQZiX2WYEiVHWGJUjAsZAvSvMq3z52KczYQrZPQ9" +
                    "DjKwHQBmAMJVY3kLeBQ4T818MBf2cTiGkJSkmS8CDT1Wp7Dw4vFMygEV",
            account.xpub
        )
        assertEquals(1, account.id.toLong())

        account = HDAccount(MainNetParams.get(),
            "xpub6CbTPgFYkRqMQZiX2WYEiVHWGJUjAsZAvSvMq3z52KczYQrZPQ9DjKwHQ" +
                    "BmAMJVY3kLeBQ4T818MBf2cTiGkJSkmS8CDT1Wp7Dw4vFMygEV")
        assertEquals(
            "xpub6CbTPgFYkRqMQZiX2WYEiVHWGJUjAsZAvSvMq3z52KczYQrZPQ9Dj" +
                    "KwHQBmAMJVY3kLeBQ4T818MBf2cTiGkJSkmS8CDT1Wp7Dw4vFMygEV",
            account.xpub
        )
        assertEquals(0, account.id.toLong())
    }
}
