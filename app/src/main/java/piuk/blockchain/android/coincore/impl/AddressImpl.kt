package piuk.blockchain.android.coincore.impl

import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.CryptoAddress

internal class ExchangeAddress(
    override val asset: CryptoCurrency,
    override val address: String,
    labels: DefaultLabels
) : CryptoAddress {
    override val label = labels.getDefaultExchangeWalletLabel(asset)
}

internal class CustodialAddress(
    override val asset: CryptoCurrency,
    override val address: String,
    labels: DefaultLabels
) : CryptoAddress {
    override val label = labels.getDefaultCustodialWalletLabel(asset)
}

internal class BitpayAddress
internal class WalletAddress
internal class ExternalAddress
