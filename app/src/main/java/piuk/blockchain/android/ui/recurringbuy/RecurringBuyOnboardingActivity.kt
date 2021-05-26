package piuk.blockchain.android.ui.recurringbuy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityRecurringBuyOnBoardingBinding
import piuk.blockchain.android.simplebuy.SimpleBuyActivity

class RecurringBuyOnboardingActivity : AppCompatActivity() {

    private val binding: ActivityRecurringBuyOnBoardingBinding by lazy {
        ActivityRecurringBuyOnBoardingBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showFullScreen()
        setContentView(binding.root)

        val recurringBuyOnBoardingPagerAdapter =
            RecurringBuyOnBoardingPagerAdapter(this, createListOfRecurringBuyInfo())

        with(binding) {
            viewpager.adapter = recurringBuyOnBoardingPagerAdapter
            indicator.setViewPager(viewpager)
            recurringBuyCta.setOnClickListener { goToRecurringSetUpScreen() }
            closeBtn.setOnClickListener { finish() }
        }
    }

    private fun showFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    private fun goToRecurringSetUpScreen() {
        startActivity(Intent(this, SimpleBuyActivity::class.java))
        finish()
    }

    private fun createListOfRecurringBuyInfo(): List<RecurringBuyInfo> = listOf(
        RecurringBuyInfo(
            title = getString(R.string.recurring_buy_title_1),
            subtitle1 = getString(R.string.recurring_buy_subtitle_1),
            hasImage = false
        ),
        RecurringBuyInfo(
            title = getString(R.string.recurring_buy_title_2),
            subtitle1 = getString(R.string.recurring_buy_subtitle_2), hasImage = true
        ),
        RecurringBuyInfo(
            title = getString(R.string.recurring_buy_title_3),
            subtitle1 = getString(R.string.recurring_buy_subtitle_3), hasImage = true
        ),
        RecurringBuyInfo(
            title = getString(R.string.recurring_buy_title_4),
            subtitle1 = getString(R.string.recurring_buy_subtitle_4), hasImage = true
        ),
        RecurringBuyInfo(
            title = getString(R.string.recurring_buy_title_5),
            subtitle1 = getString(R.string.recurring_buy_subtitle_5_1),
            subtitle2 = getString(R.string.recurring_buy_subtitle_5_2),
            hasImage = false
        )
    )

    override fun onBackPressed() {
        with(binding) {
            if (viewpager.currentItem == 0) {
                super.onBackPressed()
            } else {
                viewpager.currentItem = viewpager.currentItem - 1
            }
        }
    }
}