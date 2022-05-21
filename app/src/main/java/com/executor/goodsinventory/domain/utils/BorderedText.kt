package com.executor.goodsinventory.domain.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class BorderedText(val interiorColor: Int = Color.WHITE, val exteriorColor: Int = Color.BLACK, val textSize: Float = 14f) {

    private var interiorPaint: Paint
    private var exteriorPaint: Paint

    init {
        interiorPaint = Paint()
        interiorPaint.textSize = textSize
        interiorPaint.color = interiorColor
        interiorPaint.style = Paint.Style.FILL
        interiorPaint.isAntiAlias = true
        interiorPaint.alpha = 255
        exteriorPaint = Paint()
        exteriorPaint.textSize = textSize
        exteriorPaint.color = exteriorColor
        exteriorPaint.style = Paint.Style.FILL_AND_STROKE
        exteriorPaint.strokeWidth = textSize / 8
        exteriorPaint.isAntiAlias = false
        exteriorPaint.alpha = 255
    }

    fun drawText(canvas: Canvas, posX: Float, posY: Float, text: String?) {
        canvas.drawText(text!!, posX, posY, exteriorPaint)
        canvas.drawText(text, posX, posY, interiorPaint)
    }

    fun drawText(
        canvas: Canvas, posX: Float, posY: Float, text: String?, color: Int
    ) {
        val width = exteriorPaint.measureText(text)
        val textSize = exteriorPaint.textSize
        val paint = Paint(color)
        paint.style = Paint.Style.FILL
        paint.alpha = 160
        canvas.drawRect(posX, posY + textSize.toInt(), posX + width.toInt(), posY, paint)
        canvas.drawText(text!!, posX, posY + textSize, interiorPaint)
    }

}