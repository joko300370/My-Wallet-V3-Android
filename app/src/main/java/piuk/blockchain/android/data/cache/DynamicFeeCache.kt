package piuk.blockchain.android.data.cache

import info.blockchain.wallet.api.data.FeeOptions

@Deprecated("No longer used after new send")
class DynamicFeeCache {
    var btcFeeOptions: FeeOptions? = null
    var ethFeeOptions: FeeOptions? = null
    var bchFeeOptions: FeeOptions? = null
}
