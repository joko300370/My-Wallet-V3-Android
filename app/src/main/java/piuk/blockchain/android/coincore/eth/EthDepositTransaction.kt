package piuk.blockchain.android.coincore.eth

import info.blockchain.balance.CryptoValue
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager

class EthDepositTransaction(
    ethDataManager: EthDataManager,
    feeManager: FeeDataManager,
    exchangeRates: ExchangeRateDataManager,
    sendingAccount: CryptoAccount,
    sendTarget: CryptoAddress,
    requireSecondPassword: Boolean
) : EthSendTransaction(
    ethDataManager,
        feeManager,
        exchangeRates,
        sendingAccount,
        sendTarget,
        requireSecondPassword
) {
    override var pendingTx: PendingTx =
        PendingTx(
            amount = CryptoValue.ZeroEth,
            available = CryptoValue.ZeroEth,
            fees = CryptoValue.ZeroEth,
            feeLevel = FeeLevel.Regular,
            options = setOf(
                TxOptionValue.TxTextOption(
                    option = TxOption.AGREEMENT
                )
            )
        )
}