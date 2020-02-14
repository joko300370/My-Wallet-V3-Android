package piuk.blockchain.androidbuysell.datamanagers

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.Singles
import org.bitcoinj.core.Sha256Hash
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.androidbuysell.models.WebViewLoginDetails
import piuk.blockchain.androidbuysell.services.BuyConditions
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class BuyDataManager(
    private val settingsDataManager: SettingsDataManager,
    private val authDataManager: AuthDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val buyConditions: BuyConditions,
    private val coinifyFeatureFlag: FeatureFlag,
    private val exchangeService: ExchangeService
) {
    val canBuy: Single<Boolean>
        @Synchronized get() {
            initReplaySubjects()
            return Singles.zip(isBuyRolledOut,
                isCoinifyAllowed
            ) { isBuyRolledOut, allowCoinify ->
                isBuyRolledOut && allowCoinify
            }
        }

    /**
     * Checks if buy is rolled out for user on android based on GUID. (All exchange partners)
     *
     * @return An [Observable] wrapping a boolean value
     */
    @VisibleForTesting
    internal val isBuyRolledOut: Single<Boolean> = buyConditions.walletOptionsSource
        .flatMap { walletOptions ->
            buyConditions.walletSettingsSource
                .map { isRolloutAllowed(walletOptions.rolloutPercentage) }
        }.singleOrError()

    /**
     * Checks if user has whitelisted coinify account or in valid coinify country
     *
     * @return An [Observable] wrapping a boolean value
     */
    val isCoinifyAllowed: Single<Boolean>
        get() = Observables.zip(isInCoinifyCountry,
            buyConditions.exchangeDataSource,
            coinifyFeatureFlag.enabled.toObservable()
        ) { coinifyCountry, exchangeData, coinifyEnabled ->
            coinifyEnabled && (coinifyCountry || (exchangeData.coinify?.user != 0))
        }.singleOrError()

    /**
     * Checks whether or not a user is accessing their wallet from a SEPA country.
     *
     * @return An [Observable] wrapping a boolean value
     */
    private val isInCoinifyCountry: Observable<Boolean> = buyConditions.walletOptionsSource
        .flatMap { walletOptions ->
            buyConditions.walletSettingsSource
                .map { settings ->
                    walletOptions.partners.coinify.countries.contains(settings.countryCode)
                }
        }

    val webViewLoginDetails: Observable<WebViewLoginDetails>
        get() = exchangeService.getWebViewLoginDetails()

    /**
     * Returns user's country code based on calculated value from wallet settings.
     *
     * @return An [Observable] wrapping a String value
     */
    val countryCode: Observable<String> = buyConditions.walletSettingsSource
        .map { it.countryCode }

    /**
     * ReplaySubjects will re-emit items it observed. It is safe to assumed that walletOptions and
     * the user's country code won't change during an active session.
     */
    @SuppressLint("CheckResult")
    private fun initReplaySubjects() {
        val walletOptionsStream = authDataManager.getWalletOptions()
        walletOptionsStream.subscribeWith(buyConditions.walletOptionsSource)

        val walletSettingsStream = settingsDataManager.getSettings()
        walletSettingsStream.subscribeWith(buyConditions.walletSettingsSource)

        val exchangeDataStream = exchangeService.getExchangeMetaData()
        exchangeDataStream.subscribeWith(buyConditions.exchangeDataSource)
    }

    /**
     * Checks whether or not buy/sell is allowed to be rolled out based on percentage check on
     * user's GUID.
     *
     * @return An [Observable] wrapping a boolean value
     */
    private fun isRolloutAllowed(rolloutPercentage: Double): Boolean {
        val plainGuid = payloadDataManager.wallet!!.guid.replace("-", "")

        val guidHashBytes = Sha256Hash.hash(Hex.encode(plainGuid.toByteArray()))
        val unsignedByte = guidHashBytes[0].toInt() and 0xff

        return (unsignedByte + 1.0) / 256.0 <= rolloutPercentage
    }

    fun watchPendingTrades(): Observable<String> {
        return exchangeService.watchPendingTrades()
    }

    fun reloadExchangeData() {
        exchangeService.reloadExchangeData()
    }

    fun wipe() {
        exchangeService.wipe()
    }

    /**
     * Checks whether or not supplied country code is available for Coinify trade.
     *
     * @param countryCode ISO3 country code as defined here https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
     * @return An [Observable] wrapping a boolean value
     */
    fun isInCoinifyCountry(countryCode: String): Observable<Boolean> {
        return buyConditions.walletOptionsSource
            .map { walletOptions -> walletOptions.partners.coinify.countries.contains(countryCode) }
    }
}
