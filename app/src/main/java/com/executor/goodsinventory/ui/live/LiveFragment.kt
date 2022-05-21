package com.executor.goodsinventory.ui.live

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.executor.goodsinventory.data.InventoryModel
import com.executor.goodsinventory.databinding.FragmentLiveBinding
import com.executor.goodsinventory.domain.tflite.Classifier
import com.executor.goodsinventory.domain.tflite.YoloV4Classifier
import com.executor.goodsinventory.domain.utils.Utils
import com.executor.goodsinventory.ui.BaseViewModel
import com.executor.goodsinventory.ui.ReportAdapter
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LiveFragment : Fragment() {
    private lateinit var binding: FragmentLiveBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var adapter: ReportAdapter
    private lateinit var target: Target
    private lateinit var detector: Classifier
    private val viewModel: BaseViewModel by viewModels()
    private lateinit var cameraExecutor: ExecutorService
    private var imageRotation = 0F
    val handler = Handler()
    private val runnable = Runnable {
        if (isAdded) {
            takePhoto()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        adapter = ReportAdapter()
        binding.rvReport.adapter = adapter
        // Set up the listeners for take photo and video capture buttons

        initPreview()

        viewModel.goodsSLE.observe(viewLifecycleOwner, { goods ->
            if (goods.isNotEmpty()) {
                binding.tvReport.text = "Отчет"
                adapter.list = goods
            }
            handler.postDelayed(runnable, 1000)
        })

        initDetector()

        handler.postDelayed(runnable, 1000)
    }

    private fun initDetector() {
        try {
            detector = YoloV4Classifier.create(
                requireActivity().assets,
                InventoryModel.TF_OD_API_MODEL_FILE,
                InventoryModel.TF_OD_API_LABELS_FILE,
                InventoryModel.is_quantized
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun initPreview() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({}, ContextCompat.getMainExecutor(requireContext()))
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireActivity().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    prepareBitmapAndDetect(output.savedUri)
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun prepareBitmapAndDetect(savedUri: Uri?) {
        target = getTarget()
        Picasso.get()
            .load(savedUri)
            .rotate(imageRotation)
            .into(target)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
        setOrientationListener()
    }

    private fun getTarget() = object : Target {
        override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom?) {
            val cropBitmap = Utils.bitmapResized(
                bitmap,
                InventoryModel.TF_OD_API_INPUT_SIZE,
                InventoryModel.TF_OD_API_INPUT_SIZE
            )
            val handler = Handler()
            Thread {
                val results: List<Classifier.Recognition> =
                    detector.recognizeImage(cropBitmap)
                handler.post {
                    viewModel.handleResult(cropBitmap, results)
                }
            }.start()
        }

        override fun onBitmapFailed(e: java.lang.Exception, errorDrawable: Drawable?) {}
        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            }
        }
    }

    private fun setOrientationListener() {
        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                // Monitors orientation values to determine the target rotation value
                imageRotation = if (orientation >= 45 && orientation < 135) {
                    180F
                } else if (orientation >= 135 && orientation < 225) {
                    270F
                } else if (orientation >= 225 && orientation < 315) {
                    0F
                } else {
                    90F
                }


            }
        }
        orientationEventListener.enable()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "LiveBroadcast"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

}