package com.executor.goodsinventory.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.lifecycle.AndroidViewModel
import com.executor.goodsinventory.data.InventoryModel
import com.executor.goodsinventory.domain.entities.Goods
import com.executor.goodsinventory.domain.tflite.Classifier
import com.executor.goodsinventory.domain.utils.BorderedText
import com.executor.goodsinventory.domain.utils.SingleLiveEvent
import com.executor.goodsinventory.domain.utils.UtilsObject
import com.executor.goodsinventory.domain.utils.UtilsObject.scale
import io.reactivex.disposables.CompositeDisposable
import java.text.DecimalFormat
import java.util.*

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    private val disposable = CompositeDisposable()

    val goodsSLE = SingleLiveEvent<List<Goods>>()

    val analyzedImageSLE = SingleLiveEvent<Bitmap>()

    fun handleResult(bitmap: Bitmap, results: List<Classifier.Recognition>) {
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val colors: ArrayList<Int> = arrayListOf()
        while (colors.size != InventoryModel.classes) {
            colors.add(UtilsObject.getRandomColor())
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        paint.isAntiAlias = true

        var goods = ArrayList<Goods>()

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
                if (InventoryModel.isTiny) {
                    location.scale(0.5f, 0.75f)
                }
                canvas.drawRect(location, paint)
            }
        }
        goodsSLE.value = goods
        analyzedImageSLE.value = (bitmap)
    }

    private fun prepareGoods(
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

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }

}