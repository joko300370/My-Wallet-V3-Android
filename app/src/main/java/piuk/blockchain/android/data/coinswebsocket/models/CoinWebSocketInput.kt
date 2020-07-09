package piuk.blockchain.android.data.coinswebsocket.models

data class CoinWebSocketInput(
    val guid: String,
    val ethAddress: String,
    val erc20Address: String,
    val erc20PaxContractAddress: String,
    val erc20UsdtContractAddress: String,
    val receiveBtcAddresses: List<String>,
    val receiveBhcAddresses: List<String>,
    val xPubsBtc: List<String>,
    val xPubsBch: List<String>
)