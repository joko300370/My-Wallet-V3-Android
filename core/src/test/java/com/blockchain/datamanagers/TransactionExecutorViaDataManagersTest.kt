package com.blockchain.datamanagers

import com.blockchain.account.DefaultAccountDataManager
import com.blockchain.android.testutils.rxInit
import com.blockchain.datamanagers.fees.BitcoinLikeFees
import com.blockchain.datamanagers.fees.EthereumFees
import com.blockchain.datamanagers.fees.XlmFees
import com.blockchain.fees.FeeType
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.remoteconfig.CoinSelectionRemoteConfig
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import com.blockchain.testutils.lumens
import com.blockchain.testutils.satoshi
import com.blockchain.testutils.satoshiCash
import com.blockchain.testutils.stroops
import com.blockchain.testutils.wei
import com.blockchain.transactions.Memo
import com.blockchain.transactions.SendDetails
import com.blockchain.transactions.SendException
import com.blockchain.transactions.SendFundsResult
import com.blockchain.transactions.TransactionSender
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it throws`
import org.amshove.kluent.mock
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.DeterministicKey
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.EthereumAccountWrapper
import piuk.blockchain.androidcore.data.ethereum.exceptions.TransactionInProgressException
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import java.math.BigInteger

class TransactionExecutorViaDataManagersTest {

    private lateinit var subject: TransactionExecutor
    private val payloadDataManager: PayloadDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val erc20Account: Erc20Account = mock()
    private val sendDataManager: SendDataManager = mock()
    private val defaultAccountDataManager: DefaultAccountDataManager = mock()
    private val ethereumAccountWrapper: EthereumAccountWrapper = mock()
    private val addressResolver: AddressResolver = mock()
    private val accountLookup: AccountLookup = mock()
    private val xlmSender: TransactionSender = mock()
    private val analytics: Analytics = mock()
    private val coinSelectionRemoteConfig: CoinSelectionRemoteConfig = mock()

    private val useNewCoinSelection = true

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = TransactionExecutorViaDataManagers(
            payloadDataManager,
            ethDataManager,
            erc20Account,
            sendDataManager,
            addressResolver,
            accountLookup,
            defaultAccountDataManager,
            ethereumAccountWrapper,
            xlmSender,
            coinSelectionRemoteConfig,
            analytics
        )

        whenever(coinSelectionRemoteConfig.enabled).thenReturn(Single.just(useNewCoinSelection))
    }

    @Test
    fun `execute ethereum transaction should set regular fee by default`() {
        val amount = 10.wei()
        val destination = "DESTINATION"
        val account: EthereumAccount = mock()
        val combinedEthModel: CombinedEthModel = mock()
        val accountReference = AccountReference.Ethereum("", "")
        whenever(accountLookup.getAccountFromAddressOrXPub(accountReference)).thenReturn(account)
        whenever(ethDataManager.isLastTxPending()).thenReturn(Single.just(false))
        whenever(ethDataManager.fetchEthAddress())
            .thenReturn(Observable.just(combinedEthModel))
        whenever(ethDataManager.getEthResponseModel())
            .thenReturn(combinedEthModel)
        whenever(combinedEthModel.getNonce())
            .thenReturn(BigInteger.ONE)
        val rawTransaction: RawTransaction = mock()
        whenever(
            ethDataManager.createEthTransaction(
                BigInteger.ONE,
                destination,
                ethereumNetworkFee.gasPriceRegularInWei,
                ethereumNetworkFee.gasLimitInGwei,
                amount.toBigInteger()
            )
        ).thenReturn(rawTransaction)
        val deterministicKey: DeterministicKey = mock()
        whenever(payloadDataManager.masterKey)
            .thenReturn(deterministicKey)
        val ecKey = ECKey()
        whenever(ethereumAccountWrapper.deriveECKey(deterministicKey, 0))
            .thenReturn(ecKey)
        val signedTx = ByteArray(0)
        whenever(account.signTransaction(rawTransaction, ecKey))
            .thenReturn(signedTx)
        val txHash = "TX_HASH"
        whenever(ethDataManager.pushEthTx(signedTx))
            .thenReturn(Observable.just(txHash))
        whenever(ethDataManager.setLastTxHashObservable(eq(txHash), any()))
            .thenReturn(Observable.just(txHash))

        // Act
        subject.executeTransaction(amount, destination, accountReference, ethereumNetworkFee)
            .test()
            .assertComplete()

        // Assert
        verify(ethDataManager).createEthTransaction(
            nonce = BigInteger.ONE,
            to = destination,
            gasPriceWei = ethereumNetworkFee.gasPriceRegularInWei,
            gasLimitGwei = ethereumNetworkFee.gasLimitInGwei,
            weiValue = amount.toBigInteger()
        )
    }

    @Test
    fun `execute ethereum transaction with high priority fee`() {
        val amount = 10.wei()
        val destination = "DESTINATION"
        val account: EthereumAccount = mock()
        val combinedEthModel: CombinedEthModel = mock()
        val accountReference = AccountReference.Ethereum("", "")
        whenever(accountLookup.getAccountFromAddressOrXPub(accountReference)).thenReturn(account)
        whenever(ethDataManager.isLastTxPending()).thenReturn(Single.just(false))
        whenever(ethDataManager.fetchEthAddress())
            .thenReturn(Observable.just(combinedEthModel))
        whenever(ethDataManager.getEthResponseModel())
            .thenReturn(combinedEthModel)
        whenever(combinedEthModel.getNonce())
            .thenReturn(BigInteger.ONE)
        val rawTransaction: RawTransaction = mock()
        whenever(
            ethDataManager.createEthTransaction(
                BigInteger.ONE,
                destination,
                ethereumNetworkFee.gasPricePriorityInWei,
                ethereumNetworkFee.gasLimitInGwei,
                amount.toBigInteger()
            )
        ).thenReturn(rawTransaction)
        val deterministicKey: DeterministicKey = mock()
        whenever(payloadDataManager.masterKey)
            .thenReturn(deterministicKey)
        val ecKey = ECKey()
        whenever(ethereumAccountWrapper.deriveECKey(deterministicKey, 0))
            .thenReturn(ecKey)
        val signedTx = ByteArray(0)
        whenever(account.signTransaction(rawTransaction, ecKey))
            .thenReturn(signedTx)
        val txHash = "TX_HASH"
        whenever(ethDataManager.pushEthTx(signedTx))
            .thenReturn(Observable.just(txHash))
        whenever(ethDataManager.setLastTxHashObservable(eq(txHash), any()))
            .thenReturn(Observable.just(txHash))

        // Act
        subject.executeTransaction(amount, destination, accountReference, ethereumNetworkFee, FeeType.Priority)
            .test()
            .assertComplete()

        // Assert
        verify(ethDataManager).createEthTransaction(
            nonce = BigInteger.ONE,
            to = destination,
            gasPriceWei = ethereumNetworkFee.gasPricePriorityInWei,
            gasLimitGwei = ethereumNetworkFee.gasLimitInGwei,
            weiValue = amount.toBigInteger()
        )
    }

    @Test
    fun `execute bitcoin transaction should set regular fee by default`() {
        // Arrange
        val amount = 10.satoshi()
        val destination = "DESTINATION"
        val account = Account().apply { xpub = "XPUB" }
        val accountReference = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "XPUB")
        whenever(accountLookup.getAccountFromAddressOrXPub(accountReference)).thenReturn(account)
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBtcOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(sendDataManager.getSpendableCoins(any(), any(), any(), any()))
            .thenReturn(SpendableUnspentOutputs())
        whenever(addressResolver.getChangeAddress(account))
            .thenReturn(Single.just("CHANGE"))
        whenever(sendDataManager.submitBtcPayment(any(), any(), any(), any(), any(), any()))
            .thenReturn(Observable.just("TX_ HASH"))
        // Act
        subject.executeTransaction(amount, destination, accountReference, bitcoinLikeNetworkFee)
            .test()
            .assertComplete()
        // Assert
        verify(sendDataManager).getSpendableCoins(
            unspentOutputs,
            amount,
            bitcoinLikeNetworkFee.regularFeePerKb,
            useNewCoinSelection
        )
    }

    @Test
    fun `execute bitcoin transaction with high priority fee`() {
        // Arrange
        val amount = 10.satoshi()
        val destination = "DESTINATION"
        val unspentOutputs = UnspentOutputs()
        val account = Account().apply { xpub = "XPUB" }
        val accountReference = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "XPUB")
        whenever(accountLookup.getAccountFromAddressOrXPub(accountReference)).thenReturn(account)
        whenever(sendDataManager.getUnspentBtcOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(sendDataManager.getSpendableCoins(any(), any(), any(), any()))
            .thenReturn(SpendableUnspentOutputs())
        whenever(addressResolver.getChangeAddress(account))
            .thenReturn(Single.just("CHANGE"))
        whenever(sendDataManager.submitBtcPayment(any(), any(), any(), any(), any(), any()))
            .thenReturn(Observable.just("TX_ HASH"))
        // Act
        subject.executeTransaction(
            amount,
            destination,
            accountReference,
            bitcoinLikeNetworkFee,
            FeeType.Priority
        ).test()
            .assertComplete()
        // Assert
        verify(sendDataManager).getSpendableCoins(
            unspentOutputs,
            amount,
            bitcoinLikeNetworkFee.priorityFeePerKb,
            useNewCoinSelection
        )
    }

    @Test
    fun `execute bitcoin transaction verify entire flow`() {
        // Arrange
        val amount = 10.satoshi()
        val destination = "DESTINATION"
        val change = "CHANGE"
        val accountReference = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "XPUB")
        val account = Account().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBtcOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        val spendable = SpendableUnspentOutputs().apply { absoluteFee = BigInteger.TEN }
        whenever(sendDataManager.getSpendableCoins(any(), any(), any(), any()))
            .thenReturn(spendable)
        val ecKey = ECKey()
        whenever(payloadDataManager.getHDKeysForSigning(account, spendable))
            .thenReturn(listOf(ecKey))
        whenever(accountLookup.getAccountFromAddressOrXPub(accountReference)).thenReturn(account)
        whenever(addressResolver.getChangeAddress(account))
            .thenReturn(Single.just(change))
        val txHash = "TX_ HASH"
        whenever(
            sendDataManager.submitBtcPayment(
                spendable,
                listOf(ecKey),
                destination,
                change,
                spendable.absoluteFee,
                amount.toBigInteger()
            )
        ).thenReturn(Observable.just(txHash))
        // Act
        val testObserver =
            subject.executeTransaction(amount, destination, accountReference, bitcoinLikeNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(txHash)
        verify(sendDataManager).submitBtcPayment(
            spendable,
            listOf(ecKey),
            destination,
            change,
            spendable.absoluteFee,
            amount.toBigInteger()
        )
    }

    @Test
    fun `execute bitcoin cash transaction verify entire flow`() {
        // Arrange
        val amount = 10.satoshiCash()
        val destination = "DESTINATION"
        val change = "CHANGE"
        val bchAccount = GenericMetadataAccount().apply { xpub = "XPUB" }
        val accountReference = AccountReference.BitcoinLike(CryptoCurrency.BCH, "", "XPUB")
        val account = Account().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBchOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        val spendable = SpendableUnspentOutputs().apply { absoluteFee = BigInteger.TEN }
        whenever(sendDataManager.getSpendableCoins(any(), any(), any(), any()))
            .thenReturn(spendable)
        whenever(payloadDataManager.getAccountForXPub("XPUB"))
            .thenReturn(account)
        whenever(accountLookup.getAccountFromAddressOrXPub(accountReference)).thenReturn(bchAccount)
        val ecKey = ECKey()
        whenever(payloadDataManager.getHDKeysForSigning(account, spendable))
            .thenReturn(listOf(ecKey))
        whenever(addressResolver.getChangeAddress(bchAccount))
            .thenReturn(Single.just(change))
        val txHash = "TX_ HASH"
        whenever(
            sendDataManager.submitBchPayment(
                spendable,
                listOf(ecKey),
                destination,
                change,
                spendable.absoluteFee,
                amount.toBigInteger()
            )
        ).thenReturn(Observable.just(txHash))
        // Act
        val testObserver =
            subject.executeTransaction(amount, destination, accountReference, bitcoinLikeNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(txHash)
        verify(sendDataManager).submitBchPayment(
            spendable,
            listOf(ecKey),
            destination,
            change,
            spendable.absoluteFee,
            amount.toBigInteger()
        )
    }

    @Test
    fun `execute ethereum transaction verify entire flow`() {
        // Arrange
        val amount = 10.wei()
        val destination = "DESTINATION"
        val account: EthereumAccount = mock()
        val combinedEthModel: CombinedEthModel = mock()
        val accountReference = AccountReference.Ethereum("", "")
        whenever(accountLookup.getAccountFromAddressOrXPub(accountReference)).thenReturn(account)
        whenever(ethDataManager.isLastTxPending()).thenReturn(Single.just(false))
        whenever(ethDataManager.fetchEthAddress())
            .thenReturn(Observable.just(combinedEthModel))
        whenever(ethDataManager.getEthResponseModel())
            .thenReturn(combinedEthModel)
        whenever(combinedEthModel.getNonce())
            .thenReturn(BigInteger.ONE)
        val rawTransaction: RawTransaction = mock()
        whenever(
            ethDataManager.createEthTransaction(
                BigInteger.ONE,
                destination,
                ethereumNetworkFee.gasPriceRegularInWei,
                ethereumNetworkFee.gasLimitInGwei,
                amount.toBigInteger()
            )
        ).thenReturn(rawTransaction)
        val deterministicKey: DeterministicKey = mock()
        whenever(payloadDataManager.masterKey)
            .thenReturn(deterministicKey)
        val ecKey = ECKey()
        whenever(ethereumAccountWrapper.deriveECKey(deterministicKey, 0))
            .thenReturn(ecKey)
        val signedTx = ByteArray(0)
        whenever(account.signTransaction(rawTransaction, ecKey))
            .thenReturn(signedTx)
        val txHash = "TX_HASH"
        whenever(ethDataManager.pushEthTx(signedTx))
            .thenReturn(Observable.just(txHash))
        whenever(ethDataManager.setLastTxHashObservable(eq(txHash), any()))
            .thenReturn(Observable.just(txHash))
        // Act
        val testObserver =
            subject.executeTransaction(amount, destination, accountReference, ethereumNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(txHash)
        verify(ethDataManager).createEthTransaction(
            BigInteger.ONE,
            destination,
            ethereumNetworkFee.gasPriceRegularInWei,
            ethereumNetworkFee.gasLimitInGwei,
            amount.toBigInteger()
        )
    }

    @Test
    fun `execute xlm transaction verify entire flow`() {
        // Arrange
        val amount = 15.lumens()
        val destination = "DESTINATION"
        val accountReference = AccountReference.Xlm("", "")
        val txHash = "TX_HASH"
        val memo = Memo("theValue", "theType")
        val sendDetails = SendDetails(
            from = accountReference,
            value = amount,
            toAddress = destination,
            fee = 100.stroops(),
            memo = memo
        )
        whenever(
            xlmSender.sendFunds(sendDetails)
        ).thenReturn(
            Single.just(
                SendFundsResult(
                    errorCode = 0,
                    confirmationDetails = null,
                    hash = txHash,
                    sendDetails = sendDetails
                )
            )
        )
        whenever(accountLookup.getAccountFromAddressOrXPub(accountReference)) `it throws` IllegalArgumentException()
        // Act
        val testObserver =
            subject.executeTransaction(
                amount,
                destination,
                accountReference,
                XlmFees(100.stroops(), 100.stroops()),
                memo = memo
            ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(txHash)
    }

    @Test
    fun `execute xlm transaction failure`() {
        // Arrange
        val amount = 15.lumens()
        val destination = "DESTINATION"
        val accountReference = AccountReference.Xlm("", "")
        val txHash = "TX_HASH"
        val sendDetails = SendDetails(
            from = accountReference,
            value = amount,
            toAddress = destination,
            fee = 100.stroops()
        )
        whenever(
            xlmSender.sendFunds(sendDetails)
        ).thenReturn(
            Single.just(
                SendFundsResult(
                    errorCode = 100,
                    confirmationDetails = null,
                    hash = txHash,
                    sendDetails = sendDetails
                )
            )
        )
        whenever(accountLookup.getAccountFromAddressOrXPub(accountReference)) `it throws` IllegalArgumentException()
        // Act
        val testObserver =
            subject.executeTransaction(amount, destination, accountReference, XlmFees(100.stroops(), 1.stroops()))
                .test()
        // Assert
        testObserver.assertNotComplete().assertError(SendException::class.java)
    }

    @Test
    fun `execute ethereum transaction fails due to pending transaction`() {
        // Arrange
        val amount = 10.wei()
        val destination = "DESTINATION"
        val account: EthereumAccount = mock()
        val accountReference = AccountReference.Ethereum("", "")
        whenever(accountLookup.getAccountFromAddressOrXPub(accountReference)).thenReturn(account)
        whenever(ethDataManager.isLastTxPending()).thenReturn(Single.just(true))
        // Act
        val testObserver =
            subject.executeTransaction(amount, destination, accountReference, ethereumNetworkFee)
                .test()
        // Assert
        testObserver.assertError(TransactionInProgressException::class.java)
    }

    @Test
    fun `get maximum spendable BTC with default regular fee`() {
        // Arrange
        val account = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "XPUB")
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBtcOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(
            sendDataManager.getMaximumAvailable(
                CryptoCurrency.BTC,
                unspentOutputs,
                bitcoinLikeNetworkFee.regularFeePerKb,
                useNewCoinSelection
            )
        ).thenReturn(Pair.of(BigInteger.TEN, BigInteger.TEN))
        // Act
        val testObserver =
            subject.getMaximumSpendable(account, bitcoinLikeNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(10.satoshi())
    }

    @Test
    fun `get maximum spendable PAX`() {
        // Arrange
        val account = AccountReference.Pax("", "", "")

        whenever(erc20Account.getBalance()).thenReturn(Single.just(100.toBigInteger()))

        // Act
        val testObserver = subject.getMaximumSpendable(account, mock())
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue(CryptoCurrency.PAX, 100.toBigInteger()))
    }

    @Test
    fun `get maximum spendable BTC with priority fee`() {
        // Arrange
        val account = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "XPUB")
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBtcOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(
            sendDataManager.getMaximumAvailable(
                CryptoCurrency.BTC,
                unspentOutputs,
                bitcoinLikeNetworkFee.priorityFeePerKb,
                useNewCoinSelection
            )
        ).thenReturn(Pair.of(BigInteger.TEN, BigInteger.TEN))
        // Act
        val testObserver = subject.getMaximumSpendable(
            account,
            bitcoinLikeNetworkFee,
            FeeType.Priority
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(10.satoshi())
    }

    @Test
    fun `get maximum spendable BTC should return zero on error`() {
        // Arrange
        val account = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "XPUB")
        whenever(sendDataManager.getUnspentBtcOutputs("XPUB"))
            .thenReturn(Observable.error { Throwable() })
        // Act
        val testObserver =
            subject.getMaximumSpendable(account, bitcoinLikeNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.ZeroBtc)
    }

    @Test
    fun `get maximum spendable BCH`() {
        // Arrange
        val accountReferenece =
            AccountReference.BitcoinLike(CryptoCurrency.BCH, "", "XPUB")
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBchOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(
            sendDataManager.getMaximumAvailable(
                CryptoCurrency.BCH,
                unspentOutputs,
                bitcoinLikeNetworkFee.regularFeePerKb,
                useNewCoinSelection
            )
        ).thenReturn(Pair.of(BigInteger.TEN, BigInteger.TEN))
        // Act
        val testObserver = subject.getMaximumSpendable(
            accountReferenece,
            bitcoinLikeNetworkFee
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(10.satoshiCash())
    }

    @Test
    fun `get maximum spendable BCH should return zero on error`() {
        // Arrange
        val account = AccountReference.BitcoinLike(CryptoCurrency.BCH, "", "XPUB")
        whenever(sendDataManager.getUnspentBchOutputs("XPUB"))
            .thenReturn(Observable.error { Throwable() })
        // Act
        val testObserver =
            subject.getMaximumSpendable(account, bitcoinLikeNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.ZeroBch)
    }

    @Test
    fun `get maximum spendable ETH`() {
        // Arrange
        val account = AccountReference.Ethereum("", "")
        val combinedEthModel: CombinedEthModel = mock()
        val addressResponse: EthAddressResponse = mock()
        whenever(combinedEthModel.getAddressResponse()).thenReturn(addressResponse)
        whenever(addressResponse.balance).thenReturn(BigInteger.valueOf(1_000_000_000_000_000_000L))
        whenever(ethDataManager.fetchEthAddress())
            .thenReturn(Observable.just(combinedEthModel))
        // Act
        val testObserver = subject.getMaximumSpendable(account, ethereumNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(
            CryptoValue.fromMinor(CryptoCurrency.ETHER,
                1_000_000_000_000_000_000L.toBigInteger() -
                        ethereumNetworkFee.absoluteRegularFeeInWei.toBigInteger()
            )
        )
    }

    @Test
    fun `get maximum spendable XLM`() {
        // Arrange
        val account = AccountReference.Xlm("", "")
        whenever(defaultAccountDataManager.getMaxSpendableAfterFees(FeeType.Regular))
            .thenReturn(Single.just(150.lumens()))
        // Act
        val testObserver = subject.getMaximumSpendable(account, XlmFees(99.stroops(), 99.stroops()))
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(150.lumens())
    }

    @Test
    fun `get maximum spendable ETH should not return less than zero`() {
        // Arrange
        val account = AccountReference.Ethereum("", "")
        val combinedEthModel: CombinedEthModel = mock()
        val addressResponse: EthAddressResponse = mock()
        whenever(combinedEthModel.getAddressResponse()).thenReturn(addressResponse)
        whenever(addressResponse.balance).thenReturn(BigInteger.ZERO)
        whenever(ethDataManager.fetchEthAddress())
            .thenReturn(Observable.just(combinedEthModel))
        // Act
        val testObserver = subject.getMaximumSpendable(account, ethereumNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.ZeroEth)
    }

    @Test
    fun `get maximum spendable ETH should return zero if error fetching account`() {
        // Arrange
        val account = AccountReference.Ethereum("", "")
        whenever(ethDataManager.fetchEthAddress())
            .thenReturn(Observable.error { Throwable() })
        // Act
        val testObserver = subject.getMaximumSpendable(account, ethereumNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.ZeroEth)
    }

    @Test
    fun `get absolute fee for bitcoin`() {
        // Arrange
        val amount = 1.23.bitcoin()
        val account = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "XPUB")
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBtcOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(sendDataManager.getSpendableCoins(any(), any(), any(), any()))
            .thenReturn(SpendableUnspentOutputs().apply { absoluteFee = 500.toBigInteger() })
        // Act
        val testObserver = subject.getFeeForTransaction(amount, account, bitcoinLikeNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(500.satoshi())
    }

    @Test
    fun `get absolute fee for bitcoin uses regular fee by default`() {
        // Arrange
        val amount = 1.23.bitcoin()
        val account = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "XPUB")
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBtcOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(
            sendDataManager.getSpendableCoins(
                any(),
                any(),
                eq(bitcoinLikeNetworkFee.regularFeePerKb),
                any()
            )
        ).thenReturn(SpendableUnspentOutputs().apply { absoluteFee = 500.toBigInteger() })
        // Act
        val testObserver = subject.getFeeForTransaction(amount, account, bitcoinLikeNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(500.satoshi())
    }

    @Test
    fun `get absolute fee for bitcoin uses priority fee if specified`() {
        // Arrange
        val amount = 1.23.bitcoin()
        val account = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "XPUB")
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBtcOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(
            sendDataManager.getSpendableCoins(
                any(),
                any(),
                eq(bitcoinLikeNetworkFee.priorityFeePerKb),
                any()
            )
        ).thenReturn(SpendableUnspentOutputs().apply { absoluteFee = 500.toBigInteger() })
        // Act
        val testObserver =
            subject.getFeeForTransaction(amount, account, bitcoinLikeNetworkFee, FeeType.Priority)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(500.satoshi())
    }

    @Test
    fun `get absolute fee for bitcoin cash`() {
        // Arrange
        val amount = 1.23.bitcoinCash()
        val account = AccountReference.BitcoinLike(CryptoCurrency.BCH, "", "XPUB")
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBchOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(sendDataManager.getSpendableCoins(any(), any(), any(), any()))
            .thenReturn(SpendableUnspentOutputs().apply { absoluteFee = 500.toBigInteger() })
        // Act
        val testObserver = subject.getFeeForTransaction(amount, account, bitcoinLikeNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(500.satoshiCash())
    }

    @Test
    fun `get absolute fee for ether`() {
        // Arrange
        val amount = 1.23.ether()
        val account = AccountReference.Ethereum("", "")
        // Act
        val testObserver = subject.getFeeForTransaction(amount, account, ethereumNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(ethereumNetworkFee.absoluteRegularFeeInWei)
    }

    @Test
    fun `get absolute fee for Xlm`() {
        // Arrange
        val amount = 150.stroops()
        val account = AccountReference.Xlm("", "")
        // Act
        val testObserver = subject.getFeeForTransaction(amount, account, XlmFees(200.stroops(), 250.stroops()))
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(200.stroops())
    }

    @Test
    fun `get change address bitcoin`() {
        // Arrange
        val reference = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "")
        reference givenAddresses AddressPair("", "CHANGE1")
        // Act
        val testObserver = subject.getChangeAddress(reference)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("CHANGE1")
    }

    @Test
    fun `get change address bitcoin cash`() {
        // Arrange
        val reference = AccountReference.BitcoinLike(CryptoCurrency.BCH, "", "")
        reference givenAddresses AddressPair("", "CHANGE2")
        // Act
        val testObserver = subject.getChangeAddress(reference)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("CHANGE2")
    }

    @Test
    fun `get change address ethereum`() {
        // Arrange
        val reference = AccountReference.Ethereum("", "")
        reference givenAddresses AddressPair("", "CHANGE3")
        // Act
        val testObserver = subject.getChangeAddress(reference)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("CHANGE3")
    }

    @Test
    fun `get change address Xlm`() {
        // Arrange
        val reference = AccountReference.Xlm("", "")
        reference givenAddresses AddressPair("", "CHANGE4")
        // Act
        val testObserver = subject.getChangeAddress(reference)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("CHANGE4")
    }

    @Test
    fun `get receive address bitcoin`() {
        // Arrange
        val reference = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "")
        reference givenAddresses AddressPair("RECEIVE1", "")
        // Act
        val testObserver = subject.getReceiveAddress(reference)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("RECEIVE1")
    }

    @Test
    fun `get receive address bitcoin cash`() {
        // Arrange
        val reference = AccountReference.BitcoinLike(CryptoCurrency.BCH, "", "")
        reference givenAddresses AddressPair("RECEIVE2", "")
        // Act
        val testObserver = subject.getReceiveAddress(reference)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("RECEIVE2")
    }

    @Test
    fun `get receive address ethereum`() {
        // Arrange
        val reference = AccountReference.Ethereum("", "")
        reference givenAddresses AddressPair("RECEIVE3", "")
        // Act
        val testObserver = subject.getReceiveAddress(reference)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("RECEIVE3")
    }

    @Test
    fun `get receive address xlm`() {
        // Arrange
        val reference = AccountReference.Xlm("", "")
        reference givenAddresses AddressPair("RECEIVE4", "")
        // Act
        val testObserver = subject.getReceiveAddress(reference)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("RECEIVE4")
    }

    private val bitcoinLikeNetworkFee = BitcoinLikeFees(
        priorityFeePerByte = 100L,
        regularFeePerByte = 10L
    )

    private val ethereumNetworkFee = EthereumFees(
        gasPriceRegularGwei = 10L,
        gasPricePriorityGwei = 20L,
        gasLimitGwei = 21000L
    )

    private infix fun AccountReference.givenAddresses(
        addressPair: AddressPair
    ) {
        whenever(addressResolver.addressPairForAccount(this))
            .thenReturn(Single.just(addressPair))
    }
}
