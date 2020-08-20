package piuk.blockchain.android.coincore.impl

import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

abstract class OnChainTxProcessorBase(
    sendingAccount: CryptoAccount,
    sendTarget: CryptoAddress,
    exchangeRates: ExchangeRateDataManager,
    override val requireSecondPassword: Boolean
) : TransactionProcessor(
    sendingAccount,
    sendTarget,
    exchangeRates
) {
    init {
        require(sendTarget.address.isNotEmpty())
        require(sendingAccount.asset == sendTarget.asset)
    }
}
