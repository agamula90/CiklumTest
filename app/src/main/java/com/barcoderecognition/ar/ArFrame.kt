package com.barcoderecognition.ar

import android.media.Image
import android.util.Log
import com.google.ar.core.*
import java.nio.FloatBuffer

private const val TAG = "FrameTests"

class ArFrame(private val frame: Frame) {

    val timestamp: Long
        get() = frame.timestamp

    val camera = frame.camera

    fun acquireCameraImage(): Image? = try {
        frame.acquireCameraImage()
    } catch (e: Exception) {
        null
    }

    fun getPlaneHitResult(cpuCoordinates: FloatArray, viewCoordinates: FloatArray): HitResult? {
        frame.transformCoordinates2d(
            Coordinates2d.IMAGE_PIXELS,
            cpuCoordinates,
            Coordinates2d.VIEW,
            viewCoordinates
        )
        val tests = frame.hitTest(viewCoordinates[0], viewCoordinates[1])
        val plane = tests
            .firstOrNull { (it.trackable as? Plane)?.type == Plane.Type.VERTICAL }
        if (plane != null) {
            return plane
        }

        if (tests.isNotEmpty()) {
            Log.e(
                TAG,
                "trackables found: ${tests.joinToString { it.asString() }}"
            )
        }
        return plane
    }

    private fun HitResult.asString(): String {
        val plane = trackable as? Plane
        if (plane != null) {
            return "plane: ${plane.type}"
        }
        val point = trackable as? Point
        if (point != null) {
            return "point: ${point.orientationMode}"
        }
        return trackable.javaClass.simpleName
    }

    fun getHitResults(x: Float, y: Float): List<HitResult> {
        return frame.hitTest(x, y)
    }

    fun hasDisplayGeometryChanged(): Boolean {
        return frame.hasDisplayGeometryChanged()
    }

    fun fromOpenGlCoordinatesToTextureCoordinates(
        openGlCoordinates: FloatBuffer,
        textureCoordinates: FloatBuffer
    ) {
        frame.transformCoordinates2d(
            Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
            openGlCoordinates,
            Coordinates2d.TEXTURE_NORMALIZED,
            textureCoordinates
        )
    }
}