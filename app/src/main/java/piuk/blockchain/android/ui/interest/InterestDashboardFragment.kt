package piuk.blockchain.android.ui.interest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.models.nabu.KycTierLevel
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_interest_dashboard.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import timber.log.Timber

class InterestDashboardFragment : Fragment() {

    interface InterestDashboardHost {
        fun startKyc()
        fun showInterestSummarySheet(account: SingleAccount, cryptoCurrency: CryptoCurrency)
        fun startDepositFlow(fromAccount: SingleAccount, toAccount: SingleAccount)
        fun startAccountSelection(filter: Single<List<BlockchainAccount>>, toAccount: SingleAccount)
    }

    val host: InterestDashboardHost by lazy {
        activity as? InterestDashboardHost ?: throw IllegalStateException(
            "Host fragment is not a InterestDashboardFragment.InterestDashboardHost")
    }

    private val disposables = CompositeDisposable()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val kycTierService: TierService by scopedInject()
    private val coincore: Coincore by scopedInject()

    private val listAdapter: InterestDashboardAdapter by lazy {
        InterestDashboardAdapter(
            disposables = disposables,
            custodialWalletManager = custodialWalletManager,
            verificationClicked = ::startKyc,
            itemClicked = ::interestItemClicked
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_interest_dashboard)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        interest_dashboard_list.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = listAdapter
        }

        val items = mutableListOf<InterestDashboardItem>()

        disposables +=
            Singles.zip(
                kycTierService.tiers(),
                custodialWalletManager.getInterestEnabledAssets()
            ).observeOn(AndroidSchedulers.mainThread()).subscribeBy(
                onSuccess = { singles ->
                    val isKycGold = singles.first.isApprovedFor(KycTierLevel.GOLD)
                    if (!isKycGold) {
                        items.add(InterestIdentityVerificationItem)
                    }

                    singles.second.map {
                        items.add(InterestAssetInfoItem(isKycGold, it))
                    }

                    listAdapter.items = items
                    listAdapter.notifyDataSetChanged()
                },
                onError = {
                    Timber.e("Error loading interest summary details $it")
                }
            )
    }

    fun refreshBalances() {
        // force redraw, so balances update
        listAdapter.notifyDataSetChanged()
    }

    private fun interestItemClicked(cryptoCurrency: CryptoCurrency, hasBalance: Boolean) {
        disposables += coincore[cryptoCurrency].accountGroup(AssetFilter.Interest).subscribe {
            val interestAccount = it.accounts.first()
            if (hasBalance) {
                host.showInterestSummarySheet(interestAccount, cryptoCurrency)
            } else {
                disposables += coincore[cryptoCurrency].accountGroup(AssetFilter.NonCustodial)
                    .subscribe { ncg ->
                        if (ncg.accounts.size > 1) {
                            host.startAccountSelection(Single.just(ncg.accounts), interestAccount)
                        } else {
                            val defaultNonCustodial = ncg.accounts.first { acc -> acc.isDefault }
                            host.startDepositFlow(defaultNonCustodial, interestAccount)
                        }
                    }
            }
        }
    }

    private fun startKyc() {
        host.startKyc()
    }

    companion object {
        fun newInstance(): InterestDashboardFragment = InterestDashboardFragment()
    }
}