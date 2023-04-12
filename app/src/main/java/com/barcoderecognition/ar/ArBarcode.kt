package com.barcoderecognition.ar

import com.barcoderecognition.opengl.GlColor
import com.barcoderecognition.opengl.GlProgram
import com.barcoderecognition.opengl.GlShader

// x,y - coordinates in plane's space
class ArBarcode private constructor(
    private val x: FloatArray,
    private val y: FloatArray,
    val code: String,
    private val program: GlProgram,
    private var color: GlColor
) {

    fun setColor(color: GlColor) {
        //TODO adjust uniform
    }

    fun containsPoint(x: Float, y: Float): Boolean {
        return false //TODO validate if x,y point is inside barcode rect
    }

    fun draw(
        modelViewProjection: FloatArray,
        cameraPosition: FloatArray,
        anchorPosition: FloatArray
    ) {
        program.draw()
    }

    companion object {
        private val fragmentShaderCode = """
            |precision mediump float;
            |uniform vec4 u_Color;
            |
            |void main() {
            |  gl_FragColor = u_Color;
            |}
            |""".trimMargin()

        private fun Float.getVertexShaderCodeByNearestPlaneValue(): String {
            return """
                |attribute vec2 a_Position;
                |
                |uniform mat4 u_ModelViewProjection;
                |uniform vec3 u_CameraPosition;
                |uniform vec3 u_AnchorPosition;
                |
                |void main() {
                |  vec3 labelNormal = normalize(u_CameraPosition - u_AnchorPosition);
                |  vec3 labelSide = -cross(labelNormal, vec3(0.0, 1.0, 0.0));
                |  vec3 modelPosition = u_AnchorPosition + a_Position.x * $this * labelSide + a_Position.y * $this * vec3(0.0, 1.0, 0.0);
                |  gl_Position = u_ModelViewProjection * vec4(modelPosition, 1.0);
                |}
                |""".trimMargin()
        }

        fun create(
            x: FloatArray,
            y: FloatArray,
            code: String,
            color: GlColor,
            nearestPlane: Float
        ): ArBarcode {
            val meshCoords = floatArrayOf(
                x[0], y[0], x[1], y[1], x[2], y[2], x[3], y[3]
            )

            val program = GlProgram(
                vertexShader = GlShader.create(
                    nearestPlane.getVertexShaderCodeByNearestPlaneValue(),
                    type = GlShader.ShaderType.VERTEX
                ),
                fragmentShader = GlShader.create(
                    fragmentShaderCode,
                    type = GlShader.ShaderType.FRAGMENT
                ),
                meshCoords = meshCoords,
                meshAttributeName = "a_Position",
                updatableGlAttributeNames = listOf("u_Color")
            )

            return ArBarcode(
                x = x,
                y = y,
                code = code,
                program = program,
                color = color
            )
        }
    }
}