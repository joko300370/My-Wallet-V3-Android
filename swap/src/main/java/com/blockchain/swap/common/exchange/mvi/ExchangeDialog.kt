package com.blockchain.swap.common.exchange.mvi

import com.blockchain.swap.nabu.service.Fix
import com.blockchain.swap.nabu.service.Quote
import com.blockchain.swap.nabu.service.isFiat
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.times
import info.blockchain.balance.withMajorValue
import io.reactivex.Observable

/**
 * The dialog is the conversation between the User and the System.
 */
class ExchangeDialog(intents: Observable<ExchangeIntent>, initial: ExchangeViewModel) {

    val viewStates: Observable<ExchangeViewState> =
        intents.scan(initial.toInternalState()) { previousState, intent ->
            when (intent) {
                is LockQuoteIntent -> previousState.mapLock(intent)
                is SimpleFieldUpdateIntent -> previousState.map(intent)
                is SwapIntent -> previousState.mapSwap()
                is QuoteIntent -> previousState.mapQuote(intent)
                is ChangeCryptoFromAccount -> previousState.mapNewFromAccount(intent)
                is ChangeCryptoToAccount -> previousState.mapNewToAccount(intent)
                is ToggleFiatCryptoIntent -> previousState.toggleFiatCrypto()
                is ToggleFromToIntent -> previousState.toggleFromTo()
                is SetFixIntent -> previousState.mapSetFix(intent)
                is SetTradeLimits -> previousState.mapTradeLimits(intent)
                is ApplyMinimumLimit -> previousState.applyLimit(previousState.minTradeLimit)
                is ApplyMaximumLimit -> previousState.applyLimit(previousState.maxTrade)
                is ApplyMaxSpendable -> previousState.applyMaxSpendable(previousState.maxSpendable)
                is FiatExchangeRateIntent -> previousState.setFiatRate(intent.c2fRate)
                is ExchangeRateIntent -> previousState.setExchangeFiatRates(intent.prices)
                is SpendableValueIntent -> previousState.setSpendable(intent.cryptoValue)
                is ClearQuoteIntent -> previousState.clearQuote()
                is SetUserTier -> previousState.copy(userTier = intent.tier)
                is SetTierLimit -> previousState.mapTierLimits(intent)
                is EnoughFeesLimit -> previousState.setHasEthEnoughFees(intent)
                is IsUserEligiableForFreeEthIntent -> previousState.setIsPowerPaxTagged(intent)
                is SetEthTransactionInFlight -> previousState.setHasEthInFlight(intent)
            }
        }

    val viewModels: Observable<ExchangeViewModel> =
        viewStates.map {
            it.toViewModel()
        }
}

internal fun ExchangeViewModel.toInternalState(): ExchangeViewState {
    return ExchangeViewState(
        fromAccount = fromAccount,
        toAccount = toAccount,
        fix = fixedField,
        upToDate = true,
        fromCrypto = from.cryptoValue,
        fromFiat = from.fiatValue,
        toFiat = to.fiatValue,
        toCrypto = to.cryptoValue,
        latestQuote = latestQuote
    )
}

enum class QuoteValidity {
    Valid,
    NoQuote,
    MissMatch,
    UnderMinTrade,
    NotEnoughFees,
    OverMaxTrade,
    OverTierLimit,
    OverUserBalance,
    HasTransactionInFlight,
}

data class ExchangeViewState(
    val fromAccount: AccountReference,
    val toAccount: AccountReference,
    val fix: Fix,
    val upToDate: Boolean,
    val fromCrypto: CryptoValue,
    val toCrypto: CryptoValue,
    val fromFiat: FiatValue,
    val toFiat: FiatValue,
    val latestQuote: Quote?,
    val minTradeLimit: FiatValue? = null,
    val maxTradeLimit: FiatValue? = null,
    val maxTierLimit: FiatValue? = null,
    val exchangePrices: List<ExchangeRate.CryptoToFiat> = emptyList(),
    val c2fRate: ExchangeRate.CryptoToFiat? = null,
    val maxSpendable: CryptoValue? = null,
    val decimalCursor: Int = 0,
    val userTier: Int = 0,
    val isPowerPaxTagged: Boolean = false,
    val hasEnoughEthFees: Boolean = true,
    val hasEthTransactionPending: Boolean = false,
    val quoteLocked: Boolean = false
) {
    private val maxTradeOrTierLimit: FiatValue?
        get() {
            if (maxTradeLimit != null && maxTierLimit != null) {
                return if (maxTradeLimit < maxTierLimit) maxTradeLimit else maxTierLimit
            }
            return maxTradeLimit ?: maxTierLimit
        }

    val lastUserValue: Money =
        when (fix) {
            Fix.BASE_FIAT -> fromFiat
            Fix.BASE_CRYPTO -> fromCrypto
            Fix.COUNTER_FIAT -> toFiat
            Fix.COUNTER_CRYPTO -> toCrypto
        }

    val maxTrade: Money?
        get() {
            val limit = maxTradeOrTierLimit
            val maxSpendableFiat = try { maxSpendable * c2fRate } catch (e: Throwable) { null } ?: return limit
            if (maxSpendableFiat.currencyCode != fromFiat.currencyCode) return limit
            if (limit == null) return maxSpendableFiat
            if (limit.currencyCode != maxSpendableFiat.currencyCode) return null
            return if (maxSpendableFiat > limit) {
                limit
            } else {
                maxSpendable
            }
        }

    private val fixedMoneyValue: Money
        get() = when (fix) {
            Fix.BASE_CRYPTO -> fromCrypto
            Fix.COUNTER_CRYPTO -> toCrypto
            Fix.BASE_FIAT -> fromFiat
            Fix.COUNTER_FIAT -> toFiat
        }

    fun isValid() = validity() == QuoteValidity.Valid

    fun validity(): QuoteValidity {
        if (latestQuote == null) return QuoteValidity.NoQuote
        if (!quoteMatchesFixAndValue(latestQuote)) return QuoteValidity.MissMatch
        if (!enoughFundsIfKnown(latestQuote)) return QuoteValidity.OverUserBalance
        if (exceedsTheFiatLimit(latestQuote, maxTradeLimit)) return QuoteValidity.OverMaxTrade
        if (exceedsTheFiatLimit(latestQuote, maxTierLimit)) return QuoteValidity.OverTierLimit
        if (underTheFiatLimit(latestQuote, minTradeLimit)) return QuoteValidity.UnderMinTrade
        if (isBlockedEthTransactionInFlight(latestQuote)) return QuoteValidity.HasTransactionInFlight
        if (!hasEnoughEthFees) return QuoteValidity.NotEnoughFees
        return QuoteValidity.Valid
    }

    private fun exceedsTheFiatLimit(latestQuote: Quote, maxTradeLimit: FiatValue?): Boolean {
        if (maxTradeLimit == null) return false
        return latestQuote.from.fiatValue > maxTradeLimit
    }

    private fun underTheFiatLimit(latestQuote: Quote, minTradeLimit: FiatValue?): Boolean {
        if (minTradeLimit == null) return false
        return latestQuote.from.fiatValue < minTradeLimit
    }

    private fun quoteMatchesFixAndValue(latestQuote: Quote) =
        latestQuote.fix == fix && latestQuote.fixValue == fixedMoneyValue

    private fun enoughFundsIfKnown(latestQuote: Quote): Boolean {
        if (maxSpendable == null) return true
        if (maxSpendable.currency != latestQuote.from.cryptoValue.currency) return true
        return maxSpendable >= latestQuote.from.cryptoValue
    }

    private fun isBlockedEthTransactionInFlight(latestQuote: Quote): Boolean {
        return (latestQuote.isEtheriumTransaction()) && hasEthTransactionPending
    }
}

private fun ExchangeViewState.mapTradeLimits(intent: SetTradeLimits): ExchangeViewState {
    if (intent.min.currencyCode != fromFiat.currencyCode) return this
    return copy(
        minTradeLimit = intent.min,
        maxTradeLimit = intent.max
    )
}

private fun ExchangeViewState.mapTierLimits(intent: SetTierLimit): ExchangeViewState {
    if (intent.availableOnTier.currencyCode != fromFiat.currencyCode) return this
    return copy(
        maxTierLimit = intent.availableOnTier
    )
}

private fun ExchangeViewState.mapLock(intent: LockQuoteIntent): ExchangeViewState {
    return copy(quoteLocked = intent.lockQuote)
}

private fun ExchangeViewState.resetToZero(): ExchangeViewState {
    return copy(
        fromFiat = fromFiat.toZero(),
        toFiat = toFiat.toZero(),
        fromCrypto = CryptoValue.zero(fromAccount.cryptoCurrency),
        toCrypto = CryptoValue.zero(toAccount.cryptoCurrency),
        upToDate = false
    )
}

private fun ExchangeViewState.mapSetFix(intent: SetFixIntent): ExchangeViewState {
    return copy(fix = intent.fix)
}

fun ExchangeViewState.toViewModel(): ExchangeViewModel {
    return ExchangeViewModel(
        fromAccount = fromAccount,
        toAccount = toAccount,
        from = Value(
            cryptoValue = fromCrypto,
            fiatValue = fromFiat,
            cryptoMode = mode(fix, Fix.BASE_CRYPTO, fromCrypto, upToDate),
            fiatMode = mode(fix, Fix.BASE_FIAT, fromFiat, upToDate)
        ),
        to = Value(
            cryptoValue = toCrypto,
            fiatValue = toFiat,
            cryptoMode = mode(fix, Fix.COUNTER_CRYPTO, toCrypto, upToDate),
            fiatMode = mode(fix, Fix.COUNTER_FIAT, toFiat, upToDate)
        ),
        latestQuote = latestQuote,
        isValid = isValid()
    )
}

private fun ExchangeViewState.map(intent: SimpleFieldUpdateIntent): ExchangeViewState {
    return when (fix) {
        Fix.BASE_FIAT -> copy(fromFiat = FiatValue.fromMajor(fromFiat.currencyCode, intent.userValue), upToDate = false)
        Fix.BASE_CRYPTO -> copy(fromCrypto = fromCrypto.currency.withMajorValue(intent.userValue), upToDate = false)
        Fix.COUNTER_FIAT -> copy(toFiat = FiatValue.fromMajor(toFiat.currencyCode, intent.userValue), upToDate = false)
        Fix.COUNTER_CRYPTO -> copy(toCrypto = toCrypto.currency.withMajorValue(intent.userValue), upToDate = false)
    }.copy(decimalCursor = intent.decimalCursor)
}

private fun ExchangeViewState.toggleFiatCrypto() = copy(fix = fix.toggleFiatCrypto())

private fun ExchangeViewState.toggleFromTo() = copy(fix = fix.toggleFromTo())

private fun ExchangeViewState.setIsPowerPaxTagged(intent: IsUserEligiableForFreeEthIntent): ExchangeViewState {
    return copy(isPowerPaxTagged = intent.isEligiable)
}

private fun ExchangeViewState.clearQuote() =
    copy(
        latestQuote = null,
        fromFiat = if (fix != Fix.BASE_FIAT) fromFiat.toZero() else fromFiat,
        toFiat = if (fix != Fix.COUNTER_FIAT) toFiat.toZero() else toFiat,
        fromCrypto = if (fix != Fix.BASE_CRYPTO) fromCrypto.toZero() else fromCrypto,
        toCrypto = if (fix != Fix.COUNTER_CRYPTO) toCrypto.toZero() else toCrypto
    )

private fun ExchangeViewState.setSpendable(cryptoValue: CryptoValue): ExchangeViewState {
    if (cryptoValue.currency != fromAccount.cryptoCurrency) {
        return this
    }
    return copy(maxSpendable = cryptoValue)
}

private fun ExchangeViewState.setFiatRate(c2fRate: ExchangeRate.CryptoToFiat): ExchangeViewState {
    if (c2fRate.to != fromFiat.currencyCode || c2fRate.from != fromAccount.cryptoCurrency) {
        return this
    }
    return copy(c2fRate = c2fRate)
}

private fun ExchangeViewState.setExchangeFiatRates(exchangePrices: List<ExchangeRate.CryptoToFiat>): ExchangeViewState {
    return copy(exchangePrices = exchangePrices)
}

private fun ExchangeViewState.setHasEthEnoughFees(intent: EnoughFeesLimit): ExchangeViewState {
    return copy(hasEnoughEthFees = intent.hasEnoughForFess)
}

private fun ExchangeViewState.setHasEthInFlight(intent: SetEthTransactionInFlight): ExchangeViewState {
    return copy(hasEthTransactionPending = intent.ethInFlight)
}

private fun ExchangeViewState.applyMaxSpendable(cryptoValue: CryptoValue?): ExchangeViewState {
    return cryptoValue?.let {
        copy(fix = Fix.BASE_CRYPTO, fromCrypto = cryptoValue)
    } ?: this
}

private fun ExchangeViewState.applyLimit(tradeLimit: Money?) =
    when (tradeLimit) {
        null -> this
        is FiatValue -> copy(fix = Fix.BASE_FIAT, fromFiat = tradeLimit)
        is CryptoValue -> copy(fix = Fix.BASE_CRYPTO, fromCrypto = tradeLimit)
        else -> this
    }

private fun ExchangeViewState.mapNewFromAccount(intent: ChangeCryptoFromAccount) =
    changeAccounts(
        newFrom = intent.from,
        newTo = if (intent.from.cryptoCurrency == toAccount.cryptoCurrency) {
            fromAccount
        } else {
            toAccount
        }
    )

private fun ExchangeViewState.mapNewToAccount(intent: ChangeCryptoToAccount) =
    changeAccounts(
        newFrom = if (intent.to.cryptoCurrency == fromAccount.cryptoCurrency) {
            toAccount
        } else {
            fromAccount
        },
        newTo = intent.to
    )

private fun ExchangeViewState.mapSwap() =
    changeAccounts(
        newFrom = toAccount,
        newTo = fromAccount
    )

private fun ExchangeViewState.changeAccounts(
    newFrom: AccountReference,
    newTo: AccountReference
) = copy(fromAccount = newFrom, toAccount = newTo).resetToZeroKeepingUserFiat()

private fun ExchangeViewState.resetToZeroKeepingUserFiat(): ExchangeViewState =
    resetToZero()
        .copy(
            fromFiat = if (fix.isFiat) this.fromFiat else this.fromFiat.toZero(),
            toFiat = if (fix.isFiat) this.toFiat else this.fromFiat.toZero()
        )

private fun ExchangeViewState.mapQuote(intent: QuoteIntent) =
    if (!quoteLocked &&
        intent.quote.fix == fix &&
        intent.quote.fixValue == lastUserValue &&
        fromCurrencyMatch(intent) &&
        toCurrencyMatch(intent)
    ) {
        copy(
            fromCrypto = intent.quote.from.cryptoValue,
            fromFiat = if (fix == Fix.BASE_CRYPTO) calculateFiatValue(
                intent.quote,
                fromCrypto.currency,
                fromFiat.currencyCode,
                fromCrypto
            ) else intent.quote.from.fiatValue,
            toCrypto = intent.quote.to.cryptoValue,
            toFiat = intent.quote.to.fiatValue,
            latestQuote = intent.quote,
            upToDate = true)
    } else {
        this
    }

private fun ExchangeViewState.fromCurrencyMatch(intent: QuoteIntent) =
    currencyMatch(intent.quote.from, fromCrypto, fromFiat)

private fun ExchangeViewState.toCurrencyMatch(intent: QuoteIntent) =
    currencyMatch(intent.quote.to, toCrypto, toFiat)

private fun Quote.isEtheriumTransaction(): Boolean {
    return when (from.cryptoValue.currency) {
        CryptoCurrency.PAX, CryptoCurrency.ETHER -> true
        else -> false
    }
}

private fun Fix.toggleFiatCrypto() =
    when (this) {
        Fix.BASE_FIAT -> Fix.BASE_CRYPTO
        Fix.BASE_CRYPTO -> Fix.BASE_FIAT
        Fix.COUNTER_FIAT -> Fix.COUNTER_CRYPTO
        Fix.COUNTER_CRYPTO -> Fix.COUNTER_FIAT
    }

private fun Fix.toggleFromTo() =
    when (this) {
        Fix.BASE_FIAT -> Fix.COUNTER_FIAT
        Fix.BASE_CRYPTO -> Fix.COUNTER_CRYPTO
        Fix.COUNTER_FIAT -> Fix.BASE_FIAT
        Fix.COUNTER_CRYPTO -> Fix.BASE_CRYPTO
    }

private fun currencyMatch(
    quote: Quote.Value,
    vmValue: CryptoValue,
    vmFiatValue: FiatValue
) = quote.fiatValue.currencyCode == vmFiatValue.currencyCode && quote.cryptoValue.currency == vmValue.currency

private fun mode(
    fieldEntered: Fix,
    field: Fix,
    value: Money,
    upToDate: Boolean = true
): Value.Mode {
    return when {
        fieldEntered == field -> Value.Mode.UserEntered
        value.isPositive -> if (upToDate) Value.Mode.UpToDate else Value.Mode.OutOfDate
        else -> Value.Mode.OutOfDate
    }
}

private fun calculateFiatValue(quote: Quote, cryptoCurrency: CryptoCurrency, fiatCode: String, fromCrypto: CryptoValue):
    FiatValue = ExchangeRate.CryptoToFiat(cryptoCurrency, fiatCode, quote.baseToFiatRate)
    .applyRate(fromCrypto) ?: FiatValue.zero(fiatCode)
