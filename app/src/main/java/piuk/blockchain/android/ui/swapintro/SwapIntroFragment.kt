package piuk.blockchain.android.ui.swapintro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.SwapAnalyticsEvents
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.campaign.CampaignType
import kotlinx.android.synthetic.main.fragment_swap_intro.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.home.HomeScreenMvpFragment
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.androidcoreui.ui.base.ToolBarActivity
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class SwapIntroFragment : HomeScreenMvpFragment<SwapIntroView, SwapIntroPresenter>(), SwapIntroView {

    override val presenter: SwapIntroPresenter by scopedInject()
    override val view: SwapIntroView = this

    override fun onBackPressed(): Boolean =
        false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_swap_intro)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        fragmentManager?.let {
            intro_viewpager.adapter = SwapIntroPagerAdapter(it, items())
        }
        indicator.setViewPager(intro_viewpager)
        get_started.setOnClickListener {
            presenter.onGetStartedPressed()
            analytics.logEvent(SwapAnalyticsEvents.SwapIntroStartButtonClick)
            KycNavHostActivity.startForResult(requireActivity(), CampaignType.Swap, MainActivity.KYC_STARTED)
        }
    }

    private fun items(): List<SwapIntroModel> =
        listOf(
            SwapIntroModel(getString(R.string.swap_intro_title_page_one),
                getString(R.string.swap_intro_description_page_one),
                R.drawable.icon_swap_intro_one),
            SwapIntroModel(getString(R.string.swap_intro_title_page_two),
                getString(R.string.swap_intro_description_page_two),
                R.drawable.icon_swap_intro_two),
            SwapIntroModel(getString(R.string.swap_intro_title_page_three),
                getString(R.string.swap_intro_description_page_three),
                R.drawable.icon_swap_intro_three),
            SwapIntroModel(getString(R.string.swap_intro_title_page_four),
                getString(R.string.swap_intro_description_page_four),
                R.drawable.icon_swap_intro_four),
            SwapIntroModel(getString(R.string.swap_intro_title_page_five),
                getString(R.string.swap_intro_description_page_five_1),
                R.drawable.icon_swap_intro_five)
        )

    private fun setupToolbar() {
        val supportActionBar = (activity as AppCompatActivity).supportActionBar
        if (supportActionBar != null) {

            (activity as ToolBarActivity).setupToolbar(
                supportActionBar, R.string.swap_intro_title
            )
        } else {
            navigator().gotoDashboard()
        }
    }

    companion object {
        fun newInstance(): SwapIntroFragment = SwapIntroFragment()
    }
}
