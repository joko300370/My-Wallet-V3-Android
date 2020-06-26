package com.blockchain.koin.modules

import com.blockchain.datamanagers.BalanceCalculator
import com.blockchain.datamanagers.BalanceCalculatorImpl
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.priorityFee
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.swap.homebrew.exchange.ExchangeModel
import piuk.blockchain.android.ui.swap.homebrew.exchange.confirmation.ExchangeConfirmationPresenter
import piuk.blockchain.android.ui.swap.homebrew.exchange.history.TradeHistoryPresenter

val morphUiModule = module {

    scope(payloadScopeQualifier) {

        factory {
            ExchangeConfirmationPresenter(
                transactionExecutor = get(priorityFee),
                tradeExecutionService = get(),
                payloadDecrypt = get(),
                stringUtils = get(),
                analytics = get(),
                diagnostics = get()
            )
        }

        factory { TradeHistoryPresenter(dataManager = get(), dateUtil = get()) }

        factory { BalanceCalculatorImpl(get()) }.bind(BalanceCalculator::class)

        factory {
            ExchangeModel(quoteServiceFactory = get(),
                allAccountList = get(),
                tradeLimitService = get(),
                currentTier = get(),
                transactionExecutor = get(priorityFee),
                maximumSpendableCalculator = get(priorityFee),
                currencyPrefs = get(),
                ethDataManager = get(),
                exchangeRateDataStore = get(),
                ethEligibility = get())
        }
    }
}
