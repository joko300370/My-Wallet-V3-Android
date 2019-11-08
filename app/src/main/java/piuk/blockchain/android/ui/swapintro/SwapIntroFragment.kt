package piuk.blockchain.android.ui.swapintro

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import kotlinx.android.synthetic.main.fragment_swap_intro.*
import org.koin.android.ext.android.get
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.home.HomeFragment
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.androidcoreui.ui.base.ToolBarActivity
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class SwapIntroFragment : HomeFragment<SwapIntroView, SwapIntroPresenter>(), SwapIntroView {

    override fun onBackPressed(): Boolean =
        false

    override fun createPresenter(): SwapIntroPresenter = get()

    override fun getMvpView(): SwapIntroView = this

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_swap_intro)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        intro_viewpager.adapter = SwapIntroPagerAdapter(fragmentManager, items())
        indicator.setViewPager(intro_viewpager)
        get_started.setOnClickListener {
            presenter.onGetStartedPressed()
            startActivityForResult(
                KycNavHostActivity.intentArgs(activity!!, CampaignType.Swap),
                MainActivity.KYC_STARTED)
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
                getString(R.string.swap_intro_description_page_five),
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

interface SwapIntroView : piuk.blockchain.androidcoreui.ui.base.View