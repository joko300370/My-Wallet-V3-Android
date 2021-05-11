package com.blockchain.nabu.datamanagers.repositories

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.rx.ParameteredTimedCacheRequest
import io.reactivex.Single
import timber.log.Timber
import java.math.BigInteger

class WithdrawLocksRepository(custodialWalletManager: CustodialWalletManager) {
    private val cache = ParameteredTimedCacheRequest<Triple<PaymentMethodType, String, String>, BigInteger>(
        cacheLifetimeSeconds = 100L,
        refreshFn = { (paymentMethodType, currency, productType) ->
            custodialWalletManager.fetchWithdrawLocksTime(paymentMethodType, currency, productType)
                .doOnSuccess { it1 -> Timber.d("Withdrawal lock: $it1") }
        }
    )

    fun getWithdrawLockTypeForPaymentMethod(
        paymentMethodType: PaymentMethodType,
        fiatCurrency: String
    ): Single<BigInteger> =
        cache.getCachedSingle(Triple(paymentMethodType, fiatCurrency, "SIMPLEBUY"))
            .onErrorReturn { BigInteger.ZERO }
}