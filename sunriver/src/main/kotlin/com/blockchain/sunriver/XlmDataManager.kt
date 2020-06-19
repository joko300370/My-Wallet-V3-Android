package com.blockchain.sunriver

import com.blockchain.account.BalanceAndMin
import com.blockchain.account.DefaultAccountDataManager
import com.blockchain.fees.FeeType
import com.blockchain.sunriver.datamanager.XlmAccount
import com.blockchain.sunriver.datamanager.XlmMetaData
import com.blockchain.sunriver.datamanager.XlmMetaDataInitializer
import com.blockchain.sunriver.datamanager.default
import com.blockchain.sunriver.models.XlmTransaction
import com.blockchain.transactions.SendConfirmationDetails
import com.blockchain.transactions.SendDetails
import com.blockchain.transactions.SendFundsResult
import com.blockchain.transactions.TransactionSender
import com.blockchain.utils.toHex
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.schedulers.Schedulers
import org.stellar.sdk.KeyPair

class XlmDataManager internal constructor(
    private val horizonProxy: HorizonProxy,
    metaDataInitializer: XlmMetaDataInitializer,
    private val xlmSecretAccess: XlmSecretAccess,
    private val memoMapper: MemoMapper,
    private val xlmFeesFetcher: XlmFeesFetcher,
    private val xlmTimeoutFetcher: XlmTransactionTimeoutFetcher,
    xlmHorizonUrlFetcher: XlmHorizonUrlFetcher,
    xlmHorizonDefUrl: String
) : TransactionSender,
    DefaultAccountDataManager {

    private val xlmProxyUrl = xlmHorizonUrlFetcher.xlmHorizonUrl(xlmHorizonDefUrl).doOnSuccess {
        horizonProxy.update(it)
    }.cache()

    override fun sendFunds(
        sendDetails: SendDetails
    ): Single<SendFundsResult> =
        Single.defer {
            Singles.zip(
                xlmSecretAccess.getPrivate(HorizonKeyPair.Public(sendDetails.fromXlm.accountId)).toSingle(),
                xlmTimeoutFetcher.transactionTimeout(),
                xlmProxyUrl
            ).map {
                horizonProxy.sendTransaction(
                    it.first.toKeyPair(),
                    sendDetails.toAddress,
                    sendDetails.value,
                    memoMapper.mapMemo(sendDetails.memo),
                    it.second,
                    sendDetails.fee
                )
            }.map { it.mapToSendFundsResult(sendDetails) }
        }

    override fun dryRunSendFunds(
        sendDetails: SendDetails
    ): Single<SendFundsResult> =
        Single.defer {
            horizonProxy.dryRunTransaction(
                HorizonKeyPair.Public(sendDetails.fromXlm.accountId).toKeyPair(),
                sendDetails.toAddress,
                sendDetails.value,
                memoMapper.mapMemo(sendDetails.memo),
                sendDetails.fee
            ).mapToSendFundsResult(sendDetails).just().ensureUrlUpdated()
        }

    fun isAddressValid(address: String): Boolean =
        try {
            KeyPair.fromAccountId(address)
            true
        } catch (e: Exception) {
            false
        }

    private fun <T> T.just(): Single<T> = Single.just(this)

    private val wallet = Single.defer { metaDataInitializer.initWalletMaybePrompt.toSingle() }
    private val maybeWallet = Maybe.defer { metaDataInitializer.initWalletMaybe }

    fun getBalance(accountReference: AccountReference.Xlm): Single<CryptoValue> =
        getBalance(accountReference.accountId)

    private fun getBalance(address: String): Single<CryptoValue> =
        Single.fromCallable { horizonProxy.getBalance(address) }.ensureUrlUpdated()
            .subscribeOn(Schedulers.io())

    private fun getBalanceAndMin(accountReference: AccountReference.Xlm): Single<BalanceAndMin> =
        Single.fromCallable { horizonProxy.getBalanceAndMin(accountReference.accountId) }.ensureUrlUpdated()
            .subscribeOn(Schedulers.io())

    fun getBalance(): Single<CryptoValue> =
        Maybe.concat(
            maybeDefaultAccount().flatMap { getBalance(it).toMaybe() },
            Maybe.just(CryptoValue.ZeroXlm)
        ).firstOrError()

    /**
     * Balance - minimum - fees
     */
    override fun getMaxSpendableAfterFees(feeType: FeeType): Single<CryptoValue> =
        Maybe.concat(
            maybeDefaultAccount()
                .flatMapSingle { accountRef ->
                    xlmFeesFetcher.operationFee(feeType).map { accountRef to it }
                }
                .flatMap { (accountRef, fee) ->
                    getBalanceAndMin(accountRef).map { it.balance - it.minimumBalance - fee }
                }.toMaybe(),
            Maybe.just(CryptoValue.ZeroXlm)
        ).firstOrError()

    override fun getBalanceAndMin(): Single<BalanceAndMin> =
        Maybe.concat(
            maybeDefaultAccount().flatMap {
                getBalanceAndMin(it).toMaybe()
            },
            Maybe.just(BalanceAndMin(CryptoValue.ZeroXlm, CryptoValue.ZeroXlm))
        ).firstOrError()

    fun defaultAccount(): Single<AccountReference.Xlm> =
        defaultXlmAccount()
            .map(XlmAccount::toReference)

    override fun defaultAccountReference(): Single<AccountReference> = defaultAccount().map { it }

    fun maybeDefaultAccount(): Maybe<AccountReference.Xlm> =
        maybeDefaultXlmAccount()
            .map(XlmAccount::toReference)

    fun getTransactionList(accountReference: AccountReference.Xlm): Single<List<XlmTransaction>> =
        Single.fromCallable {
            horizonProxy.getTransactionList(accountReference.accountId)
                .map(accountReference.accountId, horizonProxy)
        }.ensureUrlUpdated().subscribeOn(Schedulers.io())

    /**
     * See also [getOperationFee]
     */
    fun getTransactionFee(hash: String): Single<CryptoValue> =
        Single.fromCallable { horizonProxy.getTransaction(hash) }.ensureUrlUpdated()
            .map { CryptoValue.lumensFromStroop(it.feeCharged.toBigInteger()) }
            .subscribeOn(Schedulers.io())

    /**
     * See also [getTransactionFee]
     */
    fun getOperationFee(transactionHash: String): Single<CryptoValue> =
        Single.fromCallable { horizonProxy.getTransaction(transactionHash) }
            .map { CryptoValue.lumensFromStroop((it.feeCharged / it.operationCount).toBigInteger()) }
            .subscribeOn(Schedulers.io())

    fun getTransactionList(): Single<List<XlmTransaction>> =
        defaultAccount().flatMap { getTransactionList(it) }

    private fun defaultXlmAccount() =
        wallet.map(XlmMetaData::default)

    private fun maybeDefaultXlmAccount() =
        maybeWallet.map(XlmMetaData::default)

    private fun <T> Single<T>.ensureUrlUpdated(): Single<T> =
        xlmProxyUrl.flatMap {
            this
        }
}

internal fun HorizonProxy.SendResult.mapToSendFundsResult(sendDetails: SendDetails): SendFundsResult =
    if (success) {
        SendFundsResult(
            sendDetails = sendDetails,
            errorCode = 0,
            confirmationDetails = SendConfirmationDetails(
                sendDetails = sendDetails,
                fees = CryptoValue.lumensFromStroop(transaction!!.fee.toBigInteger())
            ),
            hash = transaction.hash().toHex()
        )
    } else {
        SendFundsResult(
            sendDetails = sendDetails,
            errorCode = failureReason.errorCode,
            errorValue = failureValue,
            confirmationDetails = null,
            hash = null,
            errorExtra = failureExtra
        )
    }

private val SendDetails.fromXlm
    get() = from as? AccountReference.Xlm
        ?: throw XlmSendException("Source account reference is not an Xlm reference")

class XlmSendException(message: String) : RuntimeException(message)

private fun XlmAccount.toReference() =
    AccountReference.Xlm(label ?: "", publicKey)
