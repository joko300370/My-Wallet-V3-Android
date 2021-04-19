package piuk.blockchain.android.coincore

import com.blockchain.koin.aaveFeatureFlag
import com.blockchain.koin.dotFeatureFlag
import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.yfiFeatureFlag
import com.blockchain.remoteconfig.FeatureFlag
import info.blockchain.balance.CryptoCurrency
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.coincore.alg.AlgoAsset
import piuk.blockchain.android.coincore.bch.BchAsset
import piuk.blockchain.android.coincore.btc.BtcAsset
import piuk.blockchain.android.coincore.dot.PolkadotAsset
import piuk.blockchain.android.coincore.erc20.EnabledErc20FeatureFlag
import piuk.blockchain.android.coincore.erc20.Erc20Asset
import piuk.blockchain.android.coincore.eth.EthAsset
import piuk.blockchain.android.coincore.fiat.FiatAsset
import piuk.blockchain.android.coincore.fiat.LinkedBanksFactory
import piuk.blockchain.android.coincore.impl.AssetResourcesImpl
import piuk.blockchain.android.coincore.impl.OfflineAccountUpdater
import piuk.blockchain.android.coincore.impl.TxProcessorFactory
import piuk.blockchain.android.coincore.impl.txEngine.TransferQuotesEngine
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
                identity = get(),
                offlineAccounts = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            BtcAsset(
                exchangeRates = get(),
                sendDataManager = get(),
                feeDataManager = get(),
                historicRates = get(),
                currencyPrefs = get(),
                payloadManager = get(),
                custodialManager = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get(),
                walletPreferences = get(),
                offlineAccounts = get(),
                coinsWebsocket = get(),
                identity = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            BchAsset(
                payloadManager = get(),
                bchDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                crashLogger = get(),
                custodialManager = get(),
                feeDataManager = get(),
                sendDataManager = get(),
                pitLinking = get(),
                labels = get(),
                walletPreferences = get(),
                offlineAccounts = get(),
                identity = get()
            )
        }.bind(CryptoAsset::class)

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
                walletPreferences = get(),
                offlineAccounts = get(),
                identity = get()
            )
        }.bind(CryptoAsset::class)

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
                offlineAccounts = get(),
                identity = get()
            )
        }.bind(CryptoAsset::class)

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
                identity = get(),
                offlineAccounts = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            FiatAsset(
                labels = get(),
                assetBalancesRepository = get(),
                exchangeRateDataManager = get(),
                custodialWalletManager = get(),
                currencyPrefs = get()
            )
        }

        scoped {
            PolkadotAsset(
                payloadManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                crashLogger = get(),
                labels = get(),
                pitLinking = get(),
                offlineAccounts = get(),
                identity = get(),
                dotFeatureFlag = get(dotFeatureFlag)
            )
        }.bind(CryptoAsset::class)

        scoped {
            Coincore(
                payloadManager = get(),
                fiatAsset = get<FiatAsset>(),
                assetLoader = get(),
                txProcessorFactory = get(),
                defaultLabels = get(),
                crashLogger = get()
            )
        }

        scoped {
            val erc20Assets = CryptoCurrency.erc20Assets().map {
                val featureFlag: FeatureFlag = when (it) {
                    CryptoCurrency.AAVE -> get(aaveFeatureFlag)
                    CryptoCurrency.YFI -> get(yfiFeatureFlag)
                    else -> EnabledErc20FeatureFlag()
                }
                Erc20Asset(
                    asset = it,
                    featureFlag = featureFlag,
                    payloadManager = get(),
                    ethDataManager = get(),
                    feeDataManager = get(),
                    exchangeRates = get(),
                    historicRates = get(),
                    currencyPrefs = get(),
                    custodialManager = get(),
                    crashLogger = get(),
                    labels = get(),
                    pitLinking = get(),
                    offlineAccounts = get(),
                    walletPreferences = get(),
                    identity = get()
                )
            }
            val nonErc20Assets: List<CryptoAsset> = payloadScope.getAll()
            CryptoAssetLoader(
                cryptoAssets = nonErc20Assets + erc20Assets
            )
        }.bind(AssetLoader::class)

        scoped {
            AssetResourcesImpl(
                resources = get()
            )
        }.bind(AssetResources::class)

        scoped {
            TxProcessorFactory(
                bitPayManager = get(),
                exchangeRates = get(),
                walletManager = get(),
                walletPrefs = get(),
                quotesEngine = get(),
                analytics = get(),
                kycTierService = get()
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

        scoped {
            OfflineAccountUpdater(
                localCache = get(),
                payloadManager = get(),
                walletApi = get()
            )
        }

        factory {
            TransferQuotesEngine(quotesProvider = get())
        }

        factory {
            LinkedBanksFactory(
                custodialWalletManager = get()
            )
        }
    }
}