package piuk.blockchain.android.ui.sell

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.blockchain.koin.payloadScope
import com.blockchain.koin.scopedInject
import com.blockchain.ui.trackLoading
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_buy_sell.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.simplebuy.SimpleBuySelectCurrencyFragment
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.ui.home.HomeNavigator
import piuk.blockchain.android.ui.home.HomeScreenFragment
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class BuySellFragment : HomeScreenFragment, Fragment(), SellIntroFragment.SellIntroHost,
    SlidingModalBottomDialog.Host {

    private val compositeDisposable = CompositeDisposable()
    private val appUtil: AppUtil by inject()
    private val simpleBuySync: SimpleBuySyncFactory by scopedInject()
    private val buySellFlowNavigator: BuySellFlowNavigator
        get() = payloadScope.get()

    private val showView: BuySellViewType by unsafeLazy {
        arguments?.getSerializable(VIEW_TYPE) as? BuySellViewType
            ?: BuySellViewType.TYPE_BUY
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_buy_sell)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setupToolbar(R.string.buy_and_sell)
    }

    private fun subscribeForNavigation() {
        compositeDisposable += simpleBuySync.performSync().onErrorComplete().toSingleDefault(false)
            .flatMap {
                buySellFlowNavigator.navigateTo()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                buy_sell_empty.gone()
            }
            .trackLoading(appUtil.activityIndicator)
            .subscribeBy(
                onSuccess = {
                    renderBuySellFragments(it)
                },
                onError = {
                    renderErrorState()
                }
            )
    }

    private fun renderBuySellFragments(it: BuySellIntroAction?) {
        buy_sell_empty.gone()
        pager.visible()
        when (it) {
            is BuySellIntroAction.NavigateToCurrencySelection ->
                goToCurrencySelection(it.supportedCurrencies)
            is BuySellIntroAction.DisplayBuySellIntro -> {
                if (!it.isGoldButNotEligible) {
                    renderBuySellUi(it.sellEnabled)
                } else {
                    renderNotEligibleUi()
                }
            }
            else -> startActivity(SimpleBuyActivity.newInstance(
                context = activity as Context,
                launchFromNavigationBar = true, launchKycResume = false
            ))
        }
    }

    private fun renderErrorState() {
        pager.gone()
        buy_sell_empty.setDetails {
            subscribeForNavigation()
        }
        buy_sell_empty.visible()
    }

    private fun renderNotEligibleUi() {
        pager.gone()
        not_eligible_icon.visible()
        not_eligible_title.visible()
        not_eligible_description.visible()
    }

    private fun renderBuySellUi(sellEnabled: Boolean) {
        if (sellEnabled) {
            tab_layout.setupWithViewPager(pager)
            activity?.setupToolbar(R.string.buy_and_sell)
        } else {
            tab_layout.gone()
            activity?.setupToolbar(R.string.buy)
        }

        pager.adapter = ViewPagerAdapter(
            listOf(getString(R.string.buy), getString(R.string.sell)),
            sellEnabled,
            childFragmentManager
        )

        when (showView) {
            BuySellViewType.TYPE_BUY -> pager.setCurrentItem(
                BuySellViewType.TYPE_BUY.ordinal, true)
            BuySellViewType.TYPE_SELL -> pager.setCurrentItem(
                BuySellViewType.TYPE_SELL.ordinal, true)
        }

        pager.visible()
        not_eligible_icon.gone()
        not_eligible_title.gone()
        not_eligible_description.gone()
    }

    private fun goToCurrencySelection(supportedCurrencies: List<String>) {
        SimpleBuySelectCurrencyFragment.newInstance(supportedCurrencies).show(childFragmentManager, "BOTTOM_SHEET")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }

    companion object {
        private const val VIEW_TYPE = "VIEW_TYPE"

        fun newInstance(viewType: BuySellViewType = BuySellViewType.TYPE_BUY) = BuySellFragment().apply {
            arguments = Bundle().apply {
                putSerializable(VIEW_TYPE, viewType)
            }
        }
    }

    enum class BuySellViewType {
        TYPE_BUY,
        TYPE_SELL
    }

    override fun onSheetClosed() {
        subscribeForNavigation()
    }

    override fun onSellFinished() {
        subscribeForNavigation()
    }

    override fun onSellInfoClicked() {
        navigator().goToTransfer()
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
class ViewPagerAdapter(
    private val titlesList: List<String>,
    private val sellEnabled: Boolean,
    fragmentManager: FragmentManager
) :
    FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int = if (sellEnabled) titlesList.size else 1
    override fun getPageTitle(position: Int): CharSequence =
        titlesList[position]

    override fun getItem(position: Int): Fragment = when (position) {
        0 -> BuyIntroFragment.newInstance()
        else -> SellIntroFragment.newInstance()
    }
}