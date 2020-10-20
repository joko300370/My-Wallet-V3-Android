package piuk.blockchain.android.coincore

import com.blockchain.koin.paxAccount
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.usdtAccount
import info.blockchain.balance.CryptoCurrency
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.coincore.alg.AlgoAsset
import piuk.blockchain.android.coincore.bch.BchAsset
import piuk.blockchain.android.coincore.btc.BtcAsset
import piuk.blockchain.android.coincore.erc20.pax.PaxAsset
import piuk.blockchain.android.coincore.erc20.usdt.UsdtAsset
import piuk.blockchain.android.coincore.eth.EthAsset
import piuk.blockchain.android.coincore.fiat.FiatAsset
import piuk.blockchain.android.coincore.impl.TxProcessorFactory
import piuk.blockchain.android.coincore.stx.StxAsset
import piuk.blockchain.android.coincore.xlm.XlmAsset
import piuk.blockchain.android.repositories.AssetActivityRepository

val coincoreModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            StxAsset(
                payloadManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                crashLogger = get(),
                pitLinking = get(),
                labels = get(),
                tiersService = get(),
                environmentConfig = get()
            )
        }

        scoped {
            BtcAsset(
                exchangeRates = get(),
                sendDataManager = get(),
                feeDataManager = get(),
                environmentConfig = get(),
                historicRates = get(),
                currencyPrefs = get(),
                payloadManager = get(),
                custodialManager = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get(),
                tiersService = get(),
                walletPreferences = get()
            )
        }

        scoped {
            BchAsset(
                payloadManager = get(),
                bchDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                crashLogger = get(),
                stringUtils = get(),
                custodialManager = get(),
                environmentSettings = get(),
                feeDataManager = get(),
                sendDataManager = get(),
                pitLinking = get(),
                labels = get(),
                tiersService = get(),
                walletPreferences = get()
            )
        }

        scoped {
            XlmAsset(
                payloadManager = get(),
                xlmDataManager = get(),
                xlmFeesFetcher = get(),
                walletOptionsDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get(),
                tiersService = get(),
                environmentConfig = get(),
                walletPreferences = get()
            )
        }

        scoped {
            EthAsset(
                payloadManager = get(),
                ethDataManager = get(),
                feeDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                walletPrefs = get(),
                crashLogger = get(),
                custodialManager = get(),
                pitLinking = get(),
                labels = get(),
                tiersService = get(),
                environmentConfig = get()
            )
        }

        scoped {
            PaxAsset(
                payloadManager = get(),
                paxAccount = get(paxAccount),
                feeDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get(),
                tiersService = get(),
                environmentConfig = get(),
                walletPreferences = get()
            )
        }

        scoped {
            AlgoAsset(
                payloadManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get(),
                tiersService = get(),
                environmentConfig = get()
            )
        }

        scoped {
            FiatAsset(
                labels = get(),
                assetBalancesRepository = get(),
                exchangeRateDataManager = get(),
                custodialWalletManager = get(),
                tierService = get(),
                currencyPrefs = get()
            )
        }

        scoped {
            UsdtAsset(
                payloadManager = get(),
                usdtAccount = get(usdtAccount),
                feeDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                crashLogger = get(),
                labels = get(),
                pitLinking = get(),
                tierService = get(),
                environmentConfig = get(),
                walletPreferences = get()
            )
        }

        scoped {
            Coincore(
                payloadManager = get(),
                fiatAsset = get<FiatAsset>(),
                assetMap = mapOf(
                    CryptoCurrency.BTC to get<BtcAsset>(),
                    CryptoCurrency.BCH to get<BchAsset>(),
                    CryptoCurrency.ETHER to get<EthAsset>(),
                    CryptoCurrency.XLM to get<XlmAsset>(),
                    CryptoCurrency.PAX to get<PaxAsset>(),
                    CryptoCurrency.STX to get<StxAsset>(),
                    CryptoCurrency.ALGO to get<AlgoAsset>(),
                    CryptoCurrency.USDT to get<UsdtAsset>()
                ),
                txProcessorFactory = get(),
                defaultLabels = get(),
                crashLogger = get()
            )
        }

        scoped {
            TxProcessorFactory(
                bitPayManager = get(),
                exchangeRates = get(),
                walletManager = get(),
                walletPrefs = get(),
                analytics = get()
            )
        }

        scoped {
            AssetActivityRepository(
                coincore = get(),
                rxBus = get()
            )
        }

        scoped {
            AddressFactoryImpl(
                coincore = get()
            )
        }.bind(AddressFactory::class)
    }
}
