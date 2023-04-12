package com.barcoderecognition.ar

import android.hardware.display.DisplayManager
import android.view.Display
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class DefaultDisplayObserver(activity: AppCompatActivity, private val callback: (Display) -> Unit) {
    private val displayManager: DisplayManager = activity.getSystemService(DisplayManager::class.java)
    private val displayListener = object: DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}

        override fun onDisplayRemoved(displayId: Int) {}

        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                callback(displayManager.getDisplay(displayId))
            }
        }
    }
    private val lifecycleObserver = object: DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            callback(display)
            displayManager.registerDisplayListener(displayListener, null)
        }

        override fun onPause(owner: LifecycleOwner) {
            displayManager.unregisterDisplayListener(displayListener)
        }
    }

    init {
        activity.lifecycle.addObserver(lifecycleObserver)
    }
}