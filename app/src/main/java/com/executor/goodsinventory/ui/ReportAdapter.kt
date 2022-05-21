package com.executor.goodsinventory.ui

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.executor.goodsinventory.databinding.ReportItemBinding
import com.executor.goodsinventory.domain.entities.Goods


class ReportAdapter : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {
    var list: List<Goods> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ReportItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)

        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val item = list[position]
        holder.label.text = item.name
        holder.label.setTextColor(item.color)
        holder.count.text = item.count.toString()
    }

    override fun getItemCount() = list.size

    inner class ReportViewHolder(binding: ReportItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val label = binding.label
        val count = binding.count
    }
}