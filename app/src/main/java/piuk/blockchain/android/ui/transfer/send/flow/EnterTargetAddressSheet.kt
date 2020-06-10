package piuk.blockchain.android.ui.transfer.send.flow

import android.app.Activity
import android.content.Intent
import android.text.Editable
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_send_address.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AddressFactory
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.adapter.AccountsAdapter
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import timber.log.Timber

class EnterTargetAddressSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_address

    private val appUtil: AppUtil by inject()
    private val coincore: Coincore by scopedInject()
    private val addressFactory: AddressFactory by scopedInject()

    private val disposables = CompositeDisposable()
    private var state: SendState = SendState()

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterTargetAddressSheet")

        with(dialogView) {
            if (state.sendingAccount != newState.sendingAccount) {
                from_details.account = newState.sendingAccount
                setupTransferList(newState.sendingAccount)
            }
            cta_button.isEnabled = newState.nextEnabled
        }
    }

    private val addressTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            val address = addressFactory.parse(s.toString(), CryptoCurrency.ETHER)
            if (address != null) {
                addressSelected(address)
                // Hide error
            } else {
                // Show error
            }
        }
    }

    override fun initControls(view: View) {
        with(dialogView) {
            address_entry.addTextChangedListener(addressTextWatcher)
            btn_scan.setOnClickListener { onLaunchAddressScan() }
            cta_button.setOnClickListener { onCtaClick() }
        }
    }

    private fun setupTransferList(account: CryptoSingleAccount) {
        with(dialogView.wallet_select) {
            val accountAdapter = AccountsAdapter(::accountSelected)
            val itemList = mutableListOf<CryptoSingleAccount>()
            accountAdapter.itemsList = itemList

            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )

            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = accountAdapter

            // This list should be fetched by the interactor when we get initialised, but this is a hacky UI
            // just to get things working, so we'll do it here. For now. TODO: Move this!
            disposables += coincore[account.asset].canTransferTo(account)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        if (itemList.isEmpty()) {
//                            showEmptyTransfers()
                        } else {
                            itemList.addAll(it)
                            accountAdapter.notifyDataSetChanged()
                        }
                    },
                    onError = {
                        showErrorToast("Failed getting transfer wallets")
                    }
                )
        }
    }

    private fun accountSelected(account: CryptoSingleAccount) {
//        addressSelected(account.receiveAddress)
    }

    private fun onLaunchAddressScan() {
        if (!appUtil.isCameraOpen) {
            startActivityForResult(
                Intent(activity, CaptureActivity::class.java),
                SCAN_QR_ADDRESS
            )
        } else {
            showErrorToast(R.string.camera_unavailable)
        }
    }

    private fun addressSelected(address: CryptoAddress) {
        SendIntent.AddressSelected(address)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
        when (requestCode) {
            SCAN_QR_ADDRESS -> handleScanResult(resultCode, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }

    private fun handleScanResult(resultCode: Int, data: Intent?) {
        Timber.d("Got QR scan result!")
        if (resultCode == Activity.RESULT_OK && data != null) {
            data.getStringExtra(CaptureActivity.SCAN_RESULT)?.let {
                // Just supporting ETH in this pass,
                // TODO: Replace this with a full scanning parser?
                val address = addressFactory.parse(it, CryptoCurrency.ETHER)
                if (address == null) {
                    showErrorToast("Invalid ETH address!!")
                } else {
                    addressSelected(address)
                }
            }
        }
    }

    private fun onCtaClick() =
        model.process(SendIntent.AddressSelectionConfirmed)

    companion object {
        const val SCAN_QR_ADDRESS = 2985
        fun newInstance(): EnterTargetAddressSheet =
            EnterTargetAddressSheet()
    }
}
