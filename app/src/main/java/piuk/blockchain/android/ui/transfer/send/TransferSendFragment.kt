package piuk.blockchain.android.ui.transfer.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_transfer.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.flow.EnterTargetAddressSheet
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.transfer.send.adapter.AccountsAdapter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber

class TransferSendFragment : Fragment(), SlidingModalBottomDialog.Host {

    private val disposables = CompositeDisposable()
    private val coincore: Coincore by scopedInject()
    private val uiScheduler = AndroidSchedulers.mainThread()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_transfer)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(account_list) {
            val accountAdapter = AccountsAdapter(::onAccountSelected)
            val itemList = mutableListOf<CryptoSingleAccount>()
            accountAdapter.itemsList = itemList

            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = accountAdapter

            disposables += Single.merge(
                CryptoCurrency.activeCurrencies().map { ac ->
                    coincore[ac].accounts()
                }.toList())
                .observeOn(uiScheduler)
                .subscribeBy(
                    onNext = {
                        itemList.addAll(it.accounts
                            .filterIsInstance<CryptoSingleAccount>()
                            .filter { a -> a.isFunded })
                        accountAdapter.notifyDataSetChanged()
                    },
                    onError = {
                        ToastCustom.makeText(
                            requireContext(),
                            getString(R.string.transfer_wallets_load_error),
                            ToastCustom.LENGTH_SHORT,
                            ToastCustom.TYPE_ERROR
                        )
                    },
                    onComplete = {
                        if (itemList.isEmpty()) {
                            showEmptyState()
                        }
                    }
                )
        }
    }

    private fun showEmptyState() {
        account_list.gone()
        send_blurb.gone()
        empty_view.visible()
        button_buy_crypto.setOnClickListener {
            startActivity(SimpleBuyActivity.newInstance(requireContext()))
        }
    }

    private fun onAccountSelected(cryptoAccount: CryptoSingleAccount) {
        disposables += coincore.requireSecondPassword().observeOn(uiScheduler)
            .subscribeBy(onSuccess = { secondPassword ->
                initialiseSendFlow(cryptoAccount, secondPassword)
            }, onError = {
                Timber.e("Unable to configure send flow, aborting. e == $it")
                activity?.finish()
            })
    }

    private fun initialiseSendFlow(account: CryptoSingleAccount, passwordRequired: Boolean) {
        // Create the send scope
        openScope()
        // Get the model
        model.apply {
            // Trigger intent to set initial state: source account & password required
            disposables += state.subscribeBy(
                onNext = { handleStateChange(it) },
                onError = { Timber.e("Send state is broken: $it") }
            )
            process(SendIntent.Initialise(account, passwordRequired))
        }
    }

    private var currentStep: SendStep = SendStep.ZERO

    private fun handleStateChange(newState: SendState) {
        if (currentStep != newState.currentStep) {
            currentStep = newState.currentStep
            if (currentStep == SendStep.ZERO) {
                onSendComplete()
            } else {
                showFlowStep(currentStep)
            }
        }
    }

    private fun showFlowStep(step: SendStep) {
        replaceBottomSheet(
            when (step) {
                SendStep.ZERO -> null
                SendStep.ENTER_PASSWORD -> EnterSecondPasswordSheet.newInstance()
                SendStep.ENTER_ADDRESS -> EnterTargetAddressSheet.newInstance()
                SendStep.ENTER_AMOUNT -> EnterAmountSheet.newInstance()
                SendStep.CONFIRM_DETAIL -> ConfirmTransactionSheet.newInstance()
                SendStep.IN_PROGRESS -> TransactionInProgressSheet.newInstance()
                SendStep.SEND_ERROR -> TransactionErrorSheet.newInstance()
                SendStep.SEND_COMPLETE -> TransactionCompleteSheet.newInstance()
            }
        )
    }

    private fun openScope() =
        try {
            createSendScope()
        } catch (e: Throwable) {
            Timber.d("wtf? $e")
        }

    private fun closeScope() =
        closeSendScope()

    private val model: SendModel
        get() = sendScope().get()

    override fun onSheetClosed() {
        disposables.clear()
        closeScope()
        currentStep = SendStep.ZERO
    }

    private fun onSendComplete() {
        disposables.clear()
        closeScope()
        activity?.finish()
    }

    @UiThread
    fun replaceBottomSheet(bottomSheet: BottomSheetDialogFragment?) {
        childFragmentManager.findFragmentByTag(SHEET_FRAGMENT_TAG)?.let {
            childFragmentManager.beginTransaction().remove(it).commitNow()
        }
        bottomSheet?.show(childFragmentManager, SHEET_FRAGMENT_TAG)
    }

    companion object {
        private const val SHEET_FRAGMENT_TAG = "BOTTOM_SHEET"
        fun newInstance() = TransferSendFragment()
    }
}
