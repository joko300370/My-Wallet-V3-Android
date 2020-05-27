package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency

interface ReceiveAddress {
    val label: String
}

interface CryptoAddress : ReceiveAddress{
    val address: String
    val asset: CryptoCurrency
}

typealias AddressList = List<ReceiveAddress>