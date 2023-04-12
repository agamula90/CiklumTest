package com.barcoderecognition.ar

import android.graphics.Color
import android.opengl.Matrix
import com.google.ar.core.*
import com.barcoderecognition.model.Barcode
import com.barcoderecognition.opengl.GlColor

private const val Z_NEAR = 0.1f
private const val Z_FAR = 100f
private val colorUnselected = GlColor(Color.argb(100, 10, 200, 10))
private val colorSelected = GlColor(Color.argb(100, 200, 10, 10))

class VirtualObjects(private val objectsFound: (Int, Int) -> Unit) {

    private val items: MutableMap<Anchor, ArBarcode> = mutableMapOf()

    private val modelViewProjectionMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val barcodeCheckCoordsIn = FloatArray(3)
    private val barcodeCheckCoordsOut = FloatArray(3)
    private val anchorMatrix = FloatArray(16)
    private val cpuImageCoordinates = FloatArray(2)
    private val viewCoordinates = FloatArray(2)
    private var pendingBarcodes = mutableListOf<Barcode>()

    private var areSomeObjectsDetected = false


    fun attachPendingBarcodes(frame: ArFrame) {
        if (pendingBarcodes.isNotEmpty()) {
            val newBarcodes = pendingBarcodes.also { pendingBarcodes = mutableListOf() }
            val notProcessedBarcodes = mutableListOf<Barcode>()
            for (barcode in newBarcodes) {
                cpuImageCoordinates[0] = barcode.area.exactCenterX()
                cpuImageCoordinates[1] = barcode.area.exactCenterY()

                val barcodeCenterHitResult =
                    frame.getPlaneHitResult(cpuImageCoordinates, viewCoordinates)

                if (!areSomeObjectsDetected) {
                    objectsFound(viewCoordinates[0].toInt(), viewCoordinates[1].toInt())
                    areSomeObjectsDetected = true
                }

                if (barcodeCenterHitResult == null) {
                    notProcessedBarcodes.add(barcode)
                    continue
                }

                barcodeCenterHitResult.hitPose.getTranslation(barcodeCheckCoordsIn, 0)
                for (item in items.filterKeys { it.trackingState == TrackingState.TRACKING }) {
                    item.key.pose.transformPoint(barcodeCheckCoordsIn, 0, barcodeCheckCoordsOut, 0)
                    val x = barcodeCheckCoordsOut[0]
                    val y = barcodeCheckCoordsOut[2]
                    if (item.value.containsPoint(x, y)) {
                        return
                    }
                }

                val anchor = barcodeCenterHitResult.createAnchor()

                //TODO find barcode coordinates in plane x-z coordinate space + sort them
                val x = floatArrayOf(0f, 0f, 0f, 0f)
                val y = floatArrayOf(0f, 0f, 0f, 0f)

                items[anchor] =
                    ArBarcode.create(x, y, barcode.code, colorUnselected, Z_NEAR)
            }

            pendingBarcodes.addAll(notProcessedBarcodes)
        }
    }

    fun draw(frame: ArFrame) {
        val trackingItems = items.filterKeys { it.trackingState == TrackingState.TRACKING }
        if (trackingItems.isEmpty()) {
            return
        }
        val camera = frame.camera
        camera.pose.toMatrix(modelMatrix, 0)
        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewMatrix, 0, projectionMatrix, 0)

        for (item in trackingItems) {
            item.key.pose.toMatrix(anchorMatrix, 0)
            item.value.draw(modelViewProjectionMatrix, modelMatrix, anchorMatrix)
        }
    }

    fun addBarcodes(barcodes: List<Barcode>) {
        pendingBarcodes.addAll(barcodes)
    }

    fun select(frame: ArFrame, x: Float, y: Float) {
        val activeAnchors = items.filterKeys { it.trackingState == TrackingState.TRACKING }
        val hitResults = frame.getHitResults(x, y)

        var minDistanceFromCastRayToAnchors = 0
        var barcodeToSelect: ArBarcode? = null
        for (anchor in activeAnchors) {
            var distanceFromCastRay = 0
            for (hitResult in hitResults) {
                //TODO find distance from hit result to arbarcodes
                distanceFromCastRay += 2
            }
            if (distanceFromCastRay < minDistanceFromCastRayToAnchors) {
                minDistanceFromCastRayToAnchors = distanceFromCastRay
                barcodeToSelect = anchor.value
            }
        }

        barcodeToSelect?.setColor(colorSelected)
    }
}