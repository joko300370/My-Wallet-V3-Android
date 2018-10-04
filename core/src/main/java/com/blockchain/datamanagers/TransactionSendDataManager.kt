package com.blockchain.datamanagers

import com.blockchain.datamanagers.fees.BitcoinLikeFees
import com.blockchain.datamanagers.fees.EthereumFees
import com.blockchain.datamanagers.fees.FeeType
import com.blockchain.datamanagers.fees.NetworkFees
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.wallet.toAccountReference
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.bitcoinj.core.ECKey
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.EthereumAccountWrapper
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import timber.log.Timber
import java.math.BigInteger

class TransactionSendDataManager(
    private val payloadDataManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val sendDataManager: SendDataManager,
    private val ethereumAccountWrapper: EthereumAccountWrapper
) {

    fun executeTransaction(
        amount: CryptoValue,
        destination: String,
        account: JsonSerializableAccount,
        fees: NetworkFees,
        feeType: FeeType = FeeType.Regular
    ): Single<String> = when (amount.currency) {
        CryptoCurrency.BTC -> sendBtcTransaction(
            amount,
            destination,
            account as Account,
            (fees as BitcoinLikeFees).feeForType(feeType)
        )
        CryptoCurrency.ETHER -> sendEthTransaction(
            amount,
            destination,
            account as EthereumAccount,
            fees as EthereumFees
        )
        CryptoCurrency.BCH -> sendBchTransaction(
            amount,
            destination,
            account as GenericMetadataAccount,
            (fees as BitcoinLikeFees).feeForType(feeType)
        )
        CryptoCurrency.XLM -> TODO("AND-1523")
    }

    fun getMaximumSpendable(
        account: JsonSerializableAccount,
        fees: NetworkFees,
        feeType: FeeType = FeeType.Regular
    ) = getMaximumSpendable(
        account.toAccountReference(),
        fees,
        feeType
    )

    fun getMaximumSpendable(
        account: AccountReference,
        fees: NetworkFees,
        feeType: FeeType = FeeType.Regular
    ): Single<CryptoValue> = when (account) {
        is AccountReference.BitcoinLike ->
            account.getMaximumSpendable(
                fees as BitcoinLikeFees,
                feeType
            )
        is AccountReference.Ethereum -> getMaxEther(fees as EthereumFees)
    }

    fun getFeeForTransaction(
        amount: CryptoValue,
        account: JsonSerializableAccount,
        fees: NetworkFees,
        feeType: FeeType = FeeType.Regular
    ): Single<CryptoValue> = when (amount.currency) {
        CryptoCurrency.BTC -> calculateBtcFee(
            account as Account,
            amount,
            (fees as BitcoinLikeFees).feeForType(feeType)
        )
        CryptoCurrency.BCH -> calculateBchFee(
            account as GenericMetadataAccount,
            amount,
            (fees as BitcoinLikeFees).feeForType(feeType)
        )
        CryptoCurrency.ETHER -> (fees as EthereumFees).absoluteFeeInWei.just()
        CryptoCurrency.XLM -> TODO("AND-1523")
    }

    fun getChangeAddress(
        cryptoCurrency: CryptoCurrency,
        account: JsonSerializableAccount
    ): Single<String> =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> (account as Account).getChangeAddress()
            CryptoCurrency.BCH -> (account as GenericMetadataAccount).getChangeAddress()
            CryptoCurrency.ETHER -> (account as EthereumAccount).checksumAddress.just()
            CryptoCurrency.XLM -> TODO("AND-1523")
        }

    fun getReceiveAddress(
        cryptoCurrency: CryptoCurrency,
        account: JsonSerializableAccount
    ): Single<String> =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> (account as Account).getReceiveAddress()
            CryptoCurrency.BCH -> (account as GenericMetadataAccount).getReceiveAddress()
            CryptoCurrency.ETHER -> (account as EthereumAccount).checksumAddress.just()
            CryptoCurrency.XLM -> TODO("AND-1523")
        }

    private fun calculateBtcFee(
        account: Account,
        amount: CryptoValue,
        feePerKb: BigInteger
    ): Single<CryptoValue> = calculateBtcOrBchAbsoluteFee(account.xpub, amount, feePerKb)
        .map { CryptoValue.bitcoinFromSatoshis(it) }

    private fun calculateBchFee(
        account: GenericMetadataAccount,
        amount: CryptoValue,
        feePerKb: BigInteger
    ): Single<CryptoValue> = calculateBtcOrBchAbsoluteFee(account.xpub, amount, feePerKb)
        .map { CryptoValue.bitcoinCashFromSatoshis(it) }

    private fun calculateBtcOrBchAbsoluteFee(
        xPub: String,
        amount: CryptoValue,
        feePerKb: BigInteger
    ): Single<BigInteger> = getUnspentOutputs(xPub, amount.currency)
        .map { getSuggestedAbsoluteFee(it, amount.amount, feePerKb) }

    private fun getSuggestedAbsoluteFee(
        coins: UnspentOutputs,
        amountToSend: BigInteger,
        feePerKb: BigInteger
    ): BigInteger = sendDataManager.getSpendableCoins(coins, amountToSend, feePerKb).absoluteFee

    private fun AccountReference.BitcoinLike.getMaximumSpendable(
        fees: BitcoinLikeFees,
        feeType: FeeType
    ): Single<CryptoValue> =
        getUnspentOutputs(xpub, cryptoCurrency)
            .map {
                CryptoValue(
                    cryptoCurrency,
                    sendDataManager.getMaximumAvailable(it, fees.feeForType(feeType)).left
                )
            }
            .doOnError { Timber.e(it) }
            .onErrorReturn { CryptoValue.zero(cryptoCurrency) }

    private fun getMaxEther(fees: EthereumFees): Single<CryptoValue> =
        ethDataManager.fetchEthAddress()
            .map {
                (it.getAddressResponse()!!.balance - fees.absoluteFeeInWei.amount).max(BigInteger.ZERO)
            }
            .map { CryptoValue.etherFromWei(it) }
            .doOnError { Timber.e(it) }
            .onErrorReturn { CryptoValue.ZeroEth }
            .singleOrError()

    private fun sendBtcTransaction(
        amount: CryptoValue,
        destination: String,
        account: Account,
        feePerKb: BigInteger
    ): Single<String> = sendBitcoinStyleTransaction(
        amount,
        destination,
        account,
        feePerKb,
        account.getChangeAddress()
    )

    private fun sendBchTransaction(
        amount: CryptoValue,
        destination: String,
        account: GenericMetadataAccount,
        feePerKb: BigInteger
    ): Single<String> = sendBitcoinStyleTransaction(
        amount,
        destination,
        account.getHdAccount(),
        feePerKb,
        account.getChangeAddress()
    )

    private fun sendBitcoinStyleTransaction(
        amount: CryptoValue,
        destination: String,
        account: Account,
        feePerKb: BigInteger,
        changeAddress: Single<String>
    ): Single<String> = getSpendableCoins(account.xpub, amount, feePerKb)
        .flatMap { spendable ->
            getSigningKeys(account, spendable)
                .flatMap { signingKeys ->
                    changeAddress
                        .flatMap {
                            submitBitcoinStylePayment(
                                amount,
                                spendable,
                                signingKeys,
                                destination,
                                it,
                                spendable.absoluteFee
                            )
                        }
                }
        }

    private fun sendEthTransaction(
        amount: CryptoValue,
        destination: String,
        account: EthereumAccount,
        fees: EthereumFees
    ): Single<String> = ethDataManager.fetchEthAddress()
        .map {
            ethDataManager.createEthTransaction(
                nonce = ethDataManager.getEthResponseModel()!!.getNonce(),
                to = destination,
                gasPriceWei = fees.gasPriceInWei,
                gasLimitGwei = fees.gasLimitInGwei,
                weiValue = amount.amount
            )
        }
        .map {
            account.signTransaction(
                it,
                ethereumAccountWrapper.deriveECKey(payloadDataManager.masterKey, 0)
            )
        }
        .flatMap { ethDataManager.pushEthTx(it) }
        .flatMap { ethDataManager.setLastTxHashObservable(it, System.currentTimeMillis()) }
        .subscribeOn(Schedulers.io())
        .singleOrError()

    private fun getSpendableCoins(
        address: String,
        amount: CryptoValue,
        feePerKb: BigInteger
    ): Single<SpendableUnspentOutputs> = getUnspentOutputs(address, amount.currency)
        .subscribeOn(Schedulers.io())
        .map { sendDataManager.getSpendableCoins(it, amount.amount, feePerKb) }

    private fun getUnspentOutputs(
        address: String,
        currency: CryptoCurrency
    ): Single<UnspentOutputs> =
        when (currency) {
            CryptoCurrency.BTC -> sendDataManager.getUnspentOutputs(address)
            CryptoCurrency.BCH -> sendDataManager.getUnspentBchOutputs(address)
            CryptoCurrency.ETHER -> throw IllegalArgumentException("Ether does not have unspent outputs")
            CryptoCurrency.XLM -> TODO("AND-1523")
        }.subscribeOn(Schedulers.io())
            .singleOrError()

    private fun submitBitcoinStylePayment(
        amount: CryptoValue,
        unspent: SpendableUnspentOutputs,
        signingKeys: List<ECKey>,
        depositAddress: String,
        changeAddress: String,
        absoluteFee: BigInteger
    ): Single<String> = when (amount.currency) {
        CryptoCurrency.BTC -> sendDataManager.submitBtcPayment(
            unspent,
            signingKeys,
            depositAddress,
            changeAddress,
            absoluteFee,
            amount.amount
        )
        CryptoCurrency.BCH -> sendDataManager.submitBchPayment(
            unspent,
            signingKeys,
            depositAddress,
            changeAddress,
            absoluteFee,
            amount.amount
        )
        CryptoCurrency.ETHER -> throw IllegalArgumentException("Ether not supported by this method")
        CryptoCurrency.XLM -> TODO("AND-1523")
    }.subscribeOn(Schedulers.io())
        .singleOrError()

    private fun getSigningKeys(
        account: Account,
        spendable: SpendableUnspentOutputs
    ): Single<List<ECKey>> =
        payloadDataManager.getHDKeysForSigning(account, spendable).just()

    private fun Account.getChangeAddress(): Single<String> =
        payloadDataManager.getNextChangeAddress(this).singleOrError()

    private fun Account.getReceiveAddress(): Single<String> =
        payloadDataManager.getNextReceiveAddress(this).singleOrError()

    private fun GenericMetadataAccount.getChangeAddress(): Single<String> {
        val position = bchDataManager.getActiveAccounts()
            .indexOfFirst { it.xpub == this.xpub }
        return bchDataManager.getNextChangeCashAddress(position).singleOrError()
    }

    private fun GenericMetadataAccount.getReceiveAddress(): Single<String> {
        val position = bchDataManager.getActiveAccounts()
            .indexOfFirst { it.xpub == this.xpub }
        return bchDataManager.getNextReceiveCashAddress(position).singleOrError()
    }

    private fun GenericMetadataAccount.getHdAccount(): Account =
        payloadDataManager.getAccountForXPub(this.xpub)

    private fun <T> T.just(): Single<T> = Single.just(this)

    private fun BitcoinLikeFees.feeForType(feeType: FeeType): BigInteger = when (feeType) {
        FeeType.Regular -> this.regularFeePerKb
        FeeType.Priority -> this.priorityFeePerKb
    }
}