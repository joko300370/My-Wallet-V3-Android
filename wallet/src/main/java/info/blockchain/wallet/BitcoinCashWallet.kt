package info.blockchain.wallet

import info.blockchain.api.BitcoinApi
import info.blockchain.wallet.bch.BchMainNetParams
import info.blockchain.wallet.crypto.DeterministicAccount
import info.blockchain.wallet.crypto.DeterministicWallet
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.keys.SigningKeyImpl
import info.blockchain.wallet.multiaddress.MultiAddressFactoryBch
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.BalanceManagerBch
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Utxo
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.bitcoinj.core.NetworkParameters
import java.math.BigInteger
import java.util.ArrayList

open class BitcoinCashWallet : DeterministicWallet {

    private lateinit var balanceManager: BalanceManagerBch
    private lateinit var multiAddressFactoryBch: MultiAddressFactoryBch

    private constructor(
        bitcoinApi: BitcoinApi,
        params: NetworkParameters,
        coinPath: String,
        passphrase: String
    ) : super(params, coinPath, MNEMONIC_LENGTH, passphrase) {
        setupApi(bitcoinApi)
    }

    private constructor(
        bitcoinApi: BitcoinApi,
        params: NetworkParameters,
        coinPath: String,
        entropyHex: String,
        passphrase: String
    ) : super(params, coinPath, entropyHex, passphrase) {
        setupApi(bitcoinApi)
    }

    private constructor(
        bitcoinApi: BitcoinApi,
        params: NetworkParameters,
        coinPath: String,
        mnemonic: List<String>,
        passphrase: String
    ) : super(params, coinPath, mnemonic, passphrase) {
        setupApi(bitcoinApi)
    }

    private constructor(bitcoinApi: BitcoinApi, params: NetworkParameters) : super(params) {
        setupApi(bitcoinApi)
    }

    private fun setupApi(bitcoinApi: BitcoinApi) {
        this.balanceManager = BalanceManagerBch(bitcoinApi)
        this.multiAddressFactoryBch = MultiAddressFactoryBch(bitcoinApi)
    }

    /**
     * Updates the state of the [BalanceManagerBch], which ingests the balances for each address or
     * xPub.
     *
     * @param importedAddressList A list of [ImportedAddress] addresses
     * @param xpubs A list of both xPubs from HD accounts and [ImportedAddress]
     * addresses
     */
    fun updateAllBalances(
        xpubs: List<XPubs>,
        importedAddressList: List<String>
    ): Completable = Completable.fromCallable {
        balanceManager.updateAllBalances(
            xpubs,
            importedAddressList
        )
    }.subscribeOn(Schedulers.io())

    /**
     * @param activeXpubs A list of active xPubs addresses.
     * @param context Xpub address. Used to fetch transaction only relating to this address.
     * @param limit Maximum amount of transactions fetched
     * @param offset Page offset
     * @return All wallet transactions, all transactions, or transaction relating to a single context/address
     */
    fun getTransactions(
        activeXpubs: List<XPubs>,
        context: List<String>?,
        limit: Int,
        offset: Int
    ): List<TransactionSummary> =
        multiAddressFactoryBch.getAccountTransactions(
            activeXpubs,
            null,
            context,
            limit,
            offset,
            BCH_FORK_HEIGHT
        )

    /**
     * Generates a Base58 Bitcoin Cash receive address for an account at a given position. The
     * address returned will be the next unused in the chain.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash receive address in Base58 format
     */
    fun getNextReceiveAddress(accountIndex: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactoryBch.getNextReceiveAddressIndex(xpub, listOf())
        return getReceiveBase58AddressAt(accountIndex, addressIndex)
    }

    /**
     * Generates a bech32 Bitcoin Cash receive address for an account at a given position. The
     * address returned will be the next unused in the chain.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash receive address in bech32 format
     */
    fun getNextReceiveCashAddress(accountIndex: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactoryBch.getNextReceiveAddressIndex(xpub, listOf())
        return getReceiveCashAddressAt(accountIndex, addressIndex)
    }

    /**
     * Generates a Base58 Bitcoin Cash change address for an account at a given position. The
     * address returned will be the next unused in the chain.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash change address in Base58 format
     */
    fun getNextChangeAddress(accountIndex: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactoryBch.getNextChangeAddressIndex(xpub)
        return getChangeBase58AddressAt(accountIndex, addressIndex)
    }

    /**
     * Generates a bech32 Bitcoin Cash change address for an account at a given position. The
     * address returned will be the next unused in the chain.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash change address in bech32 format
     */
    fun getNextChangeCashAddress(accountIndex: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactoryBch.getNextChangeAddressIndex(xpub)
        return getChangeCashAddressAt(accountIndex, addressIndex)
    }

    /**
     * Allows you to generate a receive address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @param position Represents how many positions on the chain beyond what is already used that
     *                 you wish to generate
     * @return A Bitcoin Cash receive address in Base58
     */
    fun getReceiveAddressAtPosition(accountIndex: Int, position: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactoryBch.getNextReceiveAddressIndex(xpub, listOf())
        return getReceiveBase58AddressAt(accountIndex, addressIndex + position)
    }

    /**
     * Allows you to generate a change address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @param position Represents how many positions on the chain beyond what is already used that
     *                 you wish to generate
     * @return A Bitcoin Cash change address in Base58
     */
    fun getChangeAddressAtPosition(accountIndex: Int, position: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactoryBch.getNextChangeAddressIndex(xpub)
        return getChangeBase58AddressAt(accountIndex, addressIndex + position)
    }

    /**
     * Allows you to generate a receive address from any given point on the receive chain.
     *
     * @param accountIndex The index of the account you wish to generate an address from
     * @param addressIndex What position on the chain the address you wish to create is
     * @return A Bitcoin Cash receive address in Base58 format
     */
    fun getReceiveAddressAtArbitraryPosition(accountIndex: Int, addressIndex: Int): String {
        return getReceiveBase58AddressAt(accountIndex, addressIndex)
    }

    /**
     * Allows you to generate a change address from any given point on the change chain.
     *
     * @param accountIndex The index of the account you wish to generate an address from
     * @param addressIndex What position on the chain the address you wish to create is
     * @return A Bitcoin Cash change address in Base58 format
     */
    fun getChangeAddressAtArbitraryPosition(accountIndex: Int, addressIndex: Int): String {
        return getChangeBase58AddressAt(accountIndex, addressIndex)
    }

    fun incrementNextReceiveAddress(xpub: String) {
        multiAddressFactoryBch.incrementNextReceiveAddress(xpub, listOf())
    }

    fun incrementNextChangeAddress(xpub: String) {
        multiAddressFactoryBch.incrementNextChangeAddress(xpub)
    }

    /**
     * Returns whether or not an address belongs to this wallet.
     * @param address The base58 address you want to query
     * @return
     */
    fun isOwnAddress(address: String) =
        multiAddressFactoryBch.isOwnHDAddress(address)

    /**
     * Returns an xPub from an address if the address belongs to this wallet.
     * @param address The Bitcoin Cash base58 address you want to query
     * @return An xPub as a String
     */
    fun getXpubFromAddress(address: String): String? {
        return multiAddressFactoryBch.getXpubFromAddress(address)
    }

    /**
     * Updates address balance as well as wallet balance in [BalanceManagerBch]. This is used to immediately update
     * balances after a successful transaction which speeds up the balance the UI reflects without
     * the need to wait for incoming websocket notification.
     *
     * @param amount The amount to be subtracted from the address's balance
     * @param address A valid Bitcoin cash address in base58 format
     */
    @Throws(Exception::class)
    fun subtractAmountFromAddressBalance(address: String, amount: BigInteger) {
        balanceManager.subtractAmountFromAddressBalance(address, amount)
    }

    fun getHDKeysForSigning(
        account: DeterministicAccount,
        unspentOutputs: List<Utxo>
    ): List<SigningKey> {
        if (!account.node.hasPrivKey())
            throw HDWalletException("Wallet private key unavailable. First decrypt with second password.")
        else {
            val keys = ArrayList<SigningKey>()

            for (unspentOutput in unspentOutputs) {
                unspentOutput.xpub?.derivationPath?.let { path ->
                    val split = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    val chain = Integer.parseInt(split[1])
                    val addressIndex = Integer.parseInt(split[2])
                    val address = account.chains[chain]!!.getAddressAt(addressIndex)
                    keys.add(SigningKeyImpl(address.ecKey))
                }
            }
            return keys
        }
    }

    companion object {
        /**
         * Coin parameters
         */
        const val BITCOIN_COIN_PATH = "M/44H/0H"
        const val BITCOINCASH_COIN_PATH = "M/44H/145H"
        const val MNEMONIC_LENGTH = 12
        const val BCH_FORK_HEIGHT = 478558

        /**
         * Coin metadata store
         */
        const val METADATA_TYPE_EXTERNAL = 7

        @Synchronized
        fun create(
            bitcoinApi: BitcoinApi,
            params: NetworkParameters,
            coinPath: String
        ): BitcoinCashWallet {
            return BitcoinCashWallet(bitcoinApi, params, coinPath, "")
        }

        @Synchronized
        fun create(
            bitcoinApi: BitcoinApi,
            params: NetworkParameters,
            coinPath: String,
            passphrase: String
        ): BitcoinCashWallet {
            return BitcoinCashWallet(bitcoinApi, params, coinPath, passphrase)
        }

        @Synchronized
        fun restore(
            bitcoinApi: BitcoinApi,
            params: NetworkParameters,
            coinPath: String,
            entropyHex: String,
            passphrase: String
        ): BitcoinCashWallet {
            return BitcoinCashWallet(bitcoinApi, params, coinPath, entropyHex, passphrase)
        }

        @Synchronized
        fun restore(
            bitcoinApi: BitcoinApi,
            coinPath: String,
            mnemonic: List<String>,
            passphrase: String
        ): BitcoinCashWallet {
            val params = BchMainNetParams.get()
            return BitcoinCashWallet(bitcoinApi, params, coinPath, mnemonic, passphrase)
        }

        @Synchronized
        fun createWatchOnly(
            bitcoinApi: BitcoinApi,
            params: NetworkParameters
        ): BitcoinCashWallet {
            return BitcoinCashWallet(bitcoinApi, params)
        }
    }
}
