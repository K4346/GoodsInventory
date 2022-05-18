package com.executor.goodsinventory.ui

import android.app.Application
import android.os.Environment
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.lifecycle.AndroidViewModel
import com.executor.goodsinventory.InventoryModel
import com.executor.goodsinventory.MainActivity
import com.executor.goodsinventory.domain.entities.Goods
import com.executor.goodsinventory.domain.tflite.Classifier
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PhotoViewModel(application: Application) : AndroidViewModel(application) {
    private val disposable = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
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
}