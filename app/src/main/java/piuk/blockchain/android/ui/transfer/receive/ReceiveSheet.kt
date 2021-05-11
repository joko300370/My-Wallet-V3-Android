package piuk.blockchain.android.ui.transfer.receive

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.RequestAnalyticsEvents
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.databinding.DialogReceiveBinding
import piuk.blockchain.android.databinding.ReceiveShareRowBinding
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.transfer.receive.plugin.ReceiveMemoView
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.invisible
import piuk.blockchain.android.util.putAccount
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf

internal class ReceiveSheet : MviBottomSheet<ReceiveModel, ReceiveIntent, ReceiveState, DialogReceiveBinding>() {
    override val model: ReceiveModel by scopedInject()

    val account: CryptoAccount?
        get() = arguments?.getAccount(PARAM_ACCOUNT) as? CryptoAccount

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogReceiveBinding =
        DialogReceiveBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogReceiveBinding) {
        account?.let {
            model.process(InitWithAccount(it, DIMENSION_QR_CODE))
            binding.receiveAccountDetails.updateAccount(it)
        } ?: dismiss()

        with(binding) {
            shareButton.isEnabled = false
            copyButton.isEnabled = false
            progressbar.visible()
            qrImage.invisible()
        }
    }

    override fun render(newState: ReceiveState) {
        if (newState.shareList.isNotEmpty()) {
            renderShare(newState)
        } else {
            renderReceive(newState)
        }
    }

    private fun renderReceive(newState: ReceiveState) {
        with(binding) {
            switcher.displayedChild = VIEW_RECEIVE
            receiveTitle.text = getString(R.string.tx_title_receive, newState.account.asset.displayTicker)
            val addressAvailable = newState.qrBitmap != null
            if (addressAvailable) {
                shareButton.setOnClickListener { shareAddress() }
                copyButton.setOnClickListener { copyAddress(newState.address) }
            } else {
                shareButton.setOnClickListener { }
                copyButton.setOnClickListener { }
            }
            shareButton.isEnabled = addressAvailable
            copyButton.isEnabled = addressAvailable

            progressbar.visibleIf { addressAvailable.not() }
            qrImage.visibleIf { addressAvailable }

            qrImage.setImageBitmap(newState.qrBitmap)
            receivingAddress.text = newState.address.address
        }

        setCustomSlot(newState)
    }

    private fun renderShare(newState: ReceiveState) {
        with(binding) {
            switcher.displayedChild = VIEW_SHARE

            shareTitle.text = getString(R.string.receive_share_title, newState.account.asset.displayTicker)
            with(shareList) {
                layoutManager = LinearLayoutManager(context)
                adapter = ShareListAdapter(newState.shareList).apply {
                    itemClickedListener = { dismiss() }
                }
            }
        }
    }

    private fun setCustomSlot(newState: ReceiveState) {
        when {
            newState.address.memo != null -> ReceiveMemoView(requireContext())
            // TODO: SEGWIT LEGACY SELECTOR
            else -> null
        }?.let {
            binding.customised.addView(it)
            it.updateAddress(newState.address)
        }
    }

    private fun shareAddress() {
        activity.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_share)
                .setCancelable(false)
                .setPositiveButton(R.string.common_yes) { _, _ -> model.process(ShowShare) }
                .setNegativeButton(R.string.common_no, null)
                .show()
        }
        analytics.logEvent(RequestAnalyticsEvents.RequestPaymentClicked)
    }

    private fun copyAddress(address: CryptoAddress) {
        activity?.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.common_yes) { _, _ ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Send address", address.toUrl())
                    toast(R.string.copied_to_clipboard)
                    clipboard.setPrimaryClip(clip)
                }
                .setNegativeButton(R.string.common_no, null)
                .show()
        }
    }

    companion object {
        private const val PARAM_ACCOUNT = "account_param"
        private const val DIMENSION_QR_CODE = 600

        private const val VIEW_RECEIVE = 0
        private const val VIEW_SHARE = 1

        fun newInstance(account: CryptoAccount): ReceiveSheet =
            ReceiveSheet().apply {
                arguments = Bundle().apply {
                    putAccount(PARAM_ACCOUNT, account)
                }
            }
    }
}

private class ShareListAdapter(private val paymentCodeData: List<SendPaymentCodeData>) :
    RecyclerView.Adapter<ShareListAdapter.ViewHolder>() {

    var itemClickedListener: () -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val row = ReceiveShareRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(row)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = paymentCodeData[position]
        holder.bind(data) {
            itemClickedListener()
        }
    }

    override fun getItemCount() = paymentCodeData.size

    class ViewHolder(private val binding: ReceiveShareRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: SendPaymentCodeData, onClick: () -> Unit) {
            binding.shareAppTitle.text = data.title
            binding.shareAppImage.setImageDrawable(data.logo)

            binding.root.setOnClickListener {
                onClick.invoke()
                attemptToStartTargetActivity(itemView.context, data.title, data.intent)
            }
        }

        private fun attemptToStartTargetActivity(ctx: Context, appName: String, intent: Intent) {
            try {
                ctx.startActivity(intent)
            } catch (e: SecurityException) {
                ctx.toast(ctx.getString(R.string.share_failed, appName))
            }
        }
    }
}
