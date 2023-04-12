package com.barcoderecognition.opengl

import android.opengl.GLES20
import android.opengl.GLES30
import com.barcoderecognition.helpers.directFloatBufferOfSize
import java.nio.FloatBuffer

private const val COORDS_PER_VERTEX = 2

class UpdatableGlAttribute(
    private val glHandle: Int,
    private val numberOfEntriesPerVertex: Int = 2
) : AutoCloseable {
    private val bufferId = intArrayOf(0)
    private var capacity = 0
    private var size = 0
    private var _buffer: FloatBuffer? = null
    val buffer: FloatBuffer
        get() = _buffer!!

    init {
        GLES20.glGenBuffers(1, bufferId, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId[0])
    }

    fun setBuffer(newBuffer: FloatBuffer) {
        GLES30.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId[0])
        newBuffer.rewind()
        if (newBuffer.limit() <= capacity) {
            GLES30.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER,
                0,
                newBuffer.limit() * Int.SIZE_BYTES,
                newBuffer
            )
            size = newBuffer.limit() / numberOfEntriesPerVertex
        } else {
            GLES30.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                newBuffer.limit() * Int.SIZE_BYTES,
                newBuffer,
                GLES30.GL_DYNAMIC_DRAW
            )
            size = newBuffer.limit() / numberOfEntriesPerVertex
            capacity = newBuffer.limit()
        }
        _buffer = newBuffer
    }

    fun draw() {
        GLES20.glVertexAttribPointer(
            glHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            0,
            buffer
        )
        GLES20.glEnableVertexAttribArray(glHandle)
    }

    fun getCountOfTrianglesToRender(): Int {
        assert(numberOfEntriesPerVertex == 2) {
            "Only 2d vertexes supported"
        }
        return size - 2
    }

    override fun close() {
        if (bufferId[0] != 0) {
            GLES20.glDeleteBuffers(1, bufferId, 0)
            bufferId[0] = 0
        }
    }
}

// coords are opengl x,y in cull face order
class GlAttribute(private val glHandle: Int, coords: FloatArray) : AutoCloseable {
    val buffer = directFloatBufferOfSize(coords.size).also {
        it.put(coords)
        it.position(0)
    }
    val countTrianglesToRender = buffer.capacity() / COORDS_PER_VERTEX - 2

    fun draw() {
        GLES20.glVertexAttribPointer(
            glHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            0,
            buffer
        )
        GLES20.glEnableVertexAttribArray(glHandle)
    }

    override fun close() {
        GLES20.glDisableVertexAttribArray(glHandle)
    }
}