package piuk.blockchain.android.ui.dashboard.sheets

import android.content.DialogInterface
import android.view.View
import kotlinx.android.synthetic.main.dialog_custodial_intro.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class ForceBackupForSendSheet : SlidingModalBottomDialog() {

    interface Host : SlidingModalBottomDialog.Host {
        fun startBackupForTransfer()
        fun startTransferFunds()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException("Host fragment is not a ForceBackupForSendSheet.Host")
    }

    override val layoutResource: Int = R.layout.dialog_backup_for_send

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        host.startTransferFunds()
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }

    private fun onCtaClick() {
        dismiss()
        host.startBackupForTransfer()
    }

    companion object {
        fun newInstance(): ForceBackupForSendSheet =
            ForceBackupForSendSheet()
    }
}
