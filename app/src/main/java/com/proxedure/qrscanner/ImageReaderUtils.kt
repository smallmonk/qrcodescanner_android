package com.proxedure.qrscanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

object ImageReaderUtils {

    fun scanQrFromUri(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val firstBarcode = barcodes.firstOrNull { it.rawValue != null }
                        onResult(firstBarcode?.rawValue)
                    } else {
                        onResult(null)
                    }
                }
                .addOnFailureListener {
                    onResult(null)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(null)
        }
    }
}
