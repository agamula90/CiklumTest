package com.barcoderecognition.ar

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ArSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private lateinit var arCamera: ArCamera
    private lateinit var arSession: ArSession
    private lateinit var virtualObjects: VirtualObjects

    private var upX = -1f
    private var upY = -1f

    private val touchListener = object : OnTouchListener {
        private var isScrolling = false

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean = when {
            event.action == MotionEvent.ACTION_CANCEL -> {
                isScrolling = false
                true
            }
            event.action == MotionEvent.ACTION_UP && !isScrolling -> {
                upX = event.x
                upY = event.y
                isScrolling = false
                true
            }
            else -> false
        }
    }

    fun setup(newArSession: ArSession, newVirtualObjects: VirtualObjects) {
        if (this::arSession.isInitialized) {
            arSession.close(isFinishing = true)
        }
        arSession = newArSession
        virtualObjects = newVirtualObjects
    }

    init {
        preserveEGLContextOnPause = true
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setOnTouchListener(touchListener)
        setRenderer(object: Renderer {

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                GLES20.glClearColor(0f, 0f, 0f, 1.0f)
                arCamera = ArCamera.create()
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
                arSession.updateViewportSize(width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
                val frame = arSession.frames(arCamera.textureId) ?: return

                arCamera.draw(frame)

                val touchedX = upX.also { upX = -1f }
                val touchedY = upY.also { upY = -1f }
                if (touchedX != -1f && touchedY != -1f) {
                    virtualObjects.select(frame, touchedX, touchedY)
                }

                virtualObjects.attachPendingBarcodes(frame)
                virtualObjects.draw(frame)
            }
        })
        renderMode = RENDERMODE_CONTINUOUSLY
    }

}