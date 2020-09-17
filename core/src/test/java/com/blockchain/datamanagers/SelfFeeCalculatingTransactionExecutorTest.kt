package com.blockchain.datamanagers

import com.blockchain.datamanagers.fees.BitcoinLikeFees
import com.blockchain.datamanagers.fees.EthereumFees
import com.blockchain.fees.FeeType
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.blockchain.sunriver.Memo
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.any
import org.junit.Test
import piuk.blockchain.androidcore.data.fees.FeeDataManager

class SelfFeeCalculatingTransactionExecutorTest {

    @Test
    fun `can fetch fees and execute transaction`() {
        val bitcoinLikeFees = givenSomeFees()
        val feeDataManager = givenFeeDataManager(bitcoinLikeFees)
        val amount = 10.bitcoin()
        val sourceAccount = anAccountReference()
        val destination = "Dest"
        val memo = Memo("text")
        val transactionExecutor: TransactionExecutor = mock {
            on {
                executeTransaction(amount, destination, sourceAccount, bitcoinLikeFees, FeeType.Regular, memo)
            } `it returns` Single.just("tran_id")
        }
        SelfFeeCalculatingTransactionExecutor(transactionExecutor, mock(), feeDataManager, FeeType.Regular)
            .executeTransaction(
                amount,
                destination,
                sourceAccount,
                memo
            )
            .test()
            .assertComplete()
            .values().single() `should equal` "tran_id"
        verify(transactionExecutor).executeTransaction(
            amount,
            destination,
            sourceAccount,
            bitcoinLikeFees,
            FeeType.Regular,
            memo
        )
        verifyNoMoreInteractions(transactionExecutor)
    }

    @Test
    fun `can fetch fees and get maximum spendable`() {
        val bitcoinLikeFees = givenSomeFees()
        val feeDataManager = givenFeeDataManager(bitcoinLikeFees)
        val sourceAccount = anAccountReference()
        val transactionExecutor: TransactionExecutor = mock {
            on {
                getMaximumSpendable(sourceAccount, bitcoinLikeFees, FeeType.Regular)
            } `it returns` Single.just(99.bitcoin())
        }
        SelfFeeCalculatingTransactionExecutor(transactionExecutor, mock(), feeDataManager, FeeType.Regular)
            .getMaximumSpendable(
                sourceAccount
            )
            .test()
            .assertComplete()
            .values().single() `should equal` 99.bitcoin()
        verify(transactionExecutor).getMaximumSpendable(
            sourceAccount,
            bitcoinLikeFees,
            FeeType.Regular
        )
        verifyNoMoreInteractions(transactionExecutor)
    }

    @Test
    fun `can fetch fees and get fee for transaction`() {
        val bitcoinLikeFees = givenSomeFees()
        val feeDataManager = givenFeeDataManager(bitcoinLikeFees)
        val amount = 10.bitcoin()
        val sourceAccount = anAccountReference()
        val transactionExecutor: TransactionExecutor = mock {
            on {
                getFeeForTransaction(amount, sourceAccount, bitcoinLikeFees, FeeType.Regular)
            } `it returns` Single.just(0.1.bitcoin())
        }
        SelfFeeCalculatingTransactionExecutor(transactionExecutor, mock(), feeDataManager, FeeType.Regular)
            .getFeeForTransaction(
                amount,
                sourceAccount
            )
            .test()
            .assertComplete()
            .values().single() `should equal` 0.1.bitcoin()
        verify(transactionExecutor).getFeeForTransaction(
            amount,
            sourceAccount,
            bitcoinLikeFees,
            FeeType.Regular
        )
        verifyNoMoreInteractions(transactionExecutor)
    }

    @Test
    fun `can get change address - pass through`() {
        val feeDataManager: FeeDataManager = mock()
        val sourceAccount = anAccountReference()
        val transactionExecutor: TransactionExecutor = mock {
            on { getChangeAddress(sourceAccount) } `it returns` Single.just("address")
        }
        SelfFeeCalculatingTransactionExecutor(transactionExecutor, mock(), feeDataManager, FeeType.Regular)
            .getChangeAddress(sourceAccount)
            .test()
            .assertComplete()
            .values().single() `should equal` "address"
        verify(transactionExecutor).getChangeAddress(sourceAccount)
        verifyNoMoreInteractions(transactionExecutor)
        verifyZeroInteractions(feeDataManager)
    }

    @Test
    fun `can get receive address - pass through`() {
        val feeDataManager: FeeDataManager = mock()
        val sourceAccount = anAccountReference()
        val transactionExecutor: TransactionExecutor = mock {
            on { getReceiveAddress(sourceAccount) } `it returns` Single.just("address")
        }
        SelfFeeCalculatingTransactionExecutor(transactionExecutor, mock(), feeDataManager, FeeType.Regular)
            .getReceiveAddress(sourceAccount)
            .test()
            .assertComplete()
            .values().single() `should equal` "address"
        verify(transactionExecutor).getReceiveAddress(sourceAccount)
        verifyNoMoreInteractions(transactionExecutor)
        verifyZeroInteractions(feeDataManager)
    }

    @Test
    fun `on fee error, return zero`() {
        val feeDataManager = mock<FeeDataManager> {
            on { btcFeeOptions } `it returns` Observable.error(Throwable())
        }
        val sourceAccount = anAccountReference()
        val transactionExecutor: TransactionExecutor = mock()
        SelfFeeCalculatingTransactionExecutor(transactionExecutor, mock(), feeDataManager, FeeType.Regular)
            .getMaximumSpendable(sourceAccount)
            .test()
            .assertComplete()
            .values().single() `should equal` 0.bitcoin()
        verifyZeroInteractions(transactionExecutor)
        verifyNoMoreInteractions(transactionExecutor)
    }

    @Test
    fun `can fetch fees and execute priority transaction`() {
        val bitcoinLikeFees = givenSomeFees()
        val feeDataManager = givenFeeDataManager(bitcoinLikeFees)
        val amount = 10.bitcoin()
        val sourceAccount = anAccountReference()
        val destination = "Dest"
        val memo = Memo("text")
        val transactionExecutor: TransactionExecutor = mock {
            on {
                executeTransaction(amount, destination, sourceAccount, bitcoinLikeFees, FeeType.Priority, memo)
            } `it returns` Single.just("tran_id")
        }
        SelfFeeCalculatingTransactionExecutor(transactionExecutor, mock(), feeDataManager, FeeType.Priority)
            .executeTransaction(
                amount,
                destination,
                sourceAccount,
                memo
            )
            .test()
            .assertComplete()
            .values().single() `should equal` "tran_id"
        verify(transactionExecutor).executeTransaction(
            amount,
            destination,
            sourceAccount,
            bitcoinLikeFees,
            FeeType.Priority,
            memo
        )
        verifyNoMoreInteractions(transactionExecutor)
    }

    @Test
    fun `can fetch fees and get fee for priority transaction`() {
        val bitcoinLikeFees = givenSomeFees()
        val feeDataManager = givenFeeDataManager(bitcoinLikeFees)
        val amount = 10.bitcoin()
        val sourceAccount = anAccountReference()
        val transactionExecutor: TransactionExecutor = mock {
            on {
                getFeeForTransaction(amount, sourceAccount, bitcoinLikeFees, FeeType.Priority)
            } `it returns` Single.just(0.1.bitcoin())
        }
        SelfFeeCalculatingTransactionExecutor(transactionExecutor, mock(), feeDataManager, FeeType.Priority)
            .getFeeForTransaction(
                amount,
                sourceAccount
            )
            .test()
            .assertComplete()
            .values().single() `should equal` 0.1.bitcoin()
        verify(transactionExecutor).getFeeForTransaction(
            amount,
            sourceAccount,
            bitcoinLikeFees,
            FeeType.Priority
        )
        verifyNoMoreInteractions(transactionExecutor)
    }

    @Test
    fun `eth get maximum spendable with priority fee`() {
        val ethereumFees = givenSomeEthFees()
        val feeDataManager = givenEthFeeDataManager(ethereumFees)
        val sourceAccount = anEthAccountReference()
        val transactionExecutor: TransactionExecutor = mock {
            on {
                getMaximumSpendable(sourceAccount, ethereumFees, FeeType.Priority)
            } `it returns` Single.just(9.ether())
        }
        SelfFeeCalculatingTransactionExecutor(transactionExecutor, mock(), feeDataManager, FeeType.Priority)
            .getMaximumSpendable(sourceAccount)
            .test()
            .assertComplete()
            .values()
            .single() `should equal` 9.ether()
        verify(transactionExecutor).getMaximumSpendable(
            sourceAccount,
            ethereumFees,
            FeeType.Priority
        )
        verifyNoMoreInteractions(transactionExecutor)
    }

    @Test
    fun `ignore balance and fees when cryptocurrency is not PAX`() {

        val transactionExecutor: TransactionExecutor = mock()
        val balanceCalculator: BalanceCalculator = mock()
        val feeDataManager: FeeDataManager = mock()
        SelfFeeCalculatingTransactionExecutor(transactionExecutor, balanceCalculator, feeDataManager, FeeType.Priority)
            .hasEnoughEthFeesForTheTransaction(CryptoValue(CryptoCurrency.ETHER, 100.toBigInteger()),
                AccountReference.Ethereum("", "")).test().assertComplete()
            .values()
            .single() `should equal` true

        verifyZeroInteractions(transactionExecutor)
        verifyZeroInteractions(balanceCalculator)
        verifyZeroInteractions(feeDataManager)
    }

    @Test
    fun `pax transaction can proceed when fees are lower than the balance`() {
        val ethereumFees = givenSomeEthFees()
        val transactionExecutor: TransactionExecutor = mock {
            on {
                getFeeForTransaction(any(), any(), any(), any())
            } `it returns` Single.just(899.ether())
        }
        val feeDataManager = givenEthFeeDataManager(ethereumFees)
        val balanceCalculator: BalanceCalculator = mock {
            on {
                balance(CryptoCurrency.ETHER)
            } `it returns` Observable.just(900.ether())
        }

        SelfFeeCalculatingTransactionExecutor(transactionExecutor, balanceCalculator, feeDataManager, FeeType.Priority)
            .hasEnoughEthFeesForTheTransaction(CryptoValue(CryptoCurrency.PAX, 100.toBigInteger()),
                AccountReference.Pax("", "", "")).test().assertComplete()
            .values()
            .single() `should equal` true
        verify(balanceCalculator).balance(CryptoCurrency.ETHER)
        verify(transactionExecutor).getFeeForTransaction(any(), any(), any(), any())
    }

    @Test
    fun `pax transaction can not proceed when fees are higher than the balance`() {
        val ethereumFees = givenSomeEthFees()
        val transactionExecutor: TransactionExecutor = mock {
            on {
                getFeeForTransaction(any(), any(), any(), any())
            } `it returns` Single.just(901.ether())
        }
        val feeDataManager = givenEthFeeDataManager(ethereumFees)
        val balanceCalculator: BalanceCalculator = mock {
            on {
                balance(CryptoCurrency.ETHER)
            } `it returns` Observable.just(900.ether())
        }

        SelfFeeCalculatingTransactionExecutor(transactionExecutor, balanceCalculator, feeDataManager, FeeType.Priority)
            .hasEnoughEthFeesForTheTransaction(CryptoValue(CryptoCurrency.PAX, 100.toBigInteger()),
                AccountReference.Pax("", "", "")).test().assertComplete()
            .values()
            .single() `should equal` false

        verify(balanceCalculator).balance(CryptoCurrency.ETHER)
        verify(transactionExecutor).getFeeForTransaction(any(), any(), any(), any())
    }
}

private fun givenSomeEthFees() = EthereumFees(10, 20, 10)

private fun givenSomeFees() = BitcoinLikeFees(10, 20)

private fun givenEthFeeDataManager(ethFees: EthereumFees): FeeDataManager =
    mock {
        on { ethFeeOptions } `it returns` Observable.just(FeeOptions(
            regularFee = ethFees.gasPriceRegularInWei.toLong() / 1_000_000_000L,
            priorityFee = ethFees.gasPricePriorityInWei.toLong() / 1_000_000_000L,
            gasLimit = ethFees.gasLimitInGwei.toLong()
        ))
    }

private fun givenFeeDataManager(bitcoinLikeFees: BitcoinLikeFees): FeeDataManager =
    mock {
        on { btcFeeOptions } `it returns` Observable.just(FeeOptions(
            regularFee = bitcoinLikeFees.regularFeePerKb.toLong() / 1000L,
            priorityFee = bitcoinLikeFees.priorityFeePerKb.toLong() / 1000L
        ))
    }

private fun anAccountReference(): AccountReference = AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "")

private fun anEthAccountReference(): AccountReference = AccountReference.Ethereum("", "")
