package com.barcoderecognition.ar

import android.util.Size

data class DeviceGeometry(val rotation: Int = 0, val viewportSize: Size = Size(0, 0)) {
    fun isViewportValid() = viewportSize.width != 0 && viewportSize.height != 0
}