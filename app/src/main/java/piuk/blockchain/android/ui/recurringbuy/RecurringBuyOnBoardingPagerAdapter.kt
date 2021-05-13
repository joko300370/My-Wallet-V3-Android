package piuk.blockchain.android.ui.recurringbuy

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class RecurringBuyOnBoardingPagerAdapter(
    activity: AppCompatActivity,
    private val recurringBuyInfoList: List<RecurringBuyInfo>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = recurringBuyInfoList.size

    override fun createFragment(position: Int): Fragment =
        RecurringBuyOnBoardingFragment.newInstance(recurringBuyInfoList[position])
}