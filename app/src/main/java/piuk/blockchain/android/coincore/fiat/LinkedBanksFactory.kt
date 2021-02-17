package piuk.blockchain.android.coincore.fiat

import com.blockchain.nabu.datamanagers.BankState
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import io.reactivex.Single
import piuk.blockchain.android.coincore.SingleAccount

class LinkedBanksFactory(
    val custodialWalletManager: CustodialWalletManager
) {

    fun getAllLinkedBanks(): Single<List<SingleAccount>> =
        custodialWalletManager.getBanks().map { banks ->
            banks.filter {
                it.state == BankState.ACTIVE
            }.map {
                LinkedBankAccount(
                    label = it.name,
                    accountNumber = it.account,
                    accountId = it.id,
                    accountType = it.toHumanReadableAccount(),
                    currency = it.currency,
                    custodialWalletManager = custodialWalletManager
                )
            }
        }

    fun getNonWireTransferBanks(): Single<List<LinkedBankAccount>> =
        custodialWalletManager.getBanks().map { banks ->
            banks.filter { it.state == BankState.ACTIVE && it.paymentMethodType == PaymentMethodType.BANK_TRANSFER }
                .map { bank ->
                    LinkedBankAccount(
                        label = bank.name,
                        accountNumber = bank.account,
                        accountId = bank.id,
                        accountType = bank.toHumanReadableAccount(),
                        currency = bank.currency,
                        custodialWalletManager = custodialWalletManager
                    )
                }
        }

    fun eligibleBankPaymentMethods(fiat: String): Single<Set<PaymentMethodType>> =
        custodialWalletManager.getEligiblePaymentMethodTypes(fiat).map { methods ->
            methods.filter {
                it.paymentMethodType == PaymentMethodType.BANK_TRANSFER ||
                    it.paymentMethodType == PaymentMethodType.FUNDS
            }.map { it.paymentMethodType }.toSet()
        }
}