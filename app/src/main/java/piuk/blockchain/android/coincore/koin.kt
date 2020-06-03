package piuk.blockchain.android.coincore

import com.blockchain.koin.paxAccount
import com.blockchain.koin.payloadScopeQualifier
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
                paxAccount = get(paxAccount),
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
                btcTokens = get(),
                bchTokens = get(),
                ethTokens = get(),
                xlmTokens = get(),
                paxTokens = get(),
                stxTokens = get(),
                defaultLabels = get()
            )
        }

        scoped {
            AssetActivityRepo(
                coincore = get(),
                rxBus = get()
            )
        }
    }
}
