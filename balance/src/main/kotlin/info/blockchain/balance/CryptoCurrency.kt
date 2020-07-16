package info.blockchain.balance

enum class CryptoCurrency(
    val networkTicker: String,
    val displayTicker: String,
    val dp: Int,           // max decimal places; ie the quanta of this asset
    val userDp: Int,       // user decimal places
    val requiredConfirmations: Int,
    private val featureFlags: Long
) {
    BTC(
        networkTicker = "BTC",
        displayTicker = "BTC",
        dp = 8,
        userDp = 8,
        requiredConfirmations = 3,
        featureFlags =
            CryptoCurrency.PRICE_CHARTING or
            CryptoCurrency.MULTI_WALLET

    ),
    ETHER(
        networkTicker = "ETH",
        displayTicker = "ETH",
        dp = 18,
        userDp = 8,
        requiredConfirmations = 12,
        featureFlags =
            CryptoCurrency.PRICE_CHARTING
    ),
    BCH(
        networkTicker = "BCH",
        displayTicker = "BCH",
        dp = 8,
        userDp = 8,
        requiredConfirmations = 3,
        featureFlags =
            CryptoCurrency.PRICE_CHARTING or
                    CryptoCurrency.MULTI_WALLET
    ),
    XLM(
        networkTicker = "XLM",
        displayTicker = "XLM",
        dp = 7,
        userDp = 7,
        requiredConfirmations = 1,
        featureFlags =
            CryptoCurrency.PRICE_CHARTING
    ),
    PAX(
        networkTicker = "PAX",
        displayTicker = "USD-D",
        dp = 18,
        userDp = 8,
        requiredConfirmations = 12, // Same as ETHER
        featureFlags = 0L
    ),
    STX(
        networkTicker = "STX",
        displayTicker = "STX",
        dp = 7,
        userDp = 7,
        requiredConfirmations = 12,
        featureFlags =
            CryptoCurrency.STUB_ASSET
    ),
    ALGO(
        networkTicker = "ALGO",
        displayTicker = "ALGO",
        dp = 6,
        userDp = 6,
        requiredConfirmations = 12,
        featureFlags = CryptoCurrency.PRICE_CHARTING
    ),
    USDT(
        networkTicker = "USDT",
        displayTicker = "USDT",
        dp = 6,
        userDp = 6,
        requiredConfirmations = 12,
        featureFlags = 0L
    );

    fun hasFeature(feature: Long): Boolean = (0L != (featureFlags and feature))

    val defaultSwapTo: CryptoCurrency
        get() = when (this) {
            BTC -> ETHER
            else -> BTC
        }

    companion object {
        fun fromNetworkTicker(symbol: String?): CryptoCurrency? =
            values().firstOrNull { it.networkTicker.equals(symbol, ignoreCase = true) }

        fun activeCurrencies(): List<CryptoCurrency> = values().filter { !it.hasFeature(STUB_ASSET) }

        const val PRICE_CHARTING = 0x00000001L
        const val MULTI_WALLET = 0x00000002L
        const val STUB_ASSET = 0x10000000L
    }
}