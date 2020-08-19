package piuk.blockchain.android.coincore.eth

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionValidationError
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager

class EthDepositTransaction(
    private val currencyPrefs: CurrencyPrefs,
    val custodialWalletManager: CustodialWalletManager,
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
                TxOptionValue.TxBooleanOption(
                    option = TxOption.AGREEMENT_WITH_LINKS
                ),
                TxOptionValue.TxBooleanOption(
                    option = TxOption.TEXT_AGREEMENT
                )
            )
        )

    override fun validate(): Completable {
        return Completable.fromCallable {
            pendingTx.getOption<TxOptionValue.TxBooleanOption>(TxOption.AGREEMENT_WITH_LINKS)
                ?.let { linksAgreement ->
                    pendingTx.getOption<TxOptionValue.TxBooleanOption>(TxOption.TEXT_AGREEMENT)
                        ?.let { textAgreement ->
                            if (linksAgreement.value && textAgreement.value) {
                                super.validate()
                            } else {
                                throw TransactionValidationError(
                                    TransactionValidationError.OPTION_MISSING)
                            }
                        }
                }
        }
    }

    override fun updateAmount(amount: CryptoValue): Single<PendingTx> =
        super.updateAmount(amount).flatMap { pendingTx ->
            custodialWalletManager.getInterestLimits(amount.currency).flatMapSingle {
                val inputFiatAmount =
                    amount.toFiat(exchangeRates, currencyPrefs.selectedFiatCurrency)
                val endpointFiatAmount =
                    FiatValue.fromMajor(it.currency, it.minDepositAmount.toBigDecimal())

                if (amount.isPositive && inputFiatAmount < endpointFiatAmount) {
                    throw TransactionValidationError(TransactionValidationError.MIN_REQUIRED)
                } else {
                    Single.just(pendingTx.copy(
                        minLimit = endpointFiatAmount
                    ))
                }
            }
        }
}