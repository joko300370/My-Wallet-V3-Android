package piuk.blockchain.android.ui.send.external

import android.content.Intent
import android.text.Editable
import android.widget.EditText
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.send.DisplayFeeOptions
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.currency.CurrencyState

internal class DuelSendPresenterX<View : ViewX>(
    private val old: SendPresenterX<View>,
    private val new: SendPresenterX<View>,
    private val currencyState: CurrencyState,
    private val stringUtils: StringUtils
) : SendPresenterXD<View>() {

    override fun getFeeOptionsForDropDown(): List<DisplayFeeOptions> {
        val regular = DisplayFeeOptions(
            stringUtils.getString(R.string.fee_options_regular),
            stringUtils.getString(R.string.fee_options_regular_time)
        )
        val priority = DisplayFeeOptions(
            stringUtils.getString(R.string.fee_options_priority),
            stringUtils.getString(R.string.fee_options_priority_time)
        )
        val custom = DisplayFeeOptions(
            stringUtils.getString(R.string.fee_options_custom),
            stringUtils.getString(R.string.fee_options_custom_warning)
        )
        return listOf(regular, priority, custom)
    }

    private fun presenter(): SendPresenterX<View> =
        when (currencyState.cryptoCurrency) {
            CryptoCurrency.BTC -> old
            CryptoCurrency.ETHER -> old
            CryptoCurrency.BCH -> old
            CryptoCurrency.XLM -> new
        }

    override fun onContinueClicked() = presenter().onContinueClicked()

    override fun onSpendMaxClicked() = presenter().onSpendMaxClicked()

    override fun onBroadcastReceived() = presenter().onBroadcastReceived()

    override fun onResume() {
        presenter().onResume()
    }

    override fun onCurrencySelected(currency: CryptoCurrency) {
        view?.setSelectedCurrency(currency)
        presenter().onCurrencySelected(currency)
    }

    override fun handleURIScan(untrimmedscanData: String?) = presenter().handleURIScan(untrimmedscanData)

    override fun handlePrivxScan(scanData: String?) = presenter().handlePrivxScan(scanData)

    override fun clearReceivingObject() = presenter().clearReceivingObject()

    override fun updateFiatTextField(editable: Editable, amountCrypto: EditText) =
        presenter().updateFiatTextField(editable, amountCrypto)

    override fun selectSendingAccount(data: Intent?, currency: CryptoCurrency) =
        presenter().selectSendingAccount(data, currency)

    override fun selectReceivingAccount(data: Intent?, currency: CryptoCurrency) =
        presenter().selectReceivingAccount(data, currency)

    override fun updateCryptoTextField(editable: Editable, amountFiat: EditText) =
        presenter().updateCryptoTextField(editable, amountFiat)

    override fun selectDefaultOrFirstFundedSendingAccount() = presenter().selectDefaultOrFirstFundedSendingAccount()

    override fun submitPayment() = presenter().submitPayment()

    override fun shouldShowAdvancedFeeWarning() = presenter().shouldShowAdvancedFeeWarning()

    override fun onCryptoTextChange(cryptoText: String) = presenter().onCryptoTextChange(cryptoText)

    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) =
        presenter().spendFromWatchOnlyBIP38(pw, scanData)

    override fun setWarnWatchOnlySpend(warn: Boolean) = presenter().setWarnWatchOnlySpend(warn)

    override fun onNoSecondPassword() = presenter().onNoSecondPassword()

    override fun onSecondPasswordValidated(validateSecondPassword: String) =
        presenter().onSecondPasswordValidated(validateSecondPassword)

    override fun disableAdvancedFeeWarning() = presenter().disableAdvancedFeeWarning()

    override fun getBitcoinFeeOptions() = presenter().getBitcoinFeeOptions()

    override fun onViewReady() {
        onCurrencySelected(currencyState.cryptoCurrency)
        view?.updateFiatCurrency(currencyState.fiatUnit)
        presenter().onViewReady()
    }

    override fun initView(view: View?) {
        super.initView(view)
        new.initView(view)
        old.initView(view)
    }
}
