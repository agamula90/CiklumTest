package com.barcoderecognition.opengl

import android.opengl.GLES20
import com.barcoderecognition.helpers.assertNoGlErrors

open class GlProgram(
    vertexShader: GlShader,
    fragmentShader: GlShader,
    meshCoords: FloatArray,
    meshAttributeName: String,
    updatableGlAttributeNames: List<String>
) {
    private val programHandle: Int = GLES20.glCreateProgram().also { programId ->
        vertexShader.attach(programId)
        fragmentShader.attach(programId)
        GLES20.glLinkProgram(programId)
    }
    val mesh = GlAttribute(getAttributeHandle(meshAttributeName), meshCoords)
    val updatableGlAttributes = updatableGlAttributeNames.associateWith {
        UpdatableGlAttribute(getAttributeHandle(it))
    }

    private fun getAttributeHandle(attributeName: String) =
        GLES20.glGetAttribLocation(programHandle, attributeName)

    protected fun getUniformHandle(uniformName: String) =
        GLES20.glGetUniformLocation(programHandle, uniformName)

    protected open fun prepareDraw() {
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
    }

    protected open fun onDraw() {}

    protected open fun postDraw() {
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
    }

    fun draw() {
        prepareDraw()
        GLES20.glUseProgram(programHandle)
        assertNoGlErrors("after use")
        onDraw()
        assertNoGlErrors("after draw")
        mesh.draw()
        updatableGlAttributes.forEach { it.value.draw() }
        val countTrianglesToRender = mesh.countTrianglesToRender + updatableGlAttributes.values.sumOf {
            it.getCountOfTrianglesToRender()
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, countTrianglesToRender)
        assertNoGlErrors("after triangles draw")
        mesh.close()
        updatableGlAttributes.forEach { it.value.close() }
        assertNoGlErrors("after attributes close")
        postDraw()
        assertNoGlErrors("after post draw")
    }
}