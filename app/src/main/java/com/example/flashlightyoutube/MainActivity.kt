package com.example.flashlightyoutube

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var permissionGranted = false

    private var flashlightImage: ImageView? = null
    private var lightImage: ImageView? = null
    private var frequencySeekBar: SeekBar? = null
    private var frequencyLayout: View? = null
    private var frequencyCountText: TextView? = null
    private var modes: View? = null
    private var strobeText: TextView? = null
    private var sosText: TextView? = null
    private var cancel: ImageView? = null

    private var isFlashlightOn = false
    private var isStrobeOn = false
    private var isSosOn = false

    private val strobeHandler = Handler()
    private var strobeFrequency: Long = 1000

    private val sosHandler = Handler()
    private var pattern = arrayOf(100L, 100L, 100L, 100L, 100L, 100L, 300L, 100L, 300L, 100, 300L, 700L)
    private var currentIndex = 0

    companion object {
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            cameraId = cameraManager.cameraIdList.firstOrNull {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                val capabilities = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                capabilities ?: false
            }
        } catch(e: CameraAccessException) {
            e.printStackTrace()
        }

        flashlightImage = findViewById(R.id.flashlight)
        lightImage = findViewById(R.id.light)
        frequencySeekBar = findViewById(R.id.frequency_seek_bar)
        frequencyLayout = findViewById(R.id.frequency_layout)
        frequencyCountText = findViewById(R.id.frequency_count)
        modes = findViewById(R.id.modes)
        strobeText = findViewById(R.id.stroboscopic_mode)
        sosText = findViewById(R.id.sos_signal)
        cancel = findViewById(R.id.cancel)

        lightImage?.setColorFilter(ContextCompat.getColor(this, R.color.white))

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true
            setListeners()
        } else {
            request()
        }
    }

    private fun setListeners() {
        flashlightImage?.setOnClickListener {
            toggleFlashlight(!isFlashlightOn)
        }
        cancel?.setOnClickListener {
            handleCancel()
        }
        strobeText?.setOnClickListener {
            handleStrobe()
        }
        sosText?.setOnClickListener {
            handleSOS()
        }

        frequencySeekBar?.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                strobeFrequency = 1100 - progress.toLong() * 11
                frequencyCountText?.text = strobeFrequency.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                //Not implemented
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                //Not implemented
            }

        })
    }

    private fun setRequestListeners() {
        flashlightImage?.setOnClickListener {
            request()
        }
        strobeText?.setOnClickListener {
            request()
        }
        sosText?.setOnClickListener {
            request()
        }
    }

    private fun handleCancel() {
        resetFlashlight()
        isStrobeOn = false
        isSosOn = false
        strobeFrequency = 1000
        frequencyLayout?.visibility = View.GONE
        cancel?.visibility = View.GONE
        modes?.visibility = View.VISIBLE
    }

    private fun handleStrobe() {
        modes?.visibility = View.GONE
        frequencyLayout?.visibility = View.VISIBLE
        cancel?.visibility = View.VISIBLE
        isStrobeOn = true
        toggleFlashlight(false)
        strobeHandler.post(strobeRunnable)
    }

    private fun handleSOS() {
        isFlashlightOn = false
        isSosOn = true
        modes?.visibility = View.GONE
        cancel?.visibility = View.VISIBLE
        toggleFlashlight(false)
        sosHandler.post(sosRunnable)
    }

    private val strobeRunnable = object: Runnable {
        override fun run() {
            if(isStrobeOn) {
                toggleFlashlight(!isFlashlightOn)
            }
            strobeHandler.postDelayed(this, strobeFrequency)
        }
    }

    private val sosRunnable = object: Runnable {
        override fun run() {
            if(isSosOn) {
                toggleFlashlight(!isFlashlightOn)
                val nextDelay = pattern.getOrElse(currentIndex) {500L}
                sosHandler.postDelayed(this, nextDelay)
                currentIndex = (currentIndex + 1) % pattern.size
            }
        }
    }

    private fun toggleFlashlight(active: Boolean) {
        cameraId?.let {
            id ->
            if(active) {
                lightImage?.visibility = View.VISIBLE
            } else {
                lightImage?.visibility = View.GONE
            }
            isFlashlightOn = active
            try {
                cameraManager.setTorchMode(id, active)
            } catch(e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    private fun resetFlashlight() {
        if(isStrobeOn) {
            strobeHandler.removeCallbacks(strobeRunnable)
            frequencySeekBar?.progress = 0
        }
        if(isSosOn) {
            sosHandler.removeCallbacks(sosRunnable)
        }
        toggleFlashlight(false)
    }

    private fun request() {
        requestPermissions(arrayOf(android.Manifest.permission.CAMERA), MY_PERMISSIONS_REQUEST_CAMERA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    permissionGranted = true
                    setListeners()
                } else {
                    setRequestListeners()
                }
                return
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resetFlashlight()
    }
}