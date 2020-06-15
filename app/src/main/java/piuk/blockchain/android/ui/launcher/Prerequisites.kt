package piuk.blockchain.android.ui.launcher

import com.blockchain.logging.CrashLogger
import com.blockchain.swap.nabu.datamanagers.NabuUserRepository
import com.google.gson.Gson
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.data.api.ReceiveAddresses
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.swipetoreceive.AddressGenerator
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber

class Prerequisites(
    private val metadataManager: MetadataManager,
    private val settingsDataManager: SettingsDataManager,
    private val coincore: Coincore,
    private val crashLogger: CrashLogger,
    private val dynamicFeeCache: DynamicFeeCache,
    private val feeDataManager: FeeDataManager,
    private val simpleBuySync: SimpleBuySyncFactory,
    private val walletApi: WalletApi,
    private val payloadDataManager: PayloadDataManager,
    private val addressGenerator: AddressGenerator,
    private val rxBus: RxBus,
    private val nabuUserRepository: NabuUserRepository
) {

    fun initMetadataAndRelatedPrerequisites(): Completable =
        metadataManager.attemptMetadataSetup().logOnError(METADATA_ERROR_MESSAGE)
            .then { feesCompletable().logOnError(FEES_ERROR) }
            .then { simpleBuySync.performSync().logAndCompleteOnError(SIMPLE_BUY_SYNC) }
            .then { coincore.init().logOnError(COINCORE_INIT) }
            .then { generateAndUpdateReceiveAddresses().logAndCompleteOnError(RECEIVE_ADDRESSES) }
            .then { nabuUserRepository.fetchUser().ignoreElements() }
            .doOnComplete {
                rxBus.emitEvent(MetadataEvent::class.java, MetadataEvent.SETUP_COMPLETE)
            }.subscribeOn(Schedulers.io())

    private fun Completable.logOnError(tag: String): Completable =
        this.doOnError {
            crashLogger.logException(
                CustomLogMessagedException(tag, it)
            )
        }

    private fun Completable.logAndCompleteOnError(tag: String): Completable =
        this.logOnError(tag).onErrorComplete()

    private fun generateAndUpdateReceiveAddresses(): Completable =
        addressGenerator.generateAddresses().then {
            walletApi.submitCoinReceiveAddresses(payloadDataManager.guid, payloadDataManager.sharedKey,
                coinReceiveAddresses()
            ).ignoreElements()
        }

    private fun coinReceiveAddresses(): String {
        val coinAddresses = listOf(
            ReceiveAddresses("BTC", addressGenerator.getBitcoinReceiveAddresses()),
            ReceiveAddresses("BCH", addressGenerator.getBitcoinCashReceiveAddresses()),
            ReceiveAddresses("ETH", listOf(addressGenerator.getEthReceiveAddress()))
        )
        return Gson().toJson(coinAddresses)
    }

    fun initSettings(guid: String, sharedKey: String): Observable<Settings> =
        settingsDataManager.initSettings(
            guid,
            sharedKey
        )

    private fun feesCompletable(): Completable =
        feeDataManager.btcFeeOptions
            .doOnNext { dynamicFeeCache.btcFeeOptions = it }
            .ignoreElements()
            .andThen(feeDataManager.ethFeeOptions
                .doOnNext { dynamicFeeCache.ethFeeOptions = it }
                .ignoreElements()
            )
            .andThen(feeDataManager.bchFeeOptions
                .doOnNext { dynamicFeeCache.bchFeeOptions = it }
                .ignoreElements()

            )
            .subscribeOn(Schedulers.io())
            .doOnComplete { Timber.d("Wave!!") }

    fun decryptAndSetupMetadata(secondPassword: String) = metadataManager.decryptAndSetupMetadata(
        secondPassword
    )

    companion object {
        private const val METADATA_ERROR_MESSAGE = "metadata_init"
        private const val FEES_ERROR = "fees_init"
        private const val SIMPLE_BUY_SYNC = "simple_buy_sync"
        private const val COINCORE_INIT = "coincore_init"
        private const val RECEIVE_ADDRESSES = "receive_addresses"
    }
}