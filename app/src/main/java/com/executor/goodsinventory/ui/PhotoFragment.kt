package com.executor.goodsinventory.ui

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.executor.goodsinventory.InventoryModel
import com.executor.goodsinventory.MainActivity
import com.executor.goodsinventory.databinding.FragmentPhotoBinding
import com.executor.goodsinventory.domain.Timer
import com.executor.goodsinventory.domain.env.ImageUtils
import com.executor.goodsinventory.domain.env.Utils
import com.executor.goodsinventory.domain.tflite.Classifier
import com.executor.goodsinventory.domain.tflite.Classifier.Recognition
import com.executor.goodsinventory.domain.tflite.YoloV4Classifier
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException


class PhotoFragment : Fragment() {
    var currentImageIsAnalysis = false
    var detectionIsStarted = false
    private lateinit var binding: FragmentPhotoBinding
    private val viewModel: ViewModel by viewModels()

    lateinit var adapter: ReportAdapter

    var currentPhotoPath = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
        setAdapter()

        viewModel.goodsSLE.observe(viewLifecycleOwner, { goods ->
            if (goods.isEmpty()) return@observe
            binding.tvReport.text = "Отчет"
            adapter.list = goods
        })
        viewModel.analyzedImageSLE.observe(viewLifecycleOwner, { bitmap ->
            binding.imageView.setImageBitmap(bitmap)
            currentImageIsAnalysis= true
            detectionIsStarted = false
        })

        sourceBitmap = Utils.getBitmapFromAsset(requireContext(), "primer.jpg")

        cropBitmap = Utils.BITMAP_RESIZER(
            sourceBitmap,
            InventoryModel.TF_OD_API_INPUT_SIZE,
            InventoryModel.TF_OD_API_INPUT_SIZE
        )
        sourceBitmap = cropBitmap
        binding.imageView.setImageBitmap(sourceBitmap)

        initBox()
    }

    private fun setAdapter() {
        adapter = ReportAdapter()
        binding.rvReport.adapter = adapter
    }

    private fun refresh() {
        binding.tvReport.text = ""
        binding.imageView.setImageBitmap(sourceBitmap)
        adapter.list = emptyList()
    }

    private fun initListeners() {
        binding.detect.setOnClickListener {
            if (detectionIsStarted) return@setOnClickListener
            detectionIsStarted = true
            val handler = Handler()
            if (currentImageIsAnalysis) {
                refresh()
            } else {
                sourceBitmap = binding.imageView.drawable.toBitmap()
            }
            val drawable = sourceBitmap
            cropBitmap = Utils.BITMAP_RESIZER(
                drawable,
                InventoryModel.TF_OD_API_INPUT_SIZE,
                InventoryModel.TF_OD_API_INPUT_SIZE
            )
            Timer.startTimer(binding.chronometer, binding.progressBar)
            Thread {
                val results: List<Recognition> =
                    detector!!.recognizeImage(cropBitmap)
                handler.post {
                    Timer.endTimer(binding.chronometer, binding.progressBar)
                    viewModel.handleResult(cropBitmap, results)
                }
            }.start()

        }
        binding.galleryButton.setOnClickListener {
            openGalleryForImage()
        }
        binding.cameraButton.setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        val requestCode = 100
        startActivityForResult(intent, requestCode)
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    viewModel.createImageFile(requireActivity() as MainActivity)
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                currentPhotoPath = photoFile?.absolutePath ?: ""
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, InventoryModel.CAMERA_REQUEST)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == InventoryModel.GALLERY_REQUEST) {
            val photoUri = data?.data
            Picasso.get()
                .load(photoUri)
                .fit()
                .into(binding.imageView)
            currentImageIsAnalysis=false
        } else if (resultCode == Activity.RESULT_OK && requestCode == InventoryModel.CAMERA_REQUEST) {
            val file = File(currentPhotoPath)
            Picasso.get()
                .load(file)
                .fit()
                .into(binding.imageView)
            binding.imageView.drawable
            currentImageIsAnalysis=false
        }
    }

    private val MAINTAIN_ASPECT = false
    private val sensorOrientation = 90

    private var detector: Classifier? = null

    private var frameToCropTransform: Matrix? = null
    private var cropToFrameTransform: Matrix? = null
//    private lateinit var tracker: MultiBoxTracker

    protected var previewWidth = 0
    protected var previewHeight = 0

    private var sourceBitmap: Bitmap? = null
    private lateinit var cropBitmap: Bitmap

    private fun initBox() {
        //TODO убрать?
        previewHeight = InventoryModel.TF_OD_API_INPUT_SIZE
        previewWidth = InventoryModel.TF_OD_API_INPUT_SIZE
        frameToCropTransform = ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            InventoryModel.TF_OD_API_INPUT_SIZE, InventoryModel.TF_OD_API_INPUT_SIZE,
            sensorOrientation, MAINTAIN_ASPECT
        )
        cropToFrameTransform = Matrix()
        frameToCropTransform!!.invert(cropToFrameTransform)
        try {
            detector = YoloV4Classifier.create(
                requireActivity().assets,
                InventoryModel.TF_OD_API_MODEL_FILE,
                InventoryModel.TF_OD_API_LABELS_FILE,
                InventoryModel.is_quantized
            )
        } catch (e: IOException) {
            e.printStackTrace()
            val toast = Toast.makeText(
                requireContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT
            )
            toast.show()
        }
    }

}