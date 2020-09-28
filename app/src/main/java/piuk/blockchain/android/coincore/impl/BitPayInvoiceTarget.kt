package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoTarget
import piuk.blockchain.android.coincore.InvoiceTarget
import piuk.blockchain.android.data.api.bitpay.BITPAY_LIVE_BASE
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.PATH_BITPAY_INVOICE
import piuk.blockchain.android.data.api.bitpay.models.exceptions.BitPayApiException
import timber.log.Timber
import java.util.regex.Pattern

class BitPayInvoiceTarget(
    override val asset: CryptoCurrency,
    override val address: String,
    val amount: CryptoValue,
    val invoiceId: String,
    val merchant: String,
    val expires: String
) : InvoiceTarget, CryptoAddress {

    override val label: String = "BitPay[$merchant]"

    companion object {
        private const val INVOICE_PREFIX = "$BITPAY_LIVE_BASE$PATH_BITPAY_INVOICE/"
        private val MERCHANT_PATTERN: Pattern = Pattern.compile("for merchant ")

        fun fromLink(
            asset: CryptoCurrency,
            linkData: String,
            bitPayDataManager: BitPayDataManager
        ): Single<CryptoTarget> {
            val invoiceId: String =
                FormatsUtil.getPaymentRequestUrl(linkData)
                    .replace(INVOICE_PREFIX, "")

            return bitPayDataManager.getRawPaymentRequest(invoiceId = invoiceId)
                .map { rawRequest ->
                    BitPayInvoiceTarget(
                        asset = asset,
                        amount = CryptoValue.fromMinor(asset, rawRequest.instructions[0].outputs[0].amount),
                        invoiceId = invoiceId,
                        merchant = rawRequest.memo.split(MERCHANT_PATTERN)[1],
                        address = rawRequest.instructions[0].outputs[0].address,
                        expires = rawRequest.expires
                    ) as CryptoTarget
                }.doOnError { e ->
                    Timber.e("Error loading invoice: $e")
                    when (e) {
                        is BitPayApiException -> handleBitPayInvoiceError(e)
                        else -> throw e
                    }
                }
        }

        fun handleBitPayInvoiceError(e: BitPayApiException) {
//            e._httpErrorCode = 404
        }
    }
}