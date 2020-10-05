package piuk.blockchain.android.ui.backup.transfer

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.PendingTransaction
import piuk.blockchain.android.ui.account.TransferFundsDataManager
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.android.ui.chooser.WalletAccountHelper
import piuk.blockchain.androidcore.data.events.PayloadSyncedEvent
import piuk.blockchain.androidcore.data.events.PaymentFailedEvent
import piuk.blockchain.androidcore.data.events.PaymentSentEvent
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

class ConfirmFundsTransferPresenter(
    private val walletAccountHelper: WalletAccountHelper,
    private val fundsDataManager: TransferFundsDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val stringUtils: StringUtils,
    private val exchangeRates: ExchangeRateDataManager,
    currencyPrefs: CurrencyPrefs
) : BasePresenter<ConfirmFundsTransferView>() {

    @VisibleForTesting
    internal val pendingTransactions: MutableList<PendingTransaction> = mutableListOf()

    private val userFiat: String = currencyPrefs.selectedFiatCurrency

    override fun onViewReady() {
        updateToAddress(payloadDataManager.defaultAccountIndex)
    }

    internal fun accountSelected(position: Int) {
        updateToAddress(payloadDataManager.getPositionOfAccountFromActiveList(position))
    }

    /**
     * Transacts all [PendingTransaction] objects
     *
     * @param secondPassword The user's double encryption password if necessary
     */
    @SuppressLint("VisibleForTests", "CheckResult")
    internal fun sendPayment(secondPassword: String?) {
        val archiveAll = view.getIfArchiveChecked()

        fundsDataManager.sendPayment(pendingTransactions, secondPassword)
            .doOnSubscribe {
                view.setPaymentButtonEnabled(false)
                view.showProgressDialog()
            }
            .addToCompositeDisposable(this)
            .doOnTerminate { view.hideProgressDialog() }
            .subscribe({
                view.showToast(R.string.transfer_confirmed, ToastCustom.TYPE_OK)
                if (archiveAll) {
                    archiveAll()
                } else {
                    view.dismissDialog()
                    view.sendBroadcast(PaymentSentEvent())
                }
            }, {
                view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                view.dismissDialog()
                view.sendBroadcast(PaymentFailedEvent())
            })
    }

    /**
     * Returns only HD Accounts as we want to move funds to a backed up place
     *
     * @return A [List] of [ItemAccount] objects
     */
    internal fun getReceiveToList() = walletAccountHelper.getHdAccounts()

    /**
     * Get corrected default account position
     *
     * @return int account position in list of non-archived accounts
     */
    internal fun getDefaultAccount() = Math.max(
        payloadDataManager.getPositionOfAccountInActiveList(payloadDataManager.defaultAccountIndex),
        0
    )

    @VisibleForTesting
    internal fun updateUi(totalToSend: CryptoValue, totalFee: CryptoValue) {
        view.updateFromLabel(
            stringUtils.getQuantityString(
                R.plurals.transfer_label_plural,
                pendingTransactions.size
            )
        )

        val fiatAmount = totalToSend.toFiat(exchangeRates, userFiat).toStringWithSymbol()
        val fiatFee = totalFee.toFiat(exchangeRates, userFiat).toStringWithSymbol()

        view.updateTransferAmountBtc(
            totalToSend.toStringWithSymbol()
        )
        view.updateTransferAmountFiat(fiatAmount)
        view.updateFeeAmountBtc(totalFee.toStringWithSymbol())
        view.updateFeeAmountFiat(fiatFee)
        view.setPaymentButtonEnabled(true)

        view.onUiUpdated()
    }

    @SuppressLint("CheckResult")
    @VisibleForTesting
    internal fun archiveAll() {
        for (spend in pendingTransactions) {
            (spend.sendingObject!!.accountObject as LegacyAddress).tag =
                LegacyAddress.ARCHIVED_ADDRESS
        }

        payloadDataManager.syncPayloadWithServer()
            .doOnSubscribe { view.showProgressDialog() }
            .addToCompositeDisposable(this)
            .doOnTerminate {
                view.hideProgressDialog()
                view.dismissDialog()
                view.sendBroadcast(PayloadSyncedEvent())
            }
            .subscribe(
                { view.showToast(R.string.transfer_archive, ToastCustom.TYPE_OK) },
                { view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) })
    }

    @SuppressLint("VisibleForTests", "CheckResult")
    private fun updateToAddress(indexOfReceiveAccount: Int) {
        fundsDataManager.getTransferableFundTransactionList(indexOfReceiveAccount)
            .doOnSubscribe { view.setPaymentButtonEnabled(false) }
            .addToCompositeDisposable(this)
            .subscribeBy(
                onNext = { (pendingList, totalToSend, totalFee) ->
                    pendingTransactions.clear()
                    pendingTransactions.addAll(pendingList)
                    updateUi(
                        CryptoValue(CryptoCurrency.BTC, totalToSend),
                        CryptoValue(CryptoCurrency.BTC, totalFee)
                    )
                },
                onError = {
                    view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                    view.dismissDialog()
                }
            )
    }
}
