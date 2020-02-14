package piuk.blockchain.android.ui.buysell.launcher

import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber

class BuySellLauncherPresenter(
    private val kycStatusHelper: KycStatusHelper,
    private val exchangeService: ExchangeService
) : BasePresenter<BuySellLauncherView>() {

    override fun onViewReady() {
        compositeDisposable +=
            Singles.zip(
                exchangeService.getCoinifyData()
                    .switchIfEmpty(Single.just(CoinifyData(0, ""))),
                kycStatusHelper.getKyc2TierStatus()
            ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.displayProgressDialog() }
                .doAfterTerminate { view.dismissProgressDialog() }
                .doOnError(Timber::e)
                .subscribeBy(
                    onError = {
                        view.showErrorToast(R.string.buy_sell_launcher_error)
                        view.finishPage()
                    },
                    onSuccess = {
                        val coinifyData = it.first
                        val tierState = it.second
                        when (tierState) {
                            Kyc2TierState.Tier2Approved -> {
                                if (coinifyData.user != 0) {
                                    view.onStartCoinifyOverview()
                                } else {
                                    view.onStartCoinifyOptIn()
                                }
                            }
                            Kyc2TierState.Tier1Pending,
                            Kyc2TierState.Tier1Failed,
                            Kyc2TierState.Tier1Approved -> {
                                view.onStartCoinifyOptIn()
                            }
                            Kyc2TierState.Hidden,
                            Kyc2TierState.Locked -> view.onStartCoinifySignUp()
                            Kyc2TierState.Tier2InPending,
                            Kyc2TierState.Tier2Failed -> view.showPendingVerificationView()
                        }
                    }
                )
    }
}