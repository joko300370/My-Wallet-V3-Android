package piuk.blockchain.android.ui.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.annotations.CommonCode
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.ActivityAnalytics
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AccountIcon
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.databinding.FragmentActivitiesBinding
import piuk.blockchain.android.ui.activity.adapter.ActivitiesDelegateAdapter
import piuk.blockchain.android.ui.activity.detail.CryptoActivityDetailsBottomSheet
import piuk.blockchain.android.ui.activity.detail.FiatActivityDetailsBottomSheet
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.account.AccountSelectSheet
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.home.HomeScreenMviFragment
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.putAccount
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class ActivitiesFragment :
    HomeScreenMviFragment<ActivitiesModel, ActivitiesIntent, ActivitiesState, FragmentActivitiesBinding>(),
    AccountSelectSheet.SelectionHost {

    override val model: ActivitiesModel by scopedInject()

    private val activityAdapter: ActivitiesDelegateAdapter by lazy {
        ActivitiesDelegateAdapter(
            prefs = get(),
            assetResources = assetResources,
            onCryptoItemClicked = { cc, tx, type ->
                onCryptoActivityClicked(cc, tx, type)
            },
            onFiatItemClicked = { cc, tx -> onFiatActivityClicked(cc, tx) }
        )
    }

    private val displayList = mutableListOf<ActivitySummaryItem>()

    private val disposables = CompositeDisposable()
    private val rxBus: RxBus by inject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val exchangeRates: ExchangeRateDataManager by scopedInject()
    private val assetResources: AssetResources by scopedInject()

    private val actionEvent by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    private var state: ActivitiesState? = null

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentActivitiesBinding =
        FragmentActivitiesBinding.inflate(inflater, container, false)

    @UiThread
    override fun render(newState: ActivitiesState) {
        if (newState.isError) {
            toast(R.string.activity_loading_error, ToastCustom.TYPE_ERROR)
        }

        switchView(newState)

        binding.swipe.isRefreshing = newState.isLoading

        renderAccountDetails(newState)
        renderTransactionList(newState)

        if (this.state?.bottomSheet != newState.bottomSheet) {
            when (newState.bottomSheet) {
                ActivitiesSheet.ACCOUNT_SELECTOR -> {
                    analytics.logEvent(ActivityAnalytics.WALLET_PICKER_SHOWN)
                    showBottomSheet(AccountSelectSheet.newInstance(this))
                }
                ActivitiesSheet.CRYPTO_ACTIVITY_DETAILS -> {
                    newState.selectedCryptoCurrency?.let {
                        showBottomSheet(
                            CryptoActivityDetailsBottomSheet.newInstance(
                                it, newState.selectedTxId,
                                newState.activityType
                            )
                        )
                    }
                }
                ActivitiesSheet.FIAT_ACTIVITY_DETAILS -> {
                    newState.selectedFiatCurrency?.let {
                        showBottomSheet(
                            FiatActivityDetailsBottomSheet.newInstance(it, newState.selectedTxId)
                        )
                    }
                }
            }
        }
        this.state = newState
    }

    private fun switchView(newState: ActivitiesState) {
        with(binding) {
            when {
                newState.isLoading && newState.activityList.isEmpty() -> {
                    headerLayout.gone()
                    contentList.gone()
                    emptyView.gone()
                }
                newState.activityList.isEmpty() -> {
                    headerLayout.visible()
                    contentList.gone()
                    emptyView.visible()
                }
                else -> {
                    headerLayout.visible()
                    contentList.visible()
                    emptyView.gone()
                }
            }
        }
    }

    private fun renderAccountDetails(newState: ActivitiesState) {
        if (newState.account == state?.account) {
            return
        }

        if (newState.account == null) {
            // Should not happen! TODO: Crash
            return
        }

        with(binding) {
            disposables.clear()

            val account = newState.account

            val accIcon = AccountIcon(account, assetResources)
            accountIcon.setImageResource(accIcon.icon)

            accIcon.indicator?.let {
                check(account is CryptoAccount) {
                    "Indicators are supported only for CryptoAccounts"
                }
                val currency = account.asset
                accountIndicator.apply {
                    visible()
                    setImageResource(it)
                    setAssetIconColours(
                        tintColor = R.color.white,
                        filterColor = assetResources.assetFilter(currency)
                    )
                }
            } ?: accountIndicator.gone()

            accountName.text = account.label
            fiatBalance.text = ""

            disposables += account.fiatBalance(currencyPrefs.selectedFiatCurrency, exchangeRates)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        fiatBalance.text =
                            getString(
                                R.string.common_spaced_strings, it.toStringWithSymbol(),
                                it.currencyCode
                            )
                    },
                    onError = {
                        Timber.e("Unable to get balance for ${account.label}")
                    }
                )
        }
    }

    private fun renderTransactionList(newState: ActivitiesState) {
        if (state?.activityList == newState.activityList) {
            return
        }

        with(newState.activityList) {
            displayList.clear()
            if (isEmpty()) {
                Timber.d("Render new tx list - empty")
            } else {
                displayList.addAll(this)
            }
            activityAdapter.notifyDataSetChanged()
        }
    }

    override fun onBackPressed(): Boolean = false

    private val preselectedAccount: BlockchainAccount?
        get() = arguments?.getAccount(PARAM_ACCOUNT)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preselectedAccount?.let {
            onAccountSelected(it)
        } ?: onShowAllActivity()

        setupSwipeRefresh()
        setupRecycler()
        setupAccountSelect()
    }

    private fun setupRecycler() {
        binding.contentList.apply {
            layoutManager = SafeLayoutManager(requireContext())
            adapter = activityAdapter
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
        }
        activityAdapter.items = displayList
    }

    private fun setupToolbar() {
        activity.supportActionBar?.let {
            activity.setupToolbar(it, R.string.activities_title)
        }
    }

    private fun setupAccountSelect() {
        binding.accountSelectBtn.setOnClickListener {
            model.process(ShowAccountSelectionIntent)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipe.setOnRefreshListener {
            state?.account?.let {
                model.process(AccountSelectedIntent(it, true))
            }
        }

        // Configure the refreshing colors
        binding.swipe.setColorSchemeResources(
            R.color.blue_800,
            R.color.blue_600,
            R.color.blue_400,
            R.color.blue_200
        )
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    override fun onPause() {
        disposables.clear()
        rxBus.unregister(ActionEvent::class.java, actionEvent)
        super.onPause()
    }

    private fun onCryptoActivityClicked(
        cryptoCurrency: CryptoCurrency,
        txHash: String,
        type: CryptoActivityType
    ) {
        model.process(ShowActivityDetailsIntent(cryptoCurrency, txHash, type))
    }

    private fun onFiatActivityClicked(
        currency: String,
        txHash: String
    ) {
        model.process(ShowFiatActivityDetailsIntent(currency, txHash))
    }

    private fun onShowAllActivity() {
        model.process(SelectDefaultAccountIntent)
    }

    override fun onAccountSelected(account: BlockchainAccount) {
        model.process(AccountSelectedIntent(account, false))
    }

    // SlidingModalBottomDialog.Host
    override fun onSheetClosed() {
        model.process(ClearBottomSheetIntent)
    }

    companion object {
        private const val PARAM_ACCOUNT = "PARAM_ACCOUNT"

        fun newInstance(account: BlockchainAccount?): ActivitiesFragment {
            return ActivitiesFragment().apply {
                arguments = Bundle().apply {
                    account?.let { putAccount(PARAM_ACCOUNT, it) }
                }
            }
        }
    }
}

/**
 * supportsPredictiveItemAnimations = false to avoid crashes when computing changes.
 */
@CommonCode(commonWith = "DashboardFragment - move to ui utils package")
private class SafeLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun supportsPredictiveItemAnimations() = false
}
