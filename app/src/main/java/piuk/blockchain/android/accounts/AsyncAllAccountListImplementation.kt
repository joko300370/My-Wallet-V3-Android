package piuk.blockchain.android.accounts

import com.blockchain.accounts.AccountList
import com.blockchain.accounts.AsyncAllAccountList
import info.blockchain.balance.AccountReferenceList
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import io.reactivex.Single

@Deprecated("Only used in old swap")
internal class AsyncAllAccountListImplementation(
    private val cryptoAccountsMap: Map<CryptoCurrency, AccountList>
) : AsyncAllAccountList {

    override fun allAccounts(): Single<AccountReferenceList> =
        Observable.fromIterable(cryptoAccountsMap.values)
            .flatMapSingle { it.accounts() }
            .flatMapIterable { it }
            .toList()

    override fun get(cryptoCurrency: CryptoCurrency): Single<AccountList> =
        Single.just(
            cryptoAccountsMap.getOrElse(cryptoCurrency) {
                throw IllegalArgumentException("Unknown CryptoCurrency $cryptoCurrency")
            }
        )
}
