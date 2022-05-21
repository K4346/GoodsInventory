package com.executor.goodsinventory.domain.utils

import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import java.util.*

object Timer {
    var start = 0L
    fun startTimer(textView: TextView, progressBar: ProgressBar) {
        progressBar.isVisible=true
        val date= Date()
        textView.text="0:0"
        start = date.time
    }
    fun endTimer(textView: TextView, progressBar: ProgressBar){
        progressBar.isVisible=false
        val date= Date()
        textView.text="0:0"
        val ms = date.time - start
        val sec = (ms/1000).toInt()
        textView.text="$sec:${ms%1000}"
    }
}