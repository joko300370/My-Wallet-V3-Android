package piuk.blockchain.android.ui.send.send2

import android.content.Intent
import android.text.Editable
import android.widget.EditText
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.data.FeeOptions
import piuk.blockchain.android.ui.send.DisplayFeeOptions
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.ui.send.external.SendPresenterX

class SendPresenter2 : SendPresenterX<SendView>() {

    override fun onContinueClicked() {
        TODO("not implemented")
    }

    override fun onSpendMaxClicked() {
        TODO("not implemented")
    }

    override fun onBroadcastReceived() {
        TODO("not implemented")
    }

    override fun onResume() {
        TODO("not implemented")
    }

    override fun onCurrencySelected(currency: CryptoCurrency) {
        when (currency) {
            CryptoCurrency.XLM -> xlmSelected()
            else -> throw IllegalArgumentException("This presented is not for $currency")
        }
    }

    private fun xlmSelected() {
        view.hideFeePriority()
        view.setFeePrioritySelection(0)
        view.disableFeeDropdown()
        view.setCryptoMaxLength(15)
    }

    override fun handleURIScan(untrimmedscanData: String?) {
        TODO("not implemented")
    }

    override fun handlePrivxScan(scanData: String?) {
        TODO("not implemented")
    }

    override fun clearReceivingObject() {
        TODO("not implemented")
    }

    override fun updateFiatTextField(editable: Editable, amountCrypto: EditText) {
        TODO("not implemented")
    }

    override fun selectSendingAccount(data: Intent?, currency: CryptoCurrency) {
        TODO("not implemented")
    }

    override fun selectReceivingAccount(data: Intent?, currency: CryptoCurrency) {
        TODO("not implemented")
    }

    override fun updateCryptoTextField(editable: Editable, amountFiat: EditText) {
        TODO("not implemented")
    }

    override fun selectDefaultOrFirstFundedSendingAccount() {
        TODO("not implemented")
    }

    override fun submitPayment() {
        TODO("not implemented")
    }

    override fun getFeeOptionsForDropDown(): List<DisplayFeeOptions> {
        TODO("not implemented")
    }

    override fun shouldShowAdvancedFeeWarning(): Boolean {
        TODO("not implemented")
    }

    override fun onCryptoTextChange(cryptoText: String) {
        TODO("not implemented")
    }

    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) {
        TODO("not implemented")
    }

    override fun setWarnWatchOnlySpend(warn: Boolean) {
        TODO("not implemented")
    }

    override fun onNoSecondPassword() {
        TODO("not implemented")
    }

    override fun onSecondPasswordValidated(validateSecondPassword: String) {
        TODO("not implemented")
    }

    override fun disableAdvancedFeeWarning() {
        TODO("not implemented")
    }

    override fun getBitcoinFeeOptions(): FeeOptions? {
        TODO("not implemented")
    }

    override fun onViewReady() {
        TODO("not implemented")
    }
}
