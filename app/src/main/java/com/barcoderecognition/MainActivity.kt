package com.barcoderecognition

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.barcoderecognition.ar.*
import com.barcoderecognition.helpers.BarcodeAnalyser
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

class MainActivity : AppCompatActivity() {
    private lateinit var arSession: ArSession
    private lateinit var surfaceView: ArSurfaceView
    private lateinit var scope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        scope = CoroutineScope(EmptyCoroutineContext)
        val frame = FrameLayout(this)
        surfaceView = ArSurfaceView(this).also { frame.addView(it) }
        setContentView(frame)

        val virtualObjects = VirtualObjects { x1, y1 ->
            runOnUiThread {
                val background =
                    View(this).also { it.setBackgroundColor(Color.argb(100, 200, 10, 10)) }
                frame.addView(
                    background,
                    FrameLayout.LayoutParams(10, 10)
                        .also { it.marginStart = x1;it.topMargin = y1 })
            }
        }

        val analyser = BarcodeAnalyser(scope, virtualObjects)

        arSession = ArSession(this, scope, analyser)
        surfaceView.setup(arSession, virtualObjects)

        DefaultDisplayObserver(this) {
            arSession.updateDeviceRotation(it.rotation)
        }
    }

    override fun onResume() {
        super.onResume()
        arSession.resume()
        surfaceView.onResume()
    }

    override fun onPause() {
        surfaceView.onPause()
        arSession.close(isFinishing)
        super.onPause()
    }
}