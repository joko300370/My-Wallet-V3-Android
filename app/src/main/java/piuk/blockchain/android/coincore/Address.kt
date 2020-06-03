package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency

interface ReceiveAddress {
    val label: String
}

abstract class CryptoAddress(
    val asset: CryptoCurrency,
    val address: String
) : ReceiveAddress

typealias AddressList = List<ReceiveAddress>

class AddressFactory(
    coincore: Coincore
) {
    // Add APIs for converting from QR codes and use entered addressed to the
    // correct bitpay and cryptoaddresses etc.
}
