package piuk.blockchain.android.coincore.erc20

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager

class Erc20DepositTransaction(
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    erc20Account: Erc20Account,
    feeManager: FeeDataManager,
    exchangeRates: ExchangeRateDataManager,
    sendingAccount: Erc20NonCustodialAccount,
    sendTarget: CryptoAddress,
    requireSecondPassword: Boolean
) : Erc20OnChainTransaction(
    erc20Account,
    feeManager,
    exchangeRates,
    sendingAccount,
    sendTarget,
    requireSecondPassword
) {

    override fun doInitialiseTx(): Single<PendingTx> =
        super.doInitialiseTx()
            .flatMap { pendingTx ->
                custodialWalletManager.getInterestLimits(asset).toSingle().map {
                    pendingTx.copy(minLimit = it.minDepositAmount)
                }
            }.map {
                it.copy(
                    options = setOf(
                        TxOptionValue.TxBooleanOption(
                            option = TxOption.AGREEMENT_INTEREST_T_AND_C
                        ),
                        TxOptionValue.TxBooleanOption(
                            option = TxOption.AGREEMENT_INTEREST_TRANSFER
                        )
                    )
                )
            }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        super.doValidateAmount(pendingTx)
            .map {
                val inputFiatAmount =
                    it.amount.toFiat(exchangeRates, currencyPrefs.selectedFiatCurrency)

                if (it.amount.isPositive && inputFiatAmount < it.minLimit!!) {
                    it.copy(validationState = ValidationState.MIN_REQUIRED)
                } else {
                    it
                }
            }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        super.doValidateAll(pendingTx)
            .map {
                if (it.validationState == ValidationState.CAN_EXECUTE && !areOptionsValid(
                        pendingTx)) {
                    it.copy(validationState = ValidationState.OPTION_INVALID)
                } else {
                    it
                }
            }

    private fun areOptionsValid(pendingTx: PendingTx): Boolean {
        val terms = pendingTx.getOption<TxOptionValue.TxBooleanOption>(
            TxOption.AGREEMENT_INTEREST_T_AND_C
        )?.value ?: false
        val transfer = pendingTx.getOption<TxOptionValue.TxBooleanOption>(
            TxOption.AGREEMENT_INTEREST_TRANSFER
        )?.value ?: false

        return (terms && transfer)
    }
}