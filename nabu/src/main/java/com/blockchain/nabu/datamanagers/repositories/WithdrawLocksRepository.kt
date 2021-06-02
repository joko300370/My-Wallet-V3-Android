package com.blockchain.nabu.datamanagers.repositories

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.rx.ParameteredSingleTimedCacheRequest
import io.reactivex.Single
import timber.log.Timber
import java.math.BigInteger

class WithdrawLocksRepository(custodialWalletManager: CustodialWalletManager) {

    private val cache = ParameteredSingleTimedCacheRequest<WithdrawalData, BigInteger>(
        cacheLifetimeSeconds = 100L,
        refreshFn = { data ->
            custodialWalletManager.fetchWithdrawLocksTime(
                data.paymentMethodType, data.fiatCurrency, data.productType
            )
                .doOnSuccess { it1 -> Timber.d("Withdrawal lock: $it1") }
        }
    )

    fun getWithdrawLockTypeForPaymentMethod(
        paymentMethodType: PaymentMethodType,
        fiatCurrency: String
    ): Single<BigInteger> =
        cache.getCachedSingle(
            WithdrawalData(paymentMethodType, fiatCurrency, "SIMPLEBUY")
        )
            .onErrorReturn { BigInteger.ZERO }

    private data class WithdrawalData(
        val paymentMethodType: PaymentMethodType,
        val fiatCurrency: String,
        val productType: String
    )
}