package com.executor.goodsinventory.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.Spannable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.executor.goodsinventory.InventoryModel
import com.executor.goodsinventory.UtilsObject
import com.executor.goodsinventory.databinding.FragmentPhotoBinding
import com.executor.goodsinventory.domain.entities.Goods
import com.executor.goodsinventory.domain.env.ImageUtils
import com.executor.goodsinventory.domain.env.Utils
import com.executor.goodsinventory.domain.tflite.Classifier
import com.executor.goodsinventory.domain.tflite.Classifier.Recognition
import com.executor.goodsinventory.domain.tflite.YoloV4Classifier
import java.io.IOException
import java.text.DecimalFormat
import com.squareup.picasso.Picasso
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import org.antlr.runtime.tree.TreeParser.inContext

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.executor.goodsinventory.MainActivity
import java.io.ByteArrayOutputStream
import java.io.File


class PhotoFragment : Fragment() {
    var currentPhotoPath = ""
    private lateinit var binding: FragmentPhotoBinding
    private val viewModel: PhotoViewModel by viewModels()
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

        initListenners()

        sourceBitmap = Utils.getBitmapFromAsset(requireContext(), "primer.jpg")

        cropBitmap = Utils.BITMAP_RESIZER(sourceBitmap, InventoryModel.TF_OD_API_INPUT_SIZE,InventoryModel.TF_OD_API_INPUT_SIZE)

        binding.imageView.setImageBitmap(cropBitmap)

        initBox()
    }

    private fun initListenners() {
        binding.detect.setOnClickListener {
            val handler = Handler()
            val drawable = binding.imageView.drawable.toBitmap()
            cropBitmap = Utils.BITMAP_RESIZER(
                drawable,
                InventoryModel.TF_OD_API_INPUT_SIZE,
                InventoryModel.TF_OD_API_INPUT_SIZE
            )
            Thread {
                val results: List<Recognition> =
                    detector!!.recognizeImage(cropBitmap)
                handler.post {
                    handleResult(cropBitmap, results)
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

    private fun dispatchTakesPictureIntent() {
        val requestImageCapture = InventoryModel.CAMERA_REQUEST
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, requestImageCapture)
        } catch (e: ActivityNotFoundException) {
        }


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
        }
        else if (resultCode == Activity.RESULT_OK && requestCode == InventoryModel.CAMERA_REQUEST) {
            val   file = File(currentPhotoPath)
            Picasso.get()
                .load(file)
                .fit()
                .into(binding.imageView)

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
        previewHeight = InventoryModel.TF_OD_API_INPUT_SIZE
        previewWidth = InventoryModel.TF_OD_API_INPUT_SIZE
        frameToCropTransform = ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            InventoryModel.TF_OD_API_INPUT_SIZE, InventoryModel.TF_OD_API_INPUT_SIZE,
            sensorOrientation, MAINTAIN_ASPECT
        )
        cropToFrameTransform = Matrix()
        frameToCropTransform!!.invert(cropToFrameTransform)
//        tracker = MultiBoxTracker(requireContext())
//        binding.trackingOverlay.addCallback { canvas -> tracker.draw(canvas) }
//        tracker.setFrameConfiguration(
//            InventoryModel.TF_OD_API_INPUT_SIZE,
//            InventoryModel.TF_OD_API_INPUT_SIZE,
//            sensorOrientation
//        )
        try {
            detector = YoloV4Classifier.create(
                requireActivity().assets,
                InventoryModel.TF_OD_API_MODEL_FILE,
                InventoryModel.TF_OD_API_LABELS_FILE,
                InventoryModel.TF_OD_API_IS_QUANTIZED
            )
        } catch (e: IOException) {
            e.printStackTrace()
            InventoryModel.LOGGER.e(e, "Exception initializing classifier!")
            val toast = Toast.makeText(
                requireContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT
            )
            toast.show()
        }
    }

    private fun handleResult(bitmap: Bitmap, results: List<Recognition>) {
        binding.labels.text=""
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val colors:ArrayList<Int> = arrayListOf()
        while (colors.size!=InventoryModel.classes){
            colors.add(UtilsObject.getRandomColor())
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        paint.isAntiAlias = true

        var goods = ArrayList<Goods>()
        results.forEach { result ->
            val location = result.location
            if (location != null && result.confidence >= InventoryModel.MINIMUM_CONFIDENCE_TF_OD_API) {
                paint.color = colors[result.detectedClass]
               goods = viewModel.prepareGoods(colors, result, goods)

//                if (!binding.labels.text.contains(result.title)){

//                    val span = SpannableString(result.title+"\n")
//                    span.setSpan(ForegroundColorSpan(colors[result.detectedClass]), 0, result.title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    binding.labels.text = span
//                val borderedText = BorderedText( TypedValue.applyDimension(
//                    TypedValue.COMPLEX_UNIT_DIP,
//                    11f,
//                    resources.displayMetrics
//                ))
//                borderedText.drawText(canvas, location.left,
//                    location.top - 5,
//                    "${result.title} ${DecimalFormat("##.##").format(result.confidence)}")
                canvas.drawText(
                    DecimalFormat("##.##").format(result.confidence),
                    location.left,
                        location.top - 5,
                        paint
                    )
                    canvas.drawRect(location, paint)
                }
        }
        goods.forEach {
            binding.labels.text =  "${binding.labels.text}\n${it.name} - ${it.count}"
        }
        binding.imageView.setImageBitmap(bitmap)
    }
}