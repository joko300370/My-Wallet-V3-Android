package piuk.blockchain.android.ui.sell

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.ui.trackLoading
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.buy_intro_fragment.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import piuk.blockchain.android.ui.customviews.account.HeaderDecoration
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class BuyIntroFragment : Fragment() {

    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val compositeDisposable = CompositeDisposable()
    private val coinCore: Coincore by scopedInject()
    private val appUtil: AppUtil by inject()
    private val simpleBuyPrefs: SimpleBuyPrefs by scopedInject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.buy_intro_fragment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        compositeDisposable +=
            custodialWalletManager.getSupportedBuySellCryptoCurrencies(
                currencyPrefs.selectedFiatCurrency)
                .flatMap { pairs ->
                    Single.zip(pairs.pairs.map {
                        coinCore[it.cryptoCurrency].exchangeRate()
                    }) { t: Array<Any> ->
                        t.map {
                            it as ExchangeRate.CryptoToFiat
                        } to pairs
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .trackLoading(appUtil.activityIndicator)
                .subscribeBy(
                    onSuccess = { (exchangeRates, buyPairs) ->
                        val introHeaderView = IntroHeaderView(requireContext())
                        introHeaderView.setDetails(
                            icon = R.drawable.ic_cart,
                            label = R.string.select_crypto_you_want,
                            title = R.string.buy_with_cash)

                        rv_cryptos.addItemDecoration(
                                HeaderDecoration.with(requireContext())
                                    .parallax(0.5f)
                                    .setView(introHeaderView)
                                    .build()

                        )

                        rv_cryptos.layoutManager = LinearLayoutManager(activity)
                        rv_cryptos.adapter = BuyCryptoCurrenciesAdapter(buyPairs.pairs.map { pair ->
                            BuyCryptoItem(pair.cryptoCurrency,
                                exchangeRates.first { it.from == pair.cryptoCurrency }.price()
                            ) {
                                simpleBuyPrefs.clearState()
                                startActivity(SimpleBuyActivity.newInstance(
                                    activity as Context,
                                    pair.cryptoCurrency,
                                    launchFromNavigationBar = true,
                                    launchKycResume = false
                                )
                                )
                            }
                        })
                    }, onError = {}
                )
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = BuyIntroFragment()
    }
}

data class BuyCryptoItem(val cryptoCurrency: CryptoCurrency, val price: Money, val click: () -> Unit)