package com.barcoderecognition.helpers

import android.graphics.Rect
import android.media.Image
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.barcoderecognition.ar.ArFrame
import com.barcoderecognition.ar.VirtualObjects
import com.barcoderecognition.model.Barcode
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executors

private const val DEBOUNCE_TIMEOUT = 1000L

class BarcodeAnalyser(
    private val scope: CoroutineScope,
    private val virtualObjects: VirtualObjects
) {
    private val client = BarcodeScanning.getClient()
    private val analysisDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private var processedSuccessfully = false
    private var processingJob: Job = Job().also { it.complete() }
    var rotation: Int = 0

    fun process(frame: ArFrame, validCropRect: Rect) {
        if (processingJob.isCompleted) {
            val image = frame.acquireCameraImage()
            if (image == null) {
                processingJob = scope.launch(analysisDispatcher) {
                    delay(DEBOUNCE_TIMEOUT)
                }
                return
            }

            val newImage = InputImage.fromMediaImage(image, rotation)

            processingJob = scope.launch(analysisDispatcher) {
                coroutineScope {
                    launch(Dispatchers.IO) { delay(DEBOUNCE_TIMEOUT) }
                    launch(Dispatchers.IO) inner@{
                        if (processedSuccessfully) return@inner
                        doProcess(newImage, image, validCropRect)
                    }
                }
            }
        }
    }

    //TODO most likely we should use depth testing instead of hoping that camera position won't change during barcode detection
    private suspend fun doProcess(newImage: InputImage, image: Image, validCropRect: Rect) {
        val rotatedCropRect = when(rotation) {
            0 -> validCropRect
            else -> Rect(validCropRect.top, validCropRect.left, validCropRect.bottom, validCropRect.right)
        }

        val results = client.process(newImage).await()
        val barcodes = results.filter {
            val boundingBox = it.boundingBox
            if (boundingBox != null) {
                val isBoundingBoxInsideCropRect = rotatedCropRect.contains(boundingBox)
                isBoundingBoxInsideCropRect && it.rawValue != null
            } else {
                false
            }
        }.map {
            Barcode(it.boundingBox!!.toRotatedRect(), it.rawValue!!)
        }

        virtualObjects.addBarcodes(barcodes)
        image.close()
        if (barcodes.isNotEmpty()) {
            processedSuccessfully = true
        }
    }

    private fun Rect.toRotatedRect() = when(rotation) {
        0 -> this
        else -> Rect(top, left, bottom, right)
    }
}