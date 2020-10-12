package piuk.blockchain.android.ui.backup.completed

import com.blockchain.preferences.WalletStatus
import io.reactivex.rxkotlin.plusAssign
import android.annotation.SuppressLint
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.account.TransferFundsDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber

class BackupWalletCompletedPresenter(
    private val transferFundsDataManager: TransferFundsDataManager,
    private val walletStatus: WalletStatus
) : BasePresenter<BackupWalletCompletedView>() {

    override fun onViewReady() {
        val lastBackup = walletStatus.lastBackupTime
        if (lastBackup != 0L) {
            view.showLastBackupDate(lastBackup)
        } else {
            view.hideLastBackupDate()
        }
    }

    @SuppressLint("CheckResult")
    internal fun checkTransferableFunds() {
        compositeDisposable += transferFundsDataManager.transferableFundTransactionListForDefaultAccount
            .subscribeBy(
                onNext = { (pendingList, _, _) ->
                    if (pendingList.isNotEmpty()) {
                        view.showTransferFundsPrompt()
                    }
                },
                onError = { Timber.e(it) }
            )
    }
}
