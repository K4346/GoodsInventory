package com.executor.goodsinventory

import android.graphics.Color
import java.util.*
import kotlin.collections.ArrayList

object UtilsObject {

    fun getRandomColor(): Int {
        val rnd = Random()
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
    }
}