package piuk.blockchain.android.coincore

import com.blockchain.koin.payloadScopeQualifier
import info.blockchain.balance.CryptoCurrency
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.coincore.bch.BchTokens
import piuk.blockchain.android.coincore.btc.BtcTokens
import piuk.blockchain.android.coincore.eth.EthTokens
import piuk.blockchain.android.coincore.impl.AssetActivityRepo
import piuk.blockchain.android.coincore.pax.PaxTokens
import piuk.blockchain.android.coincore.stx.StxTokens
import piuk.blockchain.android.coincore.xlm.XlmTokens

val coincoreModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            StxTokens(
                rxBus = get(),
                payloadManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                crashLogger = get(),
                pitLinking = get(),
                labels = get()
            )
        }

        scoped {
            BtcTokens(
                exchangeRates = get(),
                environmentSettings = get(),
                historicRates = get(),
                currencyPrefs = get(),
                payloadDataManager = get(),
                rxBus = get(),
                custodialManager = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get()
            )
        }

        scoped {
            BchTokens(
                bchDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                rxBus = get(),
                crashLogger = get(),
                stringUtils = get(),
                custodialManager = get(),
                environmentSettings = get(),
                pitLinking = get(),
                labels = get()
            )
        }

        scoped {
            XlmTokens(
                rxBus = get(),
                xlmDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get()
            )
        }

        scoped {
            EthTokens(
                ethDataManager = get(),
                feeDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                rxBus = get(),
                crashLogger = get(),
                stringUtils = get(),
                custodialManager = get(),
                pitLinking = get(),
                labels = get()
            )
        }

        scoped {
            PaxTokens(
                rxBus = get(),
                paxAccount = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                stringUtils = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get()
            )
        }

        scoped {
            Coincore(
                payloadManager = get(),
                tokenMap = mapOf(
                    CryptoCurrency.BTC to get<BtcTokens>(),
                    CryptoCurrency.BCH to get<BchTokens>(),
                    CryptoCurrency.ETHER to get<EthTokens>(),
                    CryptoCurrency.XLM to get<XlmTokens>(),
                    CryptoCurrency.PAX to get<PaxTokens>(),
                    CryptoCurrency.STX to get<StxTokens>()
                ),
                defaultLabels = get()
            )
        }

        scoped {
            AssetActivityRepo(
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
