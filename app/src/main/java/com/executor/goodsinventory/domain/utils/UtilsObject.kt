package com.executor.goodsinventory.domain.utils

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Color
import android.graphics.RectF
import com.executor.goodsinventory.data.InventoryModel
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

object UtilsObject {

    fun getRandomColor(): Int {
        val rnd = Random()
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
    }

    @Throws(IOException::class)
    fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun isFolder(context: Context, title: String): Boolean {
        if (title.contains('.')) return false
        val list = context.assets.list(title)?.toList() as List<String>
        val flag1 = list.any { it.endsWith(".tflite") }
        val flag2 = list.any { it.endsWith(".txt") }

        return flag1 && flag2
    }

    fun initModels(context: Context) {
        var list = context.assets.list("")?.toList() as List<String>
        list = list.filter { isFolder(context, it) } as ArrayList<String>
        var i = 0
        list.forEachIndexed { index, s ->
            if (s.contains("tiny")) {
                i = index
                return@forEachIndexed
            }
        }
        if (i!=0) {
            Collections.swap(list, 0, i)
        }
        InventoryModel.models = list

        setCurrentModel(context, 0)
    }

    fun setCurrentModel(context: Context, i: Int) {
        val model = InventoryModel.models[i]
        val folder = (context.assets.list(model)?.toList() as ArrayList<String>)
        val txt = folder.filter { it.endsWith(".txt") }
        val tflite = folder.filter { it.endsWith(".tflite") }

        InventoryModel.TF_OD_API_MODEL_FILE = "$model/${tflite[0]}"
        InventoryModel.TF_OD_API_LABELS_FILE =
            "file:///android_asset/${model}/${txt[0]}"

        InventoryModel.isTiny = tflite[0].contains("tiny")
        InventoryModel.is_quantized = tflite[0].contains("fp16") || tflite[0].contains("ip8")
    }

    fun RectF.scale(factorH: Float, factorV: Float) {
        val oldWidth = width()
        val oldHeight = height()
        val rectCenterX = left + oldWidth / 2F
        val rectCenterY = top + oldHeight / 2F
        val newWidth = oldWidth * factorH
        val newHeight = oldHeight * factorV
        left = rectCenterX - newWidth / 2F
        right = rectCenterX + newWidth / 2F
        top = rectCenterY - newHeight / 2F
        bottom = rectCenterY + newHeight / 2F
    }

}