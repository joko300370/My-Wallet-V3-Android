package piuk.blockchain.android.ui.scan

import android.content.res.Configuration
import android.graphics.ImageFormat.YUV_420_888
import android.graphics.ImageFormat.YUV_422_888
import android.graphics.ImageFormat.YUV_444_888
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import timber.log.Timber
import java.nio.ByteBuffer
import kotlin.Exception

class QrCodeAnalyzer(
    private val targetRect: Rect,
    private val framingViewSize: Point,
    private val screenResolution: Point,
    private val orientation: Int,
    private val hints: Map<DecodeHintType, Any>,
    private val onQrCodesDetected: (qrCode: Result) -> Unit
) : ImageAnalysis.Analyzer {

    private val yuvFormats: List<Int> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        listOf(YUV_420_888, YUV_422_888, YUV_444_888)
    } else {
        listOf(YUV_420_888)
    }

    private val reader = MultiFormatReader().apply {
        setHints(hints)
    }

    private lateinit var framingRectInPreview: Rect

    override fun analyze(image: ImageProxy) {
        // We are using YUV format because, ImageProxy internally uses ImageReader to get the image
        // by default ImageReader uses YUV format unless changed.
        // https://developer.android.com/reference/androidx/camera/core/ImageProxy.html#getImage()
        // https://developer.android.com/reference/android/media/Image.html#getFormat()
        if (image.format !in yuvFormats) {
            Timber.e("QRCodeAnalyzer Expected: YUV, Actual: ${image.format}")
            return
        }
        try {
            if (::framingRectInPreview.isInitialized.not() || framingRectInPreview.isEmpty) {
                framingRectInPreview = getTargetRectInPreview(Point(image.width, image.height))
            }

            val source = PlanarYUVLuminanceSource(
                image.planes[0].buffer.toByteArray(),
                image.width,
                image.height,
                framingRectInPreview.left,
                framingRectInPreview.top,
                framingRectInPreview.width(),
                framingRectInPreview.height(),
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            // Whenever reader fails to detect a QR code in image
            // it throws NotFoundException
            val result = reader.decodeWithState(binaryBitmap)
            onQrCodesDetected(result)
        } catch (notFoundException: NotFoundException) {
            Timber.e(notFoundException)
        } catch (exception: Exception) {
            Timber.e("Unknown error while processing image: $exception")
        } finally {
            reader.reset()
            image.close()
        }
    }

    private fun getTargetRectInPreview(imageResolution: Point): Rect =
        when {
            targetRect.isEmpty -> targetRect
            orientation != Configuration.ORIENTATION_PORTRAIT ->
                Rect(
                    targetRect.left * imageResolution.x / screenResolution.x,
                    targetRect.top * imageResolution.y / screenResolution.y,
                    targetRect.right * imageResolution.x / screenResolution.x,
                    targetRect.bottom * imageResolution.y / screenResolution.y
                )
            else ->
                Rect(
                    targetRect.left * imageResolution.y / framingViewSize.x,
                    targetRect.top * imageResolution.x / framingViewSize.y,
                    targetRect.right * imageResolution.y / framingViewSize.x,
                    targetRect.bottom * imageResolution.x / framingViewSize.y
                )
            }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }
}