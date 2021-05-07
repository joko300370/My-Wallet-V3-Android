/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package piuk.blockchain.android.ui.scan

/**
 * This class provides the constants to use when sending an Intent to Barcode Scanner.
 * These strings are effectively API and cannot be changed.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
object QrScanIntents {
    /**
     * Send this intent to open the Barcodes app in scanning mode, find a barcode, and return
     * the results.
     */
    const val ACTION = "com.google.zxing.client.android.SCAN"

    /**
     * By default, sending this will decode all barcodes that we understand. However it
     * may be useful to limit scanning to certain formats. Use
     * [android.content.Intent.putExtra] with one of the values below.
     *
     * Setting this is effectively shorthand for setting explicit formats with [.FORMATS].
     * It is overridden by that setting.
     */
    const val MODE = "SCAN_MODE"

    /**
     * Decode only UPC and EAN barcodes. This is the right choice for shopping apps which get
     * prices, reviews, etc. for products.
     */
    const val PRODUCT_MODE = "PRODUCT_MODE"

    /**
     * Decode only 1D barcodes.
     */
    const val ONE_D_MODE = "ONE_D_MODE"

    /**
     * Decode only QR codes.
     */
    const val QR_CODE_MODE = "QR_CODE_MODE"

    /**
     * Decode only Data Matrix codes.
     */
    const val DATA_MATRIX_MODE = "DATA_MATRIX_MODE"

    /**
     * Comma-separated list of formats to scan for. The values must match the names of
     * [com.google.zxing.BarcodeFormat]s, e.g. [com.google.zxing.BarcodeFormat.EAN_13].
     * Example: "EAN_13,EAN_8,QR_CODE". This overrides [.MODE].
     */
    const val FORMATS = "SCAN_FORMATS"

    /**
     * @see com.google.zxing.DecodeHintType.CHARACTER_SET
     */
    const val CHARACTER_SET = "CHARACTER_SET"
}