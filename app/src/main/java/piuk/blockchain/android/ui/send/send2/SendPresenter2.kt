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
        TODO("AND-1535")
    }

    override fun onSpendMaxClicked() {
        TODO("AND-1535")
    }

    override fun onBroadcastReceived() {
        TODO("AND-1535")
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
        TODO("AND-1535")
    }

    override fun handlePrivxScan(scanData: String?) {
        TODO("AND-1535")
    }

    override fun clearReceivingObject() {
        TODO("AND-1535")
    }

    override fun selectSendingAccount(data: Intent?, currency: CryptoCurrency) {
        TODO("AND-1535")
    }

    override fun selectReceivingAccount(data: Intent?, currency: CryptoCurrency) {
        TODO("AND-1535")
    }

    override fun selectDefaultOrFirstFundedSendingAccount() {
        // Nothing to do, we have just one account on XLM
    }

    override fun submitPayment() {
        TODO("AND-1535")
    }

    override fun shouldShowAdvancedFeeWarning(): Boolean {
        TODO("AND-1535")
    }

    override fun onCryptoTextChange(cryptoText: String) {
        cryptoTextSubject.onNext(currency.withMajorValueOrZero(cryptoText))
    }

    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) {
        TODO("AND-1535")
    }

    override fun setWarnWatchOnlySpend(warn: Boolean) {
        TODO("AND-1535")
    }

    override fun onNoSecondPassword() {
        TODO("AND-1535")
    }

    override fun onSecondPasswordValidated(validateSecondPassword: String) {
        TODO("AND-1535")
    }

    override fun disableAdvancedFeeWarning() {
        TODO("AND-1535")
    }

    override fun getBitcoinFeeOptions(): FeeOptions? {
        TODO("AND-1535")
    }

    override fun onViewReady() {
    }
}
