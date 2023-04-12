package com.barcoderecognition.helpers

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

private const val TAG = "GL_ERROR"

fun assertNoGlErrors(tag: String) {
    var errorMessage = ""

    var errorCode: Int
    while (GLES20.glGetError().also { errorCode = it } != GLES20.GL_NO_ERROR) {
        errorMessage = "$tag: glError $errorCode".also { message -> Log.e(TAG, message) }
    }

    if (errorMessage.isNotEmpty()) {
        throw RuntimeException(errorMessage)
    }
}

fun directFloatBufferOfSize(size: Int): FloatBuffer {
    val directBuffer = ByteBuffer.allocateDirect(size * Float.SIZE_BYTES).also {
        it.order(ByteOrder.nativeOrder())
    }
    return directBuffer.asFloatBuffer()
}

fun directIntBufferOfSize(size: Int): IntBuffer {
    val directBuffer = ByteBuffer.allocateDirect(size * Int.SIZE_BYTES).also {
        it.order(ByteOrder.nativeOrder())
    }
    return directBuffer.asIntBuffer()
}