package piuk.blockchain.androidcore.data.erc20

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.ethereum.data.Erc20AddressResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.erc20.datastores.Erc20DataStore
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import java.math.BigInteger

@Deprecated("This need to go before we can merge the ERC20 modules")
interface Erc20Account {

    val cryptoCurrency: CryptoCurrency

    val ethDataManager: EthDataManager

    val dataStore: Erc20DataStore

    val contractAddress: String

    fun clear() = dataStore.clearData()
    /**
     * Returns an [Erc20AddressResponse] object for a given ERC20 address as an [Observable]. An
     * [Erc20DataModel] contains a list of transactions associated with the account, as well
     * as a final balance. Calling this function also caches the [Erc20DataModel].
     *
     * @return An [Observable] wrapping an [Erc20DataModel]
     */

    fun fetchErc20Address(): Observable<Erc20DataModel>

    fun getTransactions(): Observable<List<Erc20Transfer>>

    fun getAccountHash(): Single<String>

    fun getErc20Model(): Erc20DataModel?

    fun fetchAddressCompletable(): Completable

    fun getBalance(): Single<BigInteger>

    fun createTransaction(
        nonce: BigInteger,
        to: String,
        contractAddress: String,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        amount: BigInteger
    ): RawTransaction?
}