package piuk.blockchain.androidcore.data.payload

import com.blockchain.annotations.MoveCandidate
import info.blockchain.api.BitcoinApi
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.bip44.HDWalletFactory
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.keys.MasterKey
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.Derivation
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Balance
import info.blockchain.wallet.payment.OutputType
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.stx.STXAccount
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.Exceptions
import io.reactivex.schedulers.Schedulers
import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import piuk.blockchain.androidcore.data.metadata.MetadataCredentials
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import piuk.blockchain.androidcore.utils.RefreshUpdater
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import java.math.BigInteger
import java.util.LinkedHashMap

class PayloadDataManager internal constructor(
    private val payloadService: PayloadService,
    private val bitcoinApi: BitcoinApi,
    @MoveCandidate("Move this down to the PayloadManager layer, with the other crypto tools")
    private val privateKeyFactory: PrivateKeyFactory,
    private val payloadManager: PayloadManager,
    rxBus: RxBus
) {

    private val rxPinning: RxPinning = RxPinning(rxBus)

    val metadataCredentials: MetadataCredentials?
        get() = tempPassword?.let {
            MetadataCredentials(guid, sharedKey, it)
        }

    // /////////////////////////////////////////////////////////////////////////
    // CONVENIENCE METHODS AND PROPERTIES
    // /////////////////////////////////////////////////////////////////////////

    val accounts: List<Account>
        get() = wallet?.walletBody?.accounts ?: emptyList()

    val accountCount: Int
        get() = wallet?.walletBody?.accounts?.size ?: 0

    var importedAddresses: List<ImportedAddress>
        get() = wallet?.importedAddressList?.filter { !it.isWatchOnly() } ?: emptyList()
        set(addresses) {
            wallet!!.importedAddressList = addresses
        }

    val importedAddressStringList: List<String>
        get() = wallet?.importedAddressStringList ?: emptyList()

    val wallet: Wallet?
        get() = payloadManager.payload

    val defaultAccountIndex: Int
        get() = wallet?.walletBody?.defaultAccountIdx ?: 0

    val defaultAccount: Account
        get() = wallet!!.walletBody?.getAccount(defaultAccountIndex) ?: throw NoSuchElementException()

    val payloadChecksum: String?
        get() = payloadManager.payloadChecksum

    // Can be null if the user is not currently logged in and has no pin set
    var tempPassword: String?
        get() = payloadManager.tempPassword
        set(password) {
            payloadManager.tempPassword = password
        }

    val importedAddressesBalance: BigInteger
        get() = payloadManager.importedAddressesBalance

    val isDoubleEncrypted: Boolean
        get() = wallet!!.isDoubleEncryption

    val stxAccount: STXAccount
        get() {
            val hdWallet = payloadManager.payload?.walletBody
                ?: throw IllegalStateException("Wallet not available")

            return hdWallet.stxAccount
                ?: throw IllegalStateException("Wallet not available")
        }

    val isBackedUp: Boolean
        get() = payloadManager.isWalletBackedUp

    val mnemonic: List<String>
        get() = payloadManager.payload!!.walletBody?.mnemonic ?: throw NoSuchElementException()

    val guid: String
        get() = wallet!!.guid

    val sharedKey: String
        get() = wallet!!.sharedKey

    val masterKey: MasterKey
        get() = payloadManager.masterKey()

    val isWalletUpgradeRequired: Boolean
        get() = payloadManager.isV3UpgradeRequired || payloadManager.isV4UpgradeRequired

    // /////////////////////////////////////////////////////////////////////////
    // AUTH METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Decrypts and initializes a wallet from a payload String. Handles both V3 and V1 wallets. Will
     * return a [DecryptionException] if the password is incorrect, otherwise can return a
     * [HDWalletException] which should be regarded as fatal.
     *
     * @param payload The payload String to be decrypted
     * @param password The user's password
     * @return A [Completable] object
     */
    fun initializeFromPayload(payload: String, password: String): Completable =
        rxPinning.call {
            payloadService.initializeFromPayload(payload, password)
        }.applySchedulers()

    /**
     * Restores a HD wallet from a 12 word mnemonic and initializes the [PayloadDataManager].
     * Also creates a new Blockchain.info account in the process.
     *
     * @param mnemonic The 12 word mnemonic supplied as a String of words separated by whitespace
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email The user's email address, preferably not associated with another account
     * @param password The user's choice of password
     * @return An [Observable] wrapping a [Wallet] object
     */
    fun restoreHdWallet(
        mnemonic: String,
        walletName: String,
        email: String,
        password: String
    ): Single<Wallet> = rxPinning.callSingle {
            payloadService.restoreHdWallet(
                mnemonic,
                walletName,
                email,
                password
            )
        }.applySchedulers()

    /**
     * Retrieves a  master key from a 12 word mnemonic
     */
    fun generateMasterKeyFromSeed(
        recoveryPhrase: String
    ): MasterKey = HDWalletFactory.restoreWallet(
        HDWalletFactory.Language.US,
        recoveryPhrase,
        "",
        1,
        // masterKey is independent from the derivation purpose
        Derivation.SEGWIT_BECH32_PURPOSE
    ).masterKey

    /**
     * Creates a new HD wallet and Blockchain.info account.
     *
     * @param password The user's choice of password
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email The user's email address, preferably not associated with another account
     * @return An [Observable] wrapping a [Wallet] object
     */
    fun createHdWallet(
        password: String,
        walletName: String,
        email: String
    ): Single<Wallet> = rxPinning.callSingle {
        payloadService.createHdWallet(password, walletName, email)
    }.applySchedulers()

    /**
     * Fetches the user's wallet payload, and then initializes and decrypts a payload using the
     * user's  password.
     *
     * @param sharedKey The shared key as a String
     * @param guid The user's GUID
     * @param password The user's password
     * @return A [Completable] object
     */
    fun initializeAndDecrypt(sharedKey: String, guid: String, password: String): Completable =
        rxPinning.call {
            payloadService.initializeAndDecrypt(sharedKey, guid, password)
        }.applySchedulers()

    /**
     * Initializes and decrypts a user's payload given valid QR code scan data.
     *
     * @param data A QR's URI for pairing
     * @return A [Completable] object
     */
    fun handleQrCode(data: String): Completable =
        rxPinning.call { payloadService.handleQrCode(data) }
            .applySchedulers()

    /**
     * Upgrades a Wallet from V2 to V3 and saves it with the server. If saving is unsuccessful or
     * some other part fails, this will propagate an Exception.
     *
     * @param secondPassword An optional second password if the user has one
     * @param defaultAccountName A required name for the default account
     * @return A [Completable] object
     */
    fun upgradeWalletPayload(secondPassword: String?, defaultAccountName: String): Completable =
        rxPinning.call {
            Completable.fromCallable {
                if (payloadManager.isV3UpgradeRequired) {
                    if (!payloadManager.upgradeV2PayloadToV3(secondPassword, defaultAccountName)) {
                        throw Exceptions.propagate(Throwable("Upgrade wallet failed"))
                    }
                }
                if (payloadManager.isV4UpgradeRequired) {
                    if (!payloadManager.upgradeV3PayloadToV4(secondPassword)) {
                        throw Exceptions.propagate(Throwable("Upgrade wallet failed"))
                    }
                }
            }
        }.applySchedulers()

    // /////////////////////////////////////////////////////////////////////////
    // SYNC METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns a [Completable] which saves the current payload to the server.
     *
     * @return A [Completable] object
     */
    fun syncPayloadWithServer(): Completable =
        rxPinning.call { payloadService.syncPayloadWithServer() }
            .applySchedulers()

    /**
     * Returns a [Completable] which saves the current payload to the server whilst also
     * forcing the sync of the user's public keys. This method generates 20 addresses per Account,
     * so it should be used only when strictly necessary (for instance, after enabling
     * notifications).
     *
     * @return A [Completable] object
     */
    fun syncPayloadAndPublicKeys(): Completable =
        rxPinning.call { payloadService.syncPayloadAndPublicKeys() }
            .applySchedulers()

    // /////////////////////////////////////////////////////////////////////////
    // TRANSACTION METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns [Completable] which updates transactions in the PayloadManager.
     * Completable returns no value, and is used to call functions that return void but have side
     * effects.
     *
     * @return A [Completable] object
     */
    fun updateAllTransactions(): Completable =
        rxPinning.call { payloadService.updateAllTransactions() }
            .applySchedulers()

    /**
     * Returns a [Completable] which updates all balances in the PayloadManager. Completable
     * returns no value, and is used to call functions that return void but have side effects.
     *
     * @return A [Completable] object
     */
    fun updateAllBalances(): Completable =
        rxPinning.call { payloadService.updateAllBalances() }
            .applySchedulers()

    /**
     * Update notes for a specific transaction hash and then sync the payload to the server
     *
     * @param transactionHash The hash of the transaction to be updated
     * @param notes Transaction notes
     * @return A [Completable] object
     */
    fun updateTransactionNotes(transactionHash: String, notes: String): Completable =
        rxPinning.call { payloadService.updateTransactionNotes(transactionHash, notes) }
            .applySchedulers()

    // /////////////////////////////////////////////////////////////////////////
    // ACCOUNTS AND ADDRESS METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns a [LinkedHashMap] of [Balance] objects keyed to their Bitcoin Cash
     * addresses.
     *
     * @param xpubs A List of Bitcoin cash accounts
     * @return A [LinkedHashMap]
     */
    fun getBalanceOfBchAccounts(
        xpubs: List<XPubs>
    ): Observable<Map<String, Balance>> =
        rxPinning.call<Map<String, Balance>> {
            payloadService.getBalanceOfBchAccounts(xpubs)
        }.applySchedulers()

    /**
     * Converts any address to a label.
     *
     * @param address Accepts account receive or change chain address, as well as imported address.
     * @return Either the label associated with the address, or the original address
     */
    fun addressToLabel(address: String): String = payloadManager.getLabelFromAddress(address)

    /**
     * Returns the next Receive address for a given account index.
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextReceiveAddress(accountIndex: Int): Observable<String> {
        val account = accounts[accountIndex]
        return getNextReceiveAddress(account)
    }

    /**
     * Returns the next Receive address for a given [Account]
     *
     * @param account The [Account] for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextReceiveAddress(account: Account): Observable<String> =
        Observable.fromCallable {
            payloadManager.getNextReceiveAddress(
                account
            )
        }.subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())

    /**
     * Allows you to generate a receive address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param account The [Account] you wish to generate an address from
     * @param position Represents how many positions on the chain beyond what is already used that
     * you wish to generate
     * @return A bitcoin address
     */
    fun getReceiveAddressAtPosition(account: Account, position: Int): String? =
        payloadManager.getReceiveAddressAtPosition(
            account,
            position
        )

    /**
     * Returns the next Receive address for a given [Account]
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @param label Label used to reserve address
     * @return An [Observable] wrapping the receive address
     */
    fun getNextReceiveAddressAndReserve(accountIndex: Int, label: String): Observable<String> {
        val account = accounts[accountIndex]
        return Observable.fromCallable {
            payloadManager.getNextReceiveAddressAndReserve(
                account,
                label
            )
        }.subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Returns the next Change address for a given account index.
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextChangeAddress(accountIndex: Int): Observable<String> {
        val account = accounts[accountIndex]
        return getNextChangeAddress(account)
    }

    /**
     * Returns the next Change address for a given [Account].
     *
     * @param account The [Account] for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextChangeAddress(account: Account): Observable<String> =
        Observable.fromCallable {
            payloadManager.getNextChangeAddress(
                account
            )
        }.subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())

    /**
     * Returns an [SigningKey] for a given [ImportedAddress], optionally with a second password
     * should the private key be encrypted.
     *
     * @param importedAddress The [ImportedAddress] to generate an Elliptic Curve Key for
     * @param secondPassword An optional second password, necessary if the private key is encrypted
     * @return An Elliptic Curve Key object [SigningKey]
     * @see ImportedAddress.isPrivateKeyEncrypted
     */
    fun getAddressSigningKey(importedAddress: ImportedAddress, secondPassword: String?): SigningKey? =
        payloadManager.getAddressSigningKey(importedAddress, secondPassword)

    /**
     * Derives new [Account] from the master seed
     *
     * @param accountLabel A label for the account
     * @param secondPassword An optional double encryption password
     * @return An [Observable] wrapping the newly created Account
     */
    fun createNewAccount(accountLabel: String, secondPassword: String?): Observable<Account> =
        rxPinning.call<Account> {
            payloadService.createNewAccount(accountLabel, secondPassword)
        }.applySchedulers()

    /**
     * Add a private key for a [ImportedAddress]
     *
     * @param key An [SigningKey]
     * @param secondPassword An optional double encryption password
     * @return An [Observable] representing a successful save
     */
    fun addImportedAddressFromKey(key: SigningKey, secondPassword: String?): Single<ImportedAddress> =
        rxPinning.call<ImportedAddress> {
            payloadService.setKeyForImportedAddress(key, secondPassword)
        }.applySchedulers()
            .singleOrError()

    /**
     * Allows you to propagate changes to a [ImportedAddress] through the [Wallet]
     *
     * @param importedAddress The updated address
     * @return A [Completable] object representing a successful save
     */
    fun updateImportedAddress(importedAddress: ImportedAddress): Completable =
        rxPinning.call { payloadService.updateImportedAddress(importedAddress) }
            .applySchedulers()

    /**
     * Returns an Elliptic Curve key for a given private key
     *
     * @param keyFormat The format of the private key
     * @param keyData The private key from which to derive the SigningKey
     * @return An [SigningKey]
     * @see PrivateKeyFactory
     */
    fun getKeyFromImportedData(keyFormat: String, keyData: String): Single<SigningKey> =
        Single.fromCallable {
            privateKeyFactory.getKeyFromImportedData(keyFormat, keyData, bitcoinApi)
        }.applySchedulers()

    fun getBip38KeyFromImportedData(keyData: String, keyPassword: String): Single<SigningKey> =
        Single.fromCallable {
            privateKeyFactory.getBip38Key(keyData, keyPassword)
        }.applySchedulers()

    /**
     * Returns the balance of an address. If the address isn't found in the address map object, the
     * method will return CryptoValue.Zero(Btc) instead of a null object.
     *
     * @param xpub The address whose balance you wish to query
     * @return A [CryptoValue] representing the total funds in the address
     */

    fun getAddressBalance(xpub: XPubs): CryptoValue =
        payloadManager.getAddressBalance(xpub)

    // Update if timeout of forceRefresh, get the balance - pull the code from ActivityCache/BtcCoinLikeToken
    private val balanceUpdater = RefreshUpdater<CryptoValue>(
        fnRefresh = { updateAllBalances() },
        refreshInterval = BALANCE_REFRESH_INTERVAL
    )

    fun getAddressBalanceRefresh(
        address: XPubs,
        forceRefresh: Boolean = false
    ): Single<CryptoValue> =
        balanceUpdater.get(
            fnFetch = { getAddressBalance(address) },
            force = forceRefresh
        )

    /**
     * Updates the balance of the address as well as that of the entire wallet. To be called after a
     * successful sweep to ensure that balances are displayed correctly before syncing the wallet.
     *
     * @param address An address from which you've just spent funds
     * @param spentAmount The spent amount as a long
     * @throws Exception Thrown if the address isn't found
     */
    fun subtractAmountFromAddressBalance(address: String, spentAmount: Long) {
        payloadManager.subtractAmountFromAddressBalance(address, BigInteger.valueOf(spentAmount))
    }

    /**
     * Increments the index on the receive chain for an [Account] object.
     *
     * @param account The [Account] you wish to increment
     */
    fun incrementReceiveAddress(account: Account) {
        payloadManager.incrementNextReceiveAddress(account)
    }

    /**
     * Increments the index on the change chain for an [Account] object.
     *
     * @param account The [Account] you wish to increment
     */
    fun incrementChangeAddress(account: Account) {
        payloadManager.incrementNextChangeAddress(account)
    }

    /**
     * Returns an xPub from an address if the address belongs to this wallet.
     *
     * @param address The address you want to query as a String
     * @return An xPub as a String
     */
    fun getXpubFromAddress(address: String): String? = payloadManager.getXpubFromAddress(address)

    /**
     * Returns true if the supplied address belongs to the user's wallet.
     *
     * @param address The address you want to query as a String
     * @return true if the address belongs to the user
     */
    fun isOwnHDAddress(address: String): Boolean = payloadManager.isOwnHDAddress(address)

    // /////////////////////////////////////////////////////////////////////////
    // CONTACTS/METADATA/IWCS/CRYPTO-MATRIX METHODS
    // /////////////////////////////////////////////////////////////////////////

    fun getAccount(accountPosition: Int): Account =
        wallet!!.walletBody?.getAccount(accountPosition) ?: throw NoSuchElementException()

    fun getAccountTransactions(xpub: String?, limit: Int, offset: Int):
        Single<List<TransactionSummary>> =
            Single.fromCallable {
                payloadManager.getAccountTransactions(xpub, limit, offset)
            }

    /**
     * Returns the transaction notes for a given transaction hash. May return null if not found.
     *
     * @param txHash The Tx hash
     * @return A string representing the Tx note, which can be null
     */
    fun getTransactionNotes(txHash: String): String? = payloadManager.payload!!.txNotes[txHash]

    /**
     * Returns a list of [SigningKey] objects for signing transactions.
     *
     * @param account The [Account] that you wish to send funds from
     * @param unspentOutputBundle A [SpendableUnspentOutputs] bundle for a given Account
     * @return A list of [SigningKey] objects
     */
    fun getHDKeysForSigning(
        account: Account,
        unspentOutputBundle: SpendableUnspentOutputs
    ): List<SigningKey> =
        wallet!!.walletBody?.getHDKeysForSigning(
            account,
            unspentOutputBundle
        ) ?: throw NoSuchElementException()

    // /////////////////////////////////////////////////////////////////////////
    // HELPER METHODS
    // /////////////////////////////////////////////////////////////////////////

    fun setDefaultIndex(defaultIndex: Int) {
        wallet!!.walletBody?.defaultAccountIdx = defaultIndex
    }

    fun validateSecondPassword(secondPassword: String?): Boolean =
        payloadManager.validateSecondPassword(secondPassword)

    fun decryptHDWallet(secondPassword: String?) {
        payloadManager.payload!!.decryptHDWallet(secondPassword)
    }

    fun getXpubFormatOutputType(format: XPub.Format): OutputType {
        return when (format == XPub.Format.SEGWIT) {
            true -> OutputType.P2WPKH
            else -> OutputType.P2PKH
        }
    }

    fun getAddressOutputType(address: String): OutputType {
        val networkParam = MainNetParams.get()

        // Fallback to legacy type for fee calculation
        return getSegwitOutputTypeFromAddress(address, networkParam)
            ?: getLegacyOutputTypeFromAddress(address, networkParam)
            ?: OutputType.P2PKH
    }

    private fun getSegwitOutputTypeFromAddress(address: String, networkParam: NetworkParameters): OutputType? {
        return try {
            // `SegwitAddress.getOutputScriptType()` returns either P2WPKH or P2WSH
            val segwitAddress = SegwitAddress.fromBech32(networkParam, address)
            when (segwitAddress.outputScriptType == Script.ScriptType.P2WSH) {
                true -> OutputType.P2WSH
                else -> OutputType.P2WPKH
            }
        } catch (ignored: AddressFormatException) {
            null
        }
    }

    private fun getLegacyOutputTypeFromAddress(address: String, networkParam: NetworkParameters): OutputType? {
        return try {
            val legacyAddress = LegacyAddress.fromBase58(networkParam, address)
            when (legacyAddress.p2sh) {
                true -> OutputType.P2SH
                else -> OutputType.P2PKH
            }
        } catch (ignored: AddressFormatException) {
            null
        }
    }

    companion object {
        private const val BALANCE_REFRESH_INTERVAL = 15 * 1000L
    }
}

private fun ImportedAddress.isWatchOnly() = privateKey == null