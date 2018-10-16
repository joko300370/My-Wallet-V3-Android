package piuk.blockchain.android.ui.send.send2

import android.content.Intent
import android.text.Editable
import android.widget.EditText
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.withMajorValueOrZero
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.ui.send.external.SendPresenterX
import piuk.blockchain.androidcore.data.currency.CurrencyState

class SendPresenter2(currencyState: CurrencyState) : SendPresenterX<SendView>() {

    private val currency: CryptoCurrency by lazy { currencyState.cryptoCurrency }
    private var cryptoTextSubject = PublishSubject.create<CryptoValue>()

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
        // Nothing to do, we have just one account on XLM
    }

    override fun submitPayment() {
        TODO("not implemented")
    }

    override fun shouldShowAdvancedFeeWarning(): Boolean {
        TODO("not implemented")
    }

    override fun onCryptoTextChange(cryptoText: String) {
        cryptoTextSubject.onNext(currency.withMajorValueOrZero(cryptoText))
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
    }
}
