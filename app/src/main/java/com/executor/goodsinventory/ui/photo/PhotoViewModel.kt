package com.executor.goodsinventory.ui.photo

import android.app.Application
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import com.executor.goodsinventory.MainActivity
import com.executor.goodsinventory.ui.BaseViewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class PhotoViewModel(application: Application) : BaseViewModel(application) {

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

    fun setOrientationListener(path: String): Float {
        val ei = ExifInterface(path)
        val orientation: Int =
            ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> {
                0f
            }
        }
    }

}