package piuk.blockchain.android.ui.sell

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.models.nabu.KycTierLevel
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.sell_intro_fragment.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.VerifyIdentityBenefit
import piuk.blockchain.android.ui.customviews.account.AccountDecorator
import piuk.blockchain.android.ui.transfer.send.flow.DialogFlow
import piuk.blockchain.android.ui.transfer.send.flow.SendFlow
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class SellIntroFragment : Fragment(), DialogFlow.FlowHost {

    private val tierService: TierService by scopedInject()
    private val coincore: Coincore by scopedInject()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.sell_intro_fragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        compositeDisposable += tierService.tiers()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = {
                if (it.isApprovedFor(KycTierLevel.GOLD)) {
                    renderKycedUserUi()
                } else
                    renderNonKycedUserUi()
            }, onError = {})
    }

    private fun renderKycedUserUi() {
        kyc_benefits.gone()
        intro_header_parent.visible()

        compositeDisposable += supportedCryptoCurrencies()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { supportedCryptos ->
                accounts_list.initialise(
                    coincore.allWallets().map {
                        it.accounts.filter { account ->
                            account is CustodialTradingAccount &&
                                    supportedCryptos.contains(account.asset)
                        }
                    },
                    status = ::statusDecorator
                )
                accounts_list.onAccountSelected = { account ->
                    (account as? CryptoAccount)?.let {
                        startSellFlow(it)
                    }
                }
            }, onError = {})
    }

    private fun statusDecorator(account: BlockchainAccount): Single<AccountDecorator> =
        Single.just(
            object : AccountDecorator {
                override val enabled: Boolean
                    get() = account.isFunded
                override val status: String
                    get() = ""
            }
        )

    private fun startSellFlow(it: CryptoAccount) {
        SendFlow(
            sourceAccount = it,
            action = AssetAction.Sell
        ).apply {
            startFlow(
                fragmentManager = childFragmentManager,
                host = this@SellIntroFragment
            )
        }
    }

    private fun supportedCryptoCurrencies(): Single<List<CryptoCurrency>> {
        val availableFiats = custodialWalletManager.getSupportedFundsFiats(currencyPrefs.selectedFiatCurrency, true)
        return custodialWalletManager.getSupportedBuySellCryptoCurrencies()
            .zipWith(availableFiats) { supportedPairs, availableFiats ->
                supportedPairs.pairs.filter { availableFiats.contains(it.fiatCurrency) }
                    .map { it.cryptoCurrency }
            }
    }

    private fun renderNonKycedUserUi() {
        kyc_benefits.visible()
        accounts_list.gone()
        intro_header_parent.gone()
        kyc_benefits.initWithBenefits(
            benefits = listOf(
                VerifyIdentityBenefit(
                    getString(R.string.sell_intro_kyc_title_1),
                    getString(R.string.sell_intro_kyc_subtitle_1)
                ), VerifyIdentityBenefit(
                    getString(R.string.sell_intro_kyc_title_2),
                    getString(R.string.sell_intro_kyc_subtitle_2)
                ),
                VerifyIdentityBenefit(
                    getString(R.string.sell_intro_kyc_title_3),
                    getString(R.string.sell_intro_kyc_subtitle_3)
                )
            ),
            title = getString(R.string.sell_crypto),
            description = getString(R.string.sell_crypto_subtitle),
            icon = R.drawable.ic_cart,
            secondaryButton = ButtonOptions(false) {},
            primaryButton = ButtonOptions(true) {},
            showSheetIndicator = false
        )
    }

    companion object {
        fun newInstance() = SellIntroFragment()
    }

    override fun onFlowFinished() {
    }
}