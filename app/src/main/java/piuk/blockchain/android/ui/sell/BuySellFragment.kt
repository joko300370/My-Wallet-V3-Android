package piuk.blockchain.android.ui.sell

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
import piuk.blockchain.android.ui.transfer.send.flow.DialogFlow
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class BuySellFragment :
    HomeScreenFragment, Fragment(),
    DialogFlow.FlowHost, SlidingModalBottomDialog.Host {

    override fun onFlowFinished() {
        TODO("Not yet implemented")
    }

    private val compositeDisposable = CompositeDisposable()
    private val appUtil: AppUtil by inject()
    private val simpleBuySync: SimpleBuySyncFactory by scopedInject()
    private val buySellFlowNavigator: BuySellFlowNavigator
        get() = payloadScope.get()

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
            .trackLoading(appUtil.activityIndicator)
            .subscribeBy(
                onSuccess = {
                    when (it) {
                        is BuySellIntroAction.NavigateToCurrencySelection ->
                            goToCurrencySelection(it.supportedCurrencies)
                        is BuySellIntroAction.DisplayBuySellIntro -> {
                            if (!it.isGoldButNotEligible) renderBuySellUi(it.sellEnabled)
                            else renderNotEligibleUi()
                        }
                        else -> startActivity(SimpleBuyActivity.newInstance(
                            context = activity as Context,
                            launchFromNavigationBar = true, launchKycResume = false
                        ))
                    }
                },
                onError = {}
            )
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
        fun newInstance() = BuySellFragment()
    }

    override fun onSheetClosed() {
        subscribeForNavigation()
    }

    override fun onResume() {
        super.onResume()
        subscribeForNavigation()
    }

    override fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")

    override fun onBackPressed(): Boolean = false
}

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