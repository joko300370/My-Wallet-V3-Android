package info.blockchain.balance

interface ExchangeRates {
    fun getLastPrice(cryptoCurrency: CryptoCurrency, currencyName: String): Double
    fun getLastPriceOfFiat(targetFiat: String, sourceFiat: String): Double
}