package com.barcoderecognition.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

fun Context.showToast(message: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}