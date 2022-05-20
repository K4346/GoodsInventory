package com.executor.goodsinventory.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import com.executor.goodsinventory.InventoryModel
import com.executor.goodsinventory.MainActivity
import com.executor.goodsinventory.SingleLiveEvent
import com.executor.goodsinventory.UtilsObject
import com.executor.goodsinventory.domain.entities.Goods
import com.executor.goodsinventory.domain.env.BorderedText
import com.executor.goodsinventory.domain.tflite.Classifier
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class ViewModel(application: Application) : AndroidViewModel(application) {
    private val disposable = CompositeDisposable()
    val goodsSLE = SingleLiveEvent<List<Goods>>()
    val analyzedImageSLE = SingleLiveEvent<Bitmap>()

    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    fun createImageFile(activity: MainActivity): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    fun prepareGoods(
        colors: ArrayList<Int>,
        result: Classifier.Recognition,
        goods: ArrayList<Goods>
    ): ArrayList<Goods> {
        if (goods.filter { result.detectedClass == it.code }.isEmpty()) {
            goods.add(Goods(result.title, result.detectedClass, colors[result.detectedClass], 0))
        }
        goods.map {
            if (result.detectedClass == it.code)
                it.count++
        }
        return goods
    }

    fun handleResult(bitmap: Bitmap, results: List<Classifier.Recognition>) {
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val colors: java.util.ArrayList<Int> = arrayListOf()
        while (colors.size != InventoryModel.classes) {
            colors.add(UtilsObject.getRandomColor())
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        paint.isAntiAlias = true

        var goods = java.util.ArrayList<Goods>()

        results.forEach { result ->
            val location = result.location
            if (location != null && result.confidence >= InventoryModel.accuracy) {
                paint.color = colors[result.detectedClass]
                goods = prepareGoods(colors, result, goods)

                val b = BorderedText(
                    interiorColor = paint.color,
                    exteriorColor = Color.BLACK,
                    textSize = 14f
                )
                b.drawText(
                    canvas,
                    location.left,
                    location.top - 5, DecimalFormat("##.##").format(result.confidence)
                )

                canvas.drawRect(location, paint)
            }
        }
        goodsSLE.value = goods
        analyzedImageSLE.value = (bitmap)
    }


    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }

}