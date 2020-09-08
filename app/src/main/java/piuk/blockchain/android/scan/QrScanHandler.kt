package piuk.blockchain.android.scan

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.blockchain.koin.payloadScope
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.MaybeSubject
import io.reactivex.subjects.SingleSubject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AddressFactory
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BitpayInvoiceTarget
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.data.api.bitpay.BITPAY_LIVE_BASE
import piuk.blockchain.android.data.api.bitpay.PATH_BITPAY_INVOICE
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.customviews.account.AccountSelectSheet
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber

sealed class ScanResult(
    val isDeeplinked: Boolean
) {
    class HttpUri(
        val uri: String,
        isDeeplinked: Boolean
    ) : ScanResult(isDeeplinked)

    class TransactionTarget(
        val targets: Set<ReceiveAddress>,
        isDeeplinked: Boolean
    ) : ScanResult(isDeeplinked)
}

object QrScanHandler {

    const val SCAN_URI_RESULT = 12007

    fun requestScanPermissions(
        activity: Activity,
        rootView: View,
        onSuccess: () -> Unit
    ) {
        val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
            .with(rootView, R.string.request_camera_permission)
            .withButton(android.R.string.ok) {
                requestScanPermissions(
                    activity,
                    rootView,
                    onSuccess
                )
            }
            .build()

        val grantedPermissionListener = CameraPermissionListener(
            granted = onSuccess
        )

        val compositePermissionListener =
            CompositePermissionListener(deniedPermissionListener, grantedPermissionListener)

        Dexter.withActivity(activity)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(compositePermissionListener)
            .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
            .check()
    }

    fun startQrScanActivity(activity: Activity, appUtil: AppUtil) {
        if (!appUtil.isCameraOpen) {
            val intent = Intent(activity, CaptureActivity::class.java)
            activity.startActivityForResult(intent, SCAN_URI_RESULT)
        } else {
            errorCameraBusy(activity)
        }
    }

    fun startQrScanActivity(fragment: Fragment, appUtil: AppUtil) {
        if (!appUtil.isCameraOpen) {
            val intent = Intent(fragment.requireContext(), CaptureActivity::class.java)
            fragment.startActivityForResult(intent, SCAN_URI_RESULT)
        } else {
            errorCameraBusy(fragment.requireContext())
        }
    }

    private fun logQrScanAnalytics(analytics: Analytics, sourceView: String) {
        analytics.logEvent(object : AnalyticsEvent {
            override val event = "qr_scan_requested"
            override val params = mapOf("fragment" to sourceView)
        })
    }

    private fun errorCameraBusy(ctx: Context) {
        ToastCustom.makeText(
            ctx,
            ctx.getText(R.string.camera_unavailable),
            ToastCustom.LENGTH_SHORT,
            ToastCustom.TYPE_ERROR
        )
    }

    fun processScan(scanResult: String, isDeeplinked: Boolean = false): Single<ScanResult> =
        when {
            scanResult.isHttpUri() -> Single.just(ScanResult.HttpUri(scanResult, isDeeplinked))
            scanResult.isBitpayUri() -> Single.just(
                ScanResult.TransactionTarget(parseBitpayInvoice(scanResult), isDeeplinked)
            )
            else -> {
                val addressParser: AddressFactory = payloadScope.get()
                addressParser.parse(scanResult)
                    .map {
                        ScanResult.TransactionTarget(it, isDeeplinked)
                    }
            }
        }

    // Temp hack. This should be processed and checked for validity by the Address Parser
    // Do this as part of BTC new send impl TODO
    private fun parseBitpayInvoice(bitpayUri: String) =
        setOf(
            BitpayInvoiceTarget(
                asset = CryptoCurrency.BTC,
                invoiceUrl = bitpayUri
            )
        )

    fun disambiguateScan(activity: Activity, targets: Collection<CryptoAddress>): Single<CryptoAddress> {
        // TEMP while refactoring - replace with bottom sheet.
        val optionsList = ArrayList(targets)
        val selectList = optionsList.map { it.asset.name }.toTypedArray()

        val subject = SingleSubject.create<CryptoAddress>()

        AlertDialog.Builder(activity, R.style.AlertDialogStyle)
            .setTitle(R.string.confirm_currency)
            .setCancelable(true)
            .setSingleChoiceItems(
                selectList,
                -1
            ) { dialog, which ->
                subject.onSuccess(optionsList[which])
                dialog.dismiss()
            }
            .create()
            .show()

        return subject
    }

    fun selectAssetTargetFromScan(
        asset: CryptoCurrency,
        scanResult: ScanResult
    ): Maybe<CryptoAddress> =
        Maybe.just(scanResult)
            .filter { r -> r is ScanResult.TransactionTarget }
            .map { r ->
                (r as ScanResult.TransactionTarget).targets
                .filterIsInstance<CryptoAddress>()
                .first { a -> a.asset == asset }
            }

    @SuppressLint("CheckResult")
    fun selectSourceAccount(
        activity: BlockchainActivity,
        target: CryptoAddress
    ): Maybe<CryptoAccount> {
        // TODO: We currently only support sending to external addresses from a non-custodial
        // account. At some point, this will - maybe - change and we'll have to implement
        // a 'can send from' method on coincore.

        val subject = MaybeSubject.create<CryptoAccount>()

        val asset = target.asset
        val coincore = payloadScope.get<Coincore>()

        coincore[asset].accountGroup(AssetFilter.NonCustodial)
            .subscribeBy(
                onSuccess = {
                    when (it.accounts.size) {
                        1 -> subject.onSuccess(it.accounts[0] as CryptoAccount)
                        0 -> throw IllegalStateException("No account found")
                        else -> showAccountSelectionDialog(
                            activity, subject, Single.just(it.accounts)
                        )
                    }
                },
                onComplete = {
                    subject.onComplete()
                },
                onError = {
                    subject.onError(it)
                }
            )
        return subject
    }

    private fun showAccountSelectionDialog(
        activity: BlockchainActivity,
        subject: MaybeSubject<CryptoAccount>,
        source: Single<SingleAccountList>
    ) {
        val selectionHost = object : AccountSelectSheet.SelectionHost {
            override fun onAccountSelected(account: BlockchainAccount) {
                subject.onSuccess(account as CryptoAccount)
            }

            override fun onSheetClosed() {
                if (!subject.hasValue())
                    subject.onComplete()
            }
        }

        activity.showBottomSheet(
            AccountSelectSheet.newInstance(
                selectionHost,
                source.map { list ->
                    list.map { it as CryptoAccount }
                },
                R.string.select_send_source_title
            )
        )
    }
}

private fun String.isHttpUri(): Boolean = startsWith("http")

private const val bitpayInvoiceUrl = "$BITPAY_LIVE_BASE$PATH_BITPAY_INVOICE/"
private fun String.isBitpayUri(): Boolean {
    val amount = FormatsUtil.getBitcoinAmount(this)
    val paymentRequestUrl = FormatsUtil.getPaymentRequestUrl(this)
    return amount == "0.0000" && paymentRequestUrl.contains(bitpayInvoiceUrl)
}