package piuk.blockchain.androidcore.data.erc20

import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.ethereum.data.Erc20AddressResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import java.math.BigInteger

abstract class Erc20BaseAccount(
    private val environmentSettings: EnvironmentConfig
) : Erc20Account {

    override val contractAddress: String
        get() = ethDataManager.getErc20TokenData(cryptoCurrency).contractAddress

    override fun fetchErc20Address(): Observable<Erc20DataModel> =
        if (environmentSettings.environment == Environment.TESTNET) {
            Observable.just(Erc20DataModel(Erc20AddressResponse(), cryptoCurrency))
                .doOnNext { dataStore.erc20DataModel = null }
        } else {
            ethDataManager.getErc20Address(cryptoCurrency)
                .map {
                    Erc20DataModel(it, cryptoCurrency)
                }.doOnNext {
                    dataStore.erc20DataModel = it
                }.subscribeOn(Schedulers.io())
        }

    override fun getTransactions(): Observable<List<Erc20Transfer>> =
        dataStore.erc20DataModel?.let { model ->
            Observable.just(model.transfers)
                .applySchedulers()
        } ?: Observable.empty()

    override fun getAccountHash(): Single<String> =
        dataStore.erc20DataModel?.let { model ->
            Single.just(model.accountHash)
                .applySchedulers()
        } ?: Single.error(IllegalStateException("erc20 uninititalised"))

    /**
     * Returns the user's ERC20 account object if previously fetched.
     *
     * @return A nullable [Erc20DataModel] object
     */
    override fun getErc20Model(): Erc20DataModel? = dataStore.erc20DataModel

    override fun fetchAddressCompletable(): Completable = Completable.fromObservable(fetchErc20Address())

    override fun getBalance(): Single<BigInteger> =
        ethDataManager.getErc20Address(cryptoCurrency)
            .map {
                it.balance
            }
            .singleOrError()

    override fun createTransaction(
        nonce: BigInteger,
        to: String,
        contractAddress: String,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        amount: BigInteger
    ): RawTransaction? =
        RawTransaction.createTransaction(
            nonce,
            gasPriceWei,
            gasLimitGwei,
            contractAddress,
            0.toBigInteger(),
            data(to, amount))

    private fun data(to: String, amount: BigInteger): String {
        val transferMethodHex = "0xa9059cbb"

        return transferMethodHex + TypeEncoder.encode(Address(to)) +
            TypeEncoder.encode(org.web3j.abi.datatypes.generated.Uint256(
                amount))
    }
}