package com.executor.goodsinventory.domain

import android.widget.TextView
import java.util.*

object Timer {
    var start = 0L
    fun startTimer(textView: TextView) {
        val date= Date()
        textView.text="0:0"
        start= date.time
    }
    fun endTimer(textView: TextView){
        val date= Date()
        textView.text="0:0"
        val ms = date.time - start
        val sec = (ms/1000).toInt()
        textView.text="$sec:${ms%1000}"
    }
}