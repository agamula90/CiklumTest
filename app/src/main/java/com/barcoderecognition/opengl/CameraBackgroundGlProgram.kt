package com.barcoderecognition.opengl

import android.opengl.GLES20
import com.barcoderecognition.helpers.directFloatBufferOfSize

class CameraBackgroundGlProgram(
    vertexShader: GlShader,
    fragmentShader: GlShader,
    meshCoords: FloatArray,
    meshAttributeName: String,
    externalUniformName: String,
    textureCoordsAttributeName: String,
) : GlProgram(
    vertexShader = vertexShader,
    fragmentShader = fragmentShader,
    meshCoords = meshCoords,
    meshAttributeName = meshAttributeName,
    updatableGlAttributeNames = listOf(textureCoordsAttributeName)
) {
    val textureCoordsAttribute = updatableGlAttributes[textureCoordsAttributeName]!!.also {
        it.setBuffer(directFloatBufferOfSize(meshCoords.size))
    }
    private val cameraTexture = ExternalTextureUniform(getUniformHandle(externalUniformName))
    val cameraTextureId = cameraTexture.textureId

    override fun prepareDraw() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        cameraTexture.prepare()
    }

    override fun onDraw() {
        cameraTexture.draw()
    }

    override fun postDraw() {}
}