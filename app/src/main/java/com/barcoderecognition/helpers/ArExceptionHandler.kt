package com.barcoderecognition.helpers

import android.content.Context
import android.util.Log
import com.google.ar.core.exceptions.*
import com.barcoderecognition.ar.ArServicesNotUpToDate

interface ArExceptionHandler {
    fun handleException(e: Exception)

    fun runSafe(runnable: () -> Unit) = try {
        runnable()
    } catch (e: Exception) {
        handleException(e)
    }

    companion object {
        private const val TAG = "baf"

        fun createToastHandler(context: Context): ArExceptionHandler {
            return object: ArExceptionHandler {
                override fun handleException(e: Exception) {
                    if (e is ArServicesNotUpToDate) return

                    Log.e(TAG, e.message, e)

                    val message = when (e) {
                        is UnavailableUserDeclinedInstallationException ->
                            "Please install Google Play Services for AR"
                        is UnavailableApkTooOldException -> "Please update ARCore"
                        is UnavailableSdkTooOldException -> "Please update this app"
                        is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                        is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                        is MissingGlContextException -> "Tried gl rendering on thread without GL context: ${Thread.currentThread().name}"
                        is SessionPausedException -> "Tried AR with paused session"
                        is SecurityException -> "Tried AT without camera permission"
                        is IllegalStateException -> "Some AR acquired image is still opened"
                        is FatalException -> "AR fatal exception occurred: ${e.message}"
                        else -> ""
                    }

                    if (message.isNotEmpty()) context.showToast(message)
                }
            }
        }
    }
}