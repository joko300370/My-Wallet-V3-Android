package com.blockchain.koin.modules

import com.blockchain.datamanagers.BalanceCalculator
import com.blockchain.datamanagers.BalanceCalculatorImpl
import com.blockchain.koin.registerDebug
import com.blockchain.morph.trade.MergingMorphTradeDataManager
import com.blockchain.morph.trade.MorphTradeDataHistoryList

import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.ui.swap.homebrew.exchange.ExchangeModel
import piuk.blockchain.android.ui.swap.homebrew.exchange.confirmation.ExchangeConfirmationPresenter
import piuk.blockchain.android.ui.swap.homebrew.exchange.history.TradeHistoryPresenter

val morphUiModule = applicationContext {

    context("Payload") {

        factory {
            ExchangeConfirmationPresenter(
                transactionExecutor = get("Priority"),
                tradeExecutionService = get(),
                payloadDecrypt = get(),
                stringUtils = get(),
                locale = get(),
                analytics = get(),
                crashLogger = get()
            )
        }

        factory("merge") {
            MergingMorphTradeDataManager(
                get("nabu"),
                get("shapeshift")
            )
        }.bind(MorphTradeDataHistoryList::class)

        factory { TradeHistoryPresenter(get("merge"), get()) }

        factory { BalanceCalculatorImpl(get()) }.bind(BalanceCalculator::class)

        context("Quotes") {

            factory {
                ExchangeModel(quoteServiceFactory = get(),
                    allAccountList = get(),
                    tradeLimitService = get(),
                    currentTier = get(),
                    transactionExecutor = get("Priority"),
                    maximumSpendableCalculator = get("Priority"),
                    currencyPrefs = get(),
                    ethDataManager = get(),
                    exchangeRateDataStore = get(),
                    ethEligibility = get())
            }
        }
    }

    apply { registerDebug() }
}
