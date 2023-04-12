package com.barcoderecognition.opengl

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.barcoderecognition.helpers.directIntBufferOfSize

class ExternalTextureUniform(private val uniformId: Int) {

    private val textures = directIntBufferOfSize(1).also { GLES20.glGenTextures(1, it) }
    val textureId: Int = textures[0]

    init {
        val textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        GLES20.glBindTexture(textureTarget, textureId)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    fun prepare() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
    }

    fun draw() {
        GLES20.glUniform1i(uniformId, 0)
    }
}