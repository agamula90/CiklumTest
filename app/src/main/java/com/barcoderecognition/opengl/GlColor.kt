package com.barcoderecognition.opengl

import android.graphics.Color
import android.opengl.GLES20

class GlColor(private val color: Int) {

    private val openGlColor = FloatArray(4).also {
        it[0] = Color.red(color) / 255f
        it[1] = Color.green(color) / 255f
        it[2] = Color.blue(color) / 255f
        it[3] = Color.alpha(color) / 255f
    }

    fun bind(uniformId: Int) {
        GLES20.glUniform4fv(uniformId, 1, openGlColor, 0)
    }
}