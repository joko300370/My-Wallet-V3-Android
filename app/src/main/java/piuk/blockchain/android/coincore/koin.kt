package piuk.blockchain.android.coincore

import org.koin.dsl.module.applicationContext

val coincoreModule = applicationContext {

    context("Payload") {

        bean {
            STXTokens(
                rxBus = get(),
                payloadManager = get(),
                currencyPrefs = get(),
                custodialWalletManager = get()
            )
        }

        bean {
            BTCTokens(
                payloadManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                payloadDataManager = get(),
                rxBus = get(),
                custodialWalletManager = get()
            )
        }

        bean {
            BCHTokens(
                bchDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                rxBus = get(),
                crashLogger = get(),
                stringUtils = get(),
                custodialWalletManager = get()
            )
        }

        bean {
            XLMTokens(
                rxBus = get(),
                xlmDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialWalletManager = get()
            )
        }

        bean {
            ETHTokens(
                ethDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                rxBus = get(),
                crashLogger = get(),
                stringUtils = get(),
                custodialWalletManager = get()
            )
        }

        bean {
            PAXTokens(
                rxBus = get(),
                erc20Account = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                custodialWalletManager = get(),
                stringUtils = get()
            )
        }

        bean {
            AssetTokenLookup(
                btcTokens = get(),
                bchTokens = get(),
                ethTokens = get(),
                xlmTokens = get(),
                paxTokens = get(),
                stxTokens = get()
            )
        }
    }
}
