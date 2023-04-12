package com.barcoderecognition.opengl

import android.opengl.GLES20

class GlShader(private val shaderId: Int) {

    fun attach(programId: Int) {
        GLES20.glAttachShader(programId, shaderId)
    }

    companion object {
        fun create(shaderCode: String, type: ShaderType): GlShader {
            val shaderType = when (type) {
                ShaderType.FRAGMENT -> GLES20.GL_FRAGMENT_SHADER
                ShaderType.VERTEX -> GLES20.GL_VERTEX_SHADER
            }
            var shader = GLES20.glCreateShader(shaderType)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            // Get the compilation status.
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(shader)
                shader = 0
            }

            if (shader == 0) {
                throw RuntimeException("Error creating shader.")
            }

            return GlShader(shader)
        }
    }

    enum class ShaderType {
        FRAGMENT, VERTEX
    }
}