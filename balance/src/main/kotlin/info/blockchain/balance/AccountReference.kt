package info.blockchain.balance

sealed class AccountReference(
    val cryptoCurrency: CryptoCurrency,
    val label: String
) {
    abstract val receiveAddress: String

    data class BitcoinLike(
        private val _cryptoCurrency: CryptoCurrency,
        private val _label: String,
        val xpub: String
    ) : AccountReference(_cryptoCurrency, _label) {

        override val receiveAddress: String
            get() = xpub
    }

    data class Ethereum(
        private val _label: String,
        val address: String
    ) : AccountReference(CryptoCurrency.ETHER, _label) {
        override val receiveAddress: String
            get() = address
    }

    data class Xlm(
        private val _label: String,
        val accountId: String
    ) : AccountReference(CryptoCurrency.XLM, _label) {
        override val receiveAddress: String
            get() = accountId
    }

    data class Pax(
        private val _label: String,
        val ethAddress: String,
        val apiCode: String
    ) : AccountReference(CryptoCurrency.PAX, _label) {
        override val receiveAddress: String
            get() = ethAddress
    }

    data class Stx(
        private val _label: String,
        val address: String
    ) : AccountReference(CryptoCurrency.STX, _label) {
        override val receiveAddress: String
            get() = address
    }
}

enum class AccountType {
    Spendable,
    ColdStorage,
    WatchOnly
}

typealias AccountReferenceList = List<AccountReference>

data class Account(val reference: AccountReference, val type: AccountType)

fun AccountReference.toAccount(type: AccountType): Account = Account(this, type)