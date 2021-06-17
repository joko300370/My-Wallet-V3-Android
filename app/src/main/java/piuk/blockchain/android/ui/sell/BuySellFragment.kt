package piuk.blockchain.android.ui.sell

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.blockchain.koin.payloadScope
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.ui.trackProgress
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentBuySellBinding
import piuk.blockchain.android.simplebuy.BuySellViewedEvent
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SimpleBuyCheckoutFragment
import piuk.blockchain.android.simplebuy.SimpleBuySelectCurrencyFragment
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.home.HomeScreenFragment
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import kotlin.properties.Delegates

class BuySellFragment : HomeScreenFragment, Fragment(), SellIntroFragment.SellIntroHost,
    SlidingModalBottomDialog.Host {

    private var _binding: FragmentBuySellBinding? = null
    private val binding: FragmentBuySellBinding
        get() = _binding!!

    private val compositeDisposable = CompositeDisposable()
    private val appUtil: AppUtil by inject()
    private val analytics: Analytics by inject()
    private val simpleBuySync: SimpleBuySyncFactory by scopedInject()
    private val buySellFlowNavigator: BuySellFlowNavigator
        get() = payloadScope.get()

    private val showView: BuySellViewType by unsafeLazy {
        arguments?.getSerializable(VIEW_TYPE) as? BuySellViewType
            ?: BuySellViewType.TYPE_BUY
    }

    private val selectedAsset: CryptoCurrency? by unsafeLazy {
        arguments?.getSerializable(SELECTED_ASSET) as? CryptoCurrency
    }

    private var hasReturnedFromBuyActivity = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuySellBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setupToolbar(R.string.buy_and_sell)
        analytics.logEvent(BuySellViewedEvent())
    }

    private fun subscribeForNavigation() {
        compositeDisposable += simpleBuySync.performSync().onErrorComplete().toSingleDefault(false)
            .flatMap {
                buySellFlowNavigator.navigateTo(selectedAsset)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                binding.buySellEmpty.gone()
            }
            .trackProgress(appUtil.activityIndicator)
            .subscribeBy(
                onSuccess = {
                    renderBuySellFragments(it)
                },
                onError = {
                    renderErrorState()
                }
            )
    }

    private fun renderBuySellFragments(action: BuySellIntroAction?) {
        with(binding) {
            buySellEmpty.gone()
            pager.visible()
            when (action) {
                is BuySellIntroAction.NavigateToCurrencySelection ->
                    goToCurrencySelection(action.supportedCurrencies)
                is BuySellIntroAction.DisplayBuySellIntro -> {
                    if (!action.isGoldButNotEligible) {
                        renderBuySellUi(action.hasPendingBuy)
                    } else {
                        renderNotEligibleUi()
                    }
                }
                is BuySellIntroAction.StarBuyWithSelectedAsset -> {
                    renderBuySellUi(action.hasPendingBuy)
                    if (!action.hasPendingBuy && !hasReturnedFromBuyActivity) {
                        hasReturnedFromBuyActivity = false
                        startActivityForResult(
                            SimpleBuyActivity.newInstance(
                                context = activity as Context,
                                cryptoCurrency = action.selectedAsset,
                                launchFromNavigationBar = true
                            ), SB_ACTIVITY
                        )
                    }
                }
                else -> startActivity(
                    SimpleBuyActivity.newInstance(
                        context = activity as Context,
                        launchFromNavigationBar = true,
                        launchKycResume = false
                    )
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SB_ACTIVITY && resultCode == Activity.RESULT_CANCELED) {
            hasReturnedFromBuyActivity = true
        }
    }

    private fun renderErrorState() {
        with(binding) {
            pager.gone()
            buySellEmpty.setDetails {
                subscribeForNavigation()
            }
            buySellEmpty.visible()
        }
    }

    private fun renderNotEligibleUi() {
        with(binding) {
            pager.gone()
            notEligibleIcon.visible()
            notEligibleTitle.visible()
            notEligibleDescription.visible()
        }
    }

    private val pagerAdapter: ViewPagerAdapter by lazy {
        ViewPagerAdapter(
            listOf(getString(R.string.common_buy), getString(R.string.common_sell)),
            childFragmentManager
        )
    }

    private fun renderBuySellUi(hasPendingBuy: Boolean) {
        with(binding) {
            tabLayout.setupWithViewPager(pager)
            activity?.setupToolbar(R.string.buy_and_sell)

            if (pager.adapter == null) {
                pager.adapter = pagerAdapter
                when (showView) {
                    BuySellViewType.TYPE_BUY -> pager.setCurrentItem(
                        BuySellViewType.TYPE_BUY.ordinal, true
                    )
                    BuySellViewType.TYPE_SELL -> pager.setCurrentItem(
                        BuySellViewType.TYPE_SELL.ordinal, true
                    )
                }
            }

            pagerAdapter.showPendingBuy = hasPendingBuy
            pager.visible()
            notEligibleIcon.gone()
            notEligibleTitle.gone()
            notEligibleDescription.gone()
        }
    }

    private fun goToCurrencySelection(supportedCurrencies: List<String>) {
        SimpleBuySelectCurrencyFragment.newInstance(supportedCurrencies).show(childFragmentManager, "BOTTOM_SHEET")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
        _binding = null
    }

    companion object {
        private const val VIEW_TYPE = "VIEW_TYPE"
        private const val SELECTED_ASSET = "SELECTED_ASSET"
        private const val SB_ACTIVITY = 321

        fun newInstance(viewType: BuySellViewType = BuySellViewType.TYPE_BUY, asset: CryptoCurrency?) =
            BuySellFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(VIEW_TYPE, viewType)
                    asset?.let {
                        putSerializable(SELECTED_ASSET, it)
                    }
                }
            }
    }

    enum class BuySellViewType {
        TYPE_BUY,
        TYPE_SELL
    }

    override fun onSheetClosed() = subscribeForNavigation()

    override fun onSellFinished() = subscribeForNavigation()

    override fun onSellInfoClicked() = navigator().goToTransfer()

    override fun onSellListEmptyCta() {
        binding.pager.setCurrentItem(BuySellViewType.TYPE_BUY.ordinal, true)
    }

    override fun onResume() {
        super.onResume()
        subscribeForNavigation()
    }

    override fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")

    override fun onBackPressed(): Boolean = false
}

@SuppressLint("WrongConstant")
internal class ViewPagerAdapter(
    private val titlesList: List<String>,
    fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int = titlesList.size

    override fun getPageTitle(position: Int): CharSequence =
        titlesList[position]

    var showPendingBuy: Boolean by Delegates.observable(false) { _, oldV, newV ->
        if (newV != oldV)
            notifyDataSetChanged()
    }

    override fun getItem(position: Int): Fragment = when (position) {
        0 -> if (!showPendingBuy) BuyIntroFragment.newInstance() else
            SimpleBuyCheckoutFragment.newInstance(isForPending = true, showOnlyOrderData = true)
        else -> SellIntroFragment.newInstance()
    }
}