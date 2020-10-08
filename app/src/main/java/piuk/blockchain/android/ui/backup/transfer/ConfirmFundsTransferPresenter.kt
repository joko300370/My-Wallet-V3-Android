package piuk.blockchain.android.ui.backup.transfer

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.data.LegacyAddress
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.PendingTransaction
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.chooser.WalletAccountHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.events.PayloadSyncedEvent
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

class ConfirmFundsTransferPresenter(
    private val walletAccountHelper: WalletAccountHelper,
    private val payloadDataManager: PayloadDataManager,
    private val stringUtils: StringUtils,
    private val exchangeRates: ExchangeRateDataManager,
    currencyPrefs: CurrencyPrefs
) : BasePresenter<ConfirmFundsTransferView>() {

    @VisibleForTesting
    internal val pendingTransactions: MutableList<PendingTransaction> = mutableListOf()

    private val userFiat: String = currencyPrefs.selectedFiatCurrency

    override fun onViewReady() {
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
}
