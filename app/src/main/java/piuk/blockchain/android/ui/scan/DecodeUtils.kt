package piuk.blockchain.android.ui.scan

import android.content.Intent
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import timber.log.Timber
import java.util.EnumMap
import java.util.EnumSet

fun parseDecodeHints(intent: Intent): Map<DecodeHintType, Any> {

    intent.extras?.let { extras ->
        if (extras.isEmpty) {
            return mapOf()
        }
        val hints: MutableMap<DecodeHintType, Any> = EnumMap(
            DecodeHintType::class.java
        )

        DecodeHintType.values().filterNot { hintType ->
            hintType == DecodeHintType.CHARACTER_SET ||
                hintType == DecodeHintType.NEED_RESULT_POINT_CALLBACK ||
                hintType == DecodeHintType.POSSIBLE_FORMATS
        }
            .map { hintType ->
                extras.get(hintType.name)?.let { hintData ->
                    when {
                        hintType.valueType == Void::class.java -> hints[hintType] = true
                        hintType.valueType.isInstance(hintData) -> hints[hintType] = hintData
                        else -> Timber.i(
                            "Ignoring hint $hintType because it is not assignable from $hintData"
                        )
                    }
                }
            }
        Timber.i("Hints from the Intent: %s", hints)
        return hints
    }
    return mapOf()
}

fun parseDecodeFormats(
    intent: Intent,
    productBarcodeFormats: Collection<BarcodeFormat>,
    oneDBarcodeFormats: Collection<BarcodeFormat>
): Collection<BarcodeFormat> {

    val scanFormats: List<String>? = intent.getStringExtra(QrScanIntents.FORMATS)?.split(",")
    val decodeMode = intent.getStringExtra(QrScanIntents.MODE)
    val barcodeFormats = EnumSet.noneOf(BarcodeFormat::class.java)

    return when {
        scanFormats != null -> {
            try {
                scanFormats.map {
                    BarcodeFormat.valueOf(it)
                }.toCollection(barcodeFormats)
            } catch (exception: IllegalArgumentException) {
                // ignore it then
                Timber.e(exception)
                barcodeFormats
            }
        }
        decodeMode == QrScanIntents.PRODUCT_MODE -> productBarcodeFormats
        decodeMode == QrScanIntents.QR_CODE_MODE -> EnumSet.of(BarcodeFormat.QR_CODE)
        decodeMode == QrScanIntents.DATA_MATRIX_MODE -> EnumSet.of(BarcodeFormat.DATA_MATRIX)
        decodeMode == QrScanIntents.ONE_D_MODE -> oneDBarcodeFormats
        else -> barcodeFormats
    }
}