package com.blockchain.koin.modules

import android.content.Context
import com.blockchain.balance.TotalBalance
import com.blockchain.koin.getActivity
import com.blockchain.kycui.settings.KycStatusHelper
import com.blockchain.sunriver.XlmTransactionSender
import com.blockchain.ui.chooser.AccountListing
import com.blockchain.ui.password.SecondPasswordHandler
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.formatWithUnit
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.Completable
import io.reactivex.Single
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.ui.account.SecondPasswordHandlerDialog
import piuk.blockchain.android.ui.chooser.WalletAccountHelperAccountListingAdapter
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.send.SendPresenter
import piuk.blockchain.android.ui.send.SendPresenterXSendView
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.ui.send.external.DuelSendPresenterX
import piuk.blockchain.android.ui.send.external.SendFragmentXFactory
import piuk.blockchain.android.ui.send.external.SendPresenterX
import piuk.blockchain.android.ui.send.send2.XlmSendPresenterStrategy
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.PrngHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcoreui.utils.DateUtil
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.TimeUnit

val applicationModule = applicationContext {

    factory { StringUtils(get()) }

    factory { get<Context>().resources }

    factory { Locale.getDefault() }

    context("Payload") {

        factory {
            EthDataManager(get(), get(), get(), get(), get(), get(), get())
        }

        factory {
            BchDataManager(get(), get(), get(), get(), get(), get(), get())
        }

        factory {
            BuyDataManager(get(), get(), get(), get(), get())
        }

        factory {
            SwipeToReceiveHelper(get(), get(), get(), get(), get(), get())
        }

        factory { WalletAccountHelper(get(), get(), get(), get(), get(), get(), get(), get()) }

        factory { WalletAccountHelperAccountListingAdapter(get()) }
            .bind(AccountListing::class)

        factory { params ->
            SecondPasswordHandlerDialog(
                params.getActivity(),
                get()
            ) as SecondPasswordHandler
        }

        factory { KycStatusHelper(get(), get(), get()) }

        factory { TransactionListDataManager(get(), get(), get(), get(), get(), get()) }
            .bind(TotalBalance::class)

        factory {
            val old: SendPresenterX<SendView> = SendPresenter(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get()
            )
            val new: SendPresenterX<SendView> = XlmSendPresenterStrategy(get(), get(), get())
            SendPresenterXSendView(
                DuelSendPresenterX(
                    old,
                    new,
                    get(),
                    get(),
                    get()
                )
            )
        }

        factory {

            object : XlmTransactionSender {

                override fun sendFunds(value: CryptoValue, toAccountId: String): Completable {

                    return Completable.timer(4, TimeUnit.SECONDS)
                        .doOnSubscribe {
                            Timber.d("Simulating: Sending ${value.formatWithUnit()} to $toAccountId")
                        }
                        .doOnComplete {
                            Timber.d("Simulating: Sent ${value.formatWithUnit()} to $toAccountId")
                        }
                }
            } as XlmTransactionSender
        }
    }

    factory { DateUtil(get()) }

    bean { PrngHelper(get(), get()) as PrngFixer }

    factory { PrivateKeyFactory() }

    factory { SendFragmentXFactory() }

    bean { DynamicFeeCache() }
}
