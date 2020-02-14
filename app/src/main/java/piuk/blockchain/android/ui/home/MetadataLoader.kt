package piuk.blockchain.android.ui.home

import com.blockchain.logging.CrashLogger
import com.blockchain.swap.shapeshift.ShapeShiftDataManager
import info.blockchain.wallet.payload.PayloadManagerWiper
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import java.util.concurrent.atomic.AtomicBoolean

// TODO:
// This class is temporary - the aim is to do the metadata init as soon as the wallet decryption is complete.
// BUt for now, we're just moving the init code out of the MainPresenter into this object.
class MetadataLoader(
    private val metadataManager: MetadataManager,
    private val payloadManagerWiper: PayloadManagerWiper,
    private val paxAccount: Erc20Account,
    private val buyDataManager: BuyDataManager,
    private val shapeShiftDataManager: ShapeShiftDataManager,
    private val dynamicFeeCache: DynamicFeeCache,
    private val feeDataManager: FeeDataManager,
    private val accessState: AccessState,
    private val appUtil: AppUtil,
    private val rxBus: RxBus,
    private val crashLogger: CrashLogger

) {
    private var _isLoaded = AtomicBoolean(false)

    val isLoaded: Boolean
        get() = _isLoaded.get()

    fun loader(): Single<Boolean> =
        if (_isLoaded.get())
            Single.just(false)
        else
            metadataManager
                .attemptMetadataSetup()
                .andThen(shapeShiftCompletable())
                .andThen(feesCompletable())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    _isLoaded.set(true)
                    rxBus.emitEvent(MetadataEvent::class.java, MetadataEvent.SETUP_COMPLETE)
                }
                .toSingleDefault(true)

    private fun shapeShiftCompletable(): Completable {
        return shapeShiftDataManager.initShapeshiftTradeData()
            .onErrorComplete()
            .doOnError { throwable ->
                crashLogger.logException(throwable, "Failed to load shape shift trades")
            }
    }

    private fun feesCompletable(): Completable =
        feeDataManager.btcFeeOptions
            .doOnNext { dynamicFeeCache.btcFeeOptions = it }
            .ignoreElements()
            .onErrorComplete()
            .andThen(feeDataManager.ethFeeOptions
                .doOnNext { dynamicFeeCache.ethFeeOptions = it }
                .ignoreElements()
                .onErrorComplete()
            )
            .andThen(feeDataManager.bchFeeOptions
                .doOnNext { dynamicFeeCache.bchFeeOptions = it }
                .ignoreElements()
                .onErrorComplete()
            )
            .subscribeOn(Schedulers.io())

    fun unload() {
        payloadManagerWiper.wipe()
        accessState.logout()
        accessState.unpairWallet()
        appUtil.restartApp(LauncherActivity::class.java)
        accessState.clearPin()
        buyDataManager.wipe()
        paxAccount.clear()
        _isLoaded.set(false)
    }
}