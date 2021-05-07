package info.blockchain.wallet.payment

enum class OutputType(val size: Double) {
    P2PKH(34.0),
    P2WPKH(31.0),
    P2SH(32.0),
    P2WSH(43.0);
}