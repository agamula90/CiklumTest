package com.barcoderecognition.ar

import android.Manifest
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.*
import com.barcoderecognition.helpers.ArExceptionHandler
import com.barcoderecognition.helpers.BarcodeAnalyser
import com.barcoderecognition.helpers.showToast
import kotlinx.coroutines.*

class ArSession(
    private val activity: AppCompatActivity,
    private val scope: CoroutineScope,
    private val barcodeAnalyser: BarcodeAnalyser
) {
    private val arExceptionHandler = ArExceptionHandler.createToastHandler(activity)

    private val cameraPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                initSession()
            } else {
                activity.showToast("Please grant camera permission to proceed")
                    .also { activity.finish() }
            }
        }

    private var isArUnsupportedDevice = false
    private var isSessionRunning = false
    private var session: Session? = null
    private var deviceGeometry = DeviceGeometry()

    private var preparationStep = ArPreparationStep.NOT_VERIFIED
    private var camera: Camera? = null
    private var isDeviceRotationChanged = false

    private var areDeviationsInitialised = false
    private val cameraCropRect = Rect()

    val frames: (Int) -> ArFrame? = framesSection@{ cameraTextureId ->
        val currentSession = session ?: return@framesSection null

        val isFirstFrameWithSessionReady = camera == null
        if (isFirstFrameWithSessionReady) {
            currentSession.setCameraTextureName(cameraTextureId)
        }
        currentSession.setupToProduceFrames()

        val rawFrame = currentSession.update()
        if (!areDeviationsInitialised) {
            val cpuImageHeight = currentSession.cameraConfig.imageSize
            val gpuTextureSize = currentSession.cameraConfig.textureSize
            val gpuAspectRatio = gpuTextureSize.width.toFloat() / gpuTextureSize.height.toFloat()
            val cpuAspectRatio = cpuImageHeight.width.toFloat() / cpuImageHeight.height.toFloat()
            cameraCropRect.set(0, 0, cpuImageHeight.width, cpuImageHeight.height)
            if (gpuAspectRatio < cpuAspectRatio) {
                cameraCropRect.inset(((cpuImageHeight.width - cpuImageHeight.height * gpuAspectRatio) / 2).toInt(), 0)
            } else {
                cameraCropRect.inset(0, ((cpuImageHeight.height - cpuImageHeight.width / gpuAspectRatio) / 2).toInt())
            }
            areDeviationsInitialised = true
        }

        if (camera == null) camera = rawFrame.camera
        ArFrame(rawFrame).also { barcodeAnalyser.process(it, cameraCropRect) }
    }

    private val rotations = SparseIntArray().apply {
        append(Surface.ROTATION_0, 0)
        append(Surface.ROTATION_90, 90)
        append(Surface.ROTATION_180, 180)
        append(Surface.ROTATION_270, 270)
    }

    fun updateDeviceRotation(rotation: Int) {
        deviceGeometry = deviceGeometry.copy(rotation = rotation).also {
            if (it.isViewportValid()) {
                session?.also { s ->
                    s.setDisplayGeometry(it.rotation, it.viewportSize.width, it.viewportSize.height)
                }
            }
        }
        isDeviceRotationChanged = true
    }

    fun updateViewportSize(viewportWidth: Int, viewportHeight: Int) {
        deviceGeometry =
            deviceGeometry.copy(viewportSize = Size(viewportWidth, viewportHeight)).also {
                if (it.isViewportValid()) {
                    session?.also { s ->
                        s.setDisplayGeometry(
                            it.rotation,
                            it.viewportSize.width,
                            it.viewportSize.height
                        )
                    }
                }
            }
    }

    fun resume() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun close(isFinishing: Boolean) {
        if (isSessionRunning) {
            session?.pause()
            isSessionRunning = false
        }
        if (isFinishing) {
            scope.launch(Dispatchers.IO) {
                session?.close()
                scope.cancel()
            }
        }
    }

    private fun Session.setupToProduceFrames() {
        val isSessionStarted = !isSessionRunning
        if (!isSessionRunning) {
            resume()
            isSessionRunning = true
        }
        if (isDeviceRotationChanged) {
            val cameraId = cameraConfig.cameraId
            val cpuImageRotation = cameraId.getCameraRotationCompensation(deviceGeometry.rotation)
            barcodeAnalyser.rotation = cpuImageRotation
            isDeviceRotationChanged = false
        }
        if (isSessionStarted && deviceGeometry.isViewportValid()) {
            setDisplayGeometry(
                deviceGeometry.rotation,
                deviceGeometry.viewportSize.width,
                deviceGeometry.viewportSize.height
            )
        }
    }

    private fun String.getCameraRotationCompensation(deviceRotation: Int): Int {
        val rotation = rotations.get(deviceRotation)
        val cameraManager = activity.getSystemService(CameraManager::class.java)

        val sensorOrientation = cameraManager
            .getCameraCharacteristics(this)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        return (sensorOrientation - rotation + 360) % 360
    }

    private fun initSession() {
        if (isArUnsupportedDevice) return

        arExceptionHandler.runSafe {
            try {
                when (preparationStep) {
                    ArPreparationStep.NOT_VERIFIED -> {
                        val installStatus = ArCoreApk.getInstance().requestInstall(activity, true)
                        if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                            preparationStep = ArPreparationStep.NOT_UP_TO_DATE
                            return@runSafe
                        }
                        preparationStep = ArPreparationStep.INSTALLED_AND_UP_TO_DATE
                        initSession()
                    }
                    ArPreparationStep.NOT_UP_TO_DATE -> {
                        val installStatus = ArCoreApk.getInstance().requestInstall(activity, false)
                        if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                            return@runSafe
                        }
                        preparationStep = ArPreparationStep.INSTALLED_AND_UP_TO_DATE
                        initSession()
                    }
                    ArPreparationStep.INSTALLED_AND_UP_TO_DATE -> {
                        if (session == null) {
                            session = Session(activity).also {
                                val config = Config(it).also {
                                    it.planeFindingMode = Config.PlaneFindingMode.VERTICAL
                                    it.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                                }
                                it.configure(config)
                                it.setupToProduceFrames()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                isArUnsupportedDevice = true
                throw e
            }
        }
    }
}

enum class ArPreparationStep {
    NOT_VERIFIED, NOT_UP_TO_DATE, INSTALLED_AND_UP_TO_DATE
}

class ArServicesNotUpToDate : RuntimeException()