package com.blockchain.swap.koin

import com.blockchain.koin.nabu
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.swap.homebrew.QuoteWebSocketServiceFactory
import com.blockchain.swap.common.exchange.service.QuoteServiceFactory
import com.blockchain.swap.nabu.service.TradeExecutionService
import com.blockchain.swap.homebrew.HomeBrewTradeExecutionService
import com.blockchain.swap.common.trade.MorphTradeDataHistoryList
import com.blockchain.swap.common.trade.MorphTradeDataManager
import com.blockchain.swap.nabu.NabuDataManagerAdapter
import org.koin.dsl.bind
import org.koin.dsl.module

val swapModule = module {

    scope(payloadScopeQualifier) {

        factory {
            QuoteWebSocketServiceFactory(
                nabuWebSocketOptions = get(nabu),
                auth = get(),
                moshi = get(),
                okHttpClient = get()
            )
        }.bind(QuoteServiceFactory::class)

        factory {
            HomeBrewTradeExecutionService(get())
        }.bind(TradeExecutionService::class)

        factory {
            NabuDataManagerAdapter(
                nabuMarketsService = get(),
                currencyPreference = get()
            )
        }.bind(MorphTradeDataManager::class)
            .bind(MorphTradeDataHistoryList::class)
    }
}
