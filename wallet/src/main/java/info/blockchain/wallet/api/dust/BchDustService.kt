package info.blockchain.wallet.api.dust

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.api.dust.data.DustInput
import io.reactivex.Single
import java.util.Locale

interface DustService {

    fun getDust(cryptoCurrency: CryptoCurrency): Single<DustInput>
}

internal class BchDustService(private val api: DustApi, private val apiCode: ApiCode) : DustService {

    override fun getDust(cryptoCurrency: CryptoCurrency): Single<DustInput> =
        api.getDust(cryptoCurrency.networkTicker.toLowerCase(Locale.ENGLISH), apiCode.apiCode)
}