package com.barcoderecognition.ar

import com.barcoderecognition.opengl.CameraBackgroundGlProgram
import com.barcoderecognition.opengl.GlShader

class ArCamera private constructor(private val program: CameraBackgroundGlProgram) {
    val textureId = program.cameraTextureId

    fun draw(frame: ArFrame) {
        if (frame.hasDisplayGeometryChanged()) {
            frame.fromOpenGlCoordinatesToTextureCoordinates(
                program.mesh.buffer,
                program.textureCoordsAttribute.buffer
            )
        }
        if (frame.timestamp != 0L) program.draw()
    }

    companion object {
        private val fragmentShaderCode = """
            |#extension GL_OES_EGL_image_external : require
            |
            |precision mediump float;
            |varying vec2 v_TextureCoords;
            |uniform samplerExternalOES u_Texture;
            |
            |void main() {
            |  gl_FragColor = texture2D(u_Texture, v_TextureCoords);
            |}
            |""".trimMargin()

        private val vertexShaderCode = """
            |attribute vec4 a_CameraCoords;
            |attribute vec2 a_TextureCoords;
            |varying vec2 v_TextureCoords;
            |
            |void main() {
            |  gl_Position = a_CameraCoords;
            |  v_TextureCoords = a_TextureCoords;
            |}
            |""".trimMargin()

        /**
         * (-1, 1) ------- (1, 1)
         *   |    \           |
         *   |       \        |
         *   |          \     |
         *   |             \  |
         * (-1, -1) ------ (1, -1)
         * Ensure triangles are front-facing, to support glCullFace().
         * This quad will be drawn using GL_TRIANGLE_STRIP which draws two
         * triangles: v0->v1->v2, then v2->v1->v3.
         */
        private val MESH_COORDS = floatArrayOf(
            -1.0f, -1.0f, +1.0f, -1.0f, -1.0f, +1.0f, +1.0f, +1.0f
        )

        fun create(): ArCamera {
            val fragmentShader = GlShader.create(fragmentShaderCode, GlShader.ShaderType.FRAGMENT)
            val vertexShader = GlShader.create(vertexShaderCode, GlShader.ShaderType.VERTEX)
            return ArCamera(
                CameraBackgroundGlProgram(
                    vertexShader = vertexShader,
                    fragmentShader = fragmentShader,
                    meshCoords = MESH_COORDS,
                    meshAttributeName = "a_CameraCoords",
                    externalUniformName = "u_Texture",
                    textureCoordsAttributeName = "a_TextureCoords"
                )
            )
        }
    }
}