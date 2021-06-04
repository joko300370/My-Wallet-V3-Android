package info.blockchain.balance

enum class CryptoCurrency(
    val networkTicker: String,
    val displayTicker: String,
    val dp: Int,           // max decimal places; ie the quanta of this asset
    val userDp: Int,       // user decimal places
    val requiredConfirmations: Int,
    val startDateForPrice: Long, // token price start times in epoch-seconds
    private val featureFlags: Long
) {
    /**
     * NB ordering in this enum matters - it is used as the default ordering for the dashboard if the remote config enum
     * fails to load for whatever reason
     */
    BTC(
        networkTicker = "BTC",
        displayTicker = "BTC",
        dp = 8,
        userDp = 8,
        requiredConfirmations = 3,
        startDateForPrice = 1282089600L, // 2010-08-18 00:00:00 UTC
        featureFlags =
        CryptoCurrency.PRICE_CHARTING or
                CryptoCurrency.MULTI_WALLET or
                CryptoCurrency.OFFLINE_RECEIVE_ADDRESS
    ),
    ETHER(
        networkTicker = "ETH",
        displayTicker = "ETH",
        dp = 18,
        userDp = 8,
        requiredConfirmations = 12,
        startDateForPrice = 1438992000L, // 2015-08-08 00:00:00 UTC
        featureFlags =
        CryptoCurrency.PRICE_CHARTING or
                CryptoCurrency.OFFLINE_RECEIVE_ADDRESS
    ),
    BCH(
        networkTicker = "BCH",
        displayTicker = "BCH",
        dp = 8,
        userDp = 8,
        requiredConfirmations = 3,
        startDateForPrice = 1500854400L, // 2017-07-24 00:00:00 UTC
        featureFlags =
        CryptoCurrency.PRICE_CHARTING or
                CryptoCurrency.MULTI_WALLET or
                CryptoCurrency.OFFLINE_RECEIVE_ADDRESS

    ),
    XLM(
        networkTicker = "XLM",
        displayTicker = "XLM",
        dp = 7,
        userDp = 7,
        requiredConfirmations = 1,
        startDateForPrice = 1409875200L, // 2014-09-04 00:00:00 UTC
        featureFlags =
        CryptoCurrency.PRICE_CHARTING or
                CryptoCurrency.OFFLINE_RECEIVE_ADDRESS
    ),
    ALGO(
        networkTicker = "ALGO",
        displayTicker = "ALGO",
        dp = 6,
        userDp = 6,
        requiredConfirmations = 12,
        startDateForPrice = 1560985200L, // 2019-06-20 00:00:00 UTC
        featureFlags = CryptoCurrency.PRICE_CHARTING or CryptoCurrency.CUSTODIAL_ONLY
    ),
    DGLD(
        networkTicker = "WDGLD",
        displayTicker = "wDGLD",
        dp = 8,
        userDp = 8,
        requiredConfirmations = 12, // Same as ETHER
        startDateForPrice = 1576108800L, // 2019-12-12 00:00:00 UTC
        featureFlags = CryptoCurrency.PRICE_CHARTING or CryptoCurrency.IS_ERC20
    ),
    PAX(
        networkTicker = "PAX",
        displayTicker = "USD-D",
        dp = 18,
        userDp = 8,
        requiredConfirmations = 12, // Same as ETHER
        startDateForPrice = 1438992000L, // Same as ETHER
        featureFlags = CryptoCurrency.IS_ERC20 or
                CryptoCurrency.OFFLINE_RECEIVE_ADDRESS
    ),
    USDT(
        networkTicker = "USDT",
        displayTicker = "USDT",
        dp = 6,
        userDp = 6,
        requiredConfirmations = 12, // Same as ETHER
        startDateForPrice = 1438992000L, // Same as ETHER
        featureFlags = CryptoCurrency.IS_ERC20
    ),
    STX(
        networkTicker = "STX",
        displayTicker = "STX",
        dp = 7,
        userDp = 7,
        requiredConfirmations = 12,
        startDateForPrice = 0,
        featureFlags =
        CryptoCurrency.STUB_ASSET
    ),
    AAVE(
        networkTicker = "AAVE",
        displayTicker = "AAVE",
        dp = 18,
        userDp = 8,
        requiredConfirmations = 12, // Same as ETHER
        startDateForPrice = 1615831200L, // 2021-03-15 00:00:00 UTC
        featureFlags = CryptoCurrency.PRICE_CHARTING or CryptoCurrency.IS_ERC20
    ),
    YFI(
        networkTicker = "YFI",
        displayTicker = "YFI",
        dp = 18,
        userDp = 8,
        requiredConfirmations = 12, // Same as ETHER
        startDateForPrice = 1615831200L, // Same as AAVE
        featureFlags = CryptoCurrency.PRICE_CHARTING or CryptoCurrency.IS_ERC20
    ),
    DOT(
        networkTicker = "DOT",
        displayTicker = "DOT",
        dp = 10,
        userDp = 10,
        requiredConfirmations = 12,
        startDateForPrice = 1615831200L, // Same as AAVE
        featureFlags = CryptoCurrency.PRICE_CHARTING or CryptoCurrency.CUSTODIAL_ONLY
    );

    fun hasFeature(feature: Long): Boolean = (0L != (featureFlags and feature))

    companion object {
        fun fromNetworkTicker(symbol: String?): CryptoCurrency? =
            values().firstOrNull { it.networkTicker.equals(symbol, ignoreCase = true) }

        @Deprecated("Historical accessibility helper",
            ReplaceWith("Coincore (cryptoAssets, fiatAssets, allAssets)")
        )
        fun activeCurrencies(): List<CryptoCurrency> = values().filter {
            !it.hasFeature(STUB_ASSET)
        }

        fun erc20Assets(): List<CryptoCurrency> = values().filter {
            it.hasFeature(IS_ERC20)
        }

        @Deprecated("Temporary fix")
        fun swipeToReceiveAssets(): List<CryptoCurrency> = values().filter {
            it.hasFeature(OFFLINE_RECEIVE_ADDRESS)
        }

        const val PRICE_CHARTING = 0x00000001L
        const val MULTI_WALLET = 0x00000002L
        const val CUSTODIAL_ONLY = 0x0000004L
        const val IS_ERC20 = 0x00000008L
        const val CUSTODIAL_MEMO = 0x00000010L

        const val STUB_ASSET = 0x10000000L

        // TEMP Crash workaround until swipe to receive is updated to use coincore
        const val OFFLINE_RECEIVE_ADDRESS = 0x20000000L
    }
}