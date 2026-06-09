package com.example.travelrecord_20214009

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class TravelAdapter(
    private var records: List<TravelRecord>,
    private val onItemClick: (TravelRecord) -> Unit,
    private val onRegisterContextMenu: (View, TravelRecord) -> Unit
) : RecyclerView.Adapter<TravelAdapter.TravelViewHolder>() {

    class TravelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel, parent, false)
        return TravelViewHolder(view)
    }

    override fun onBindViewHolder(holder: TravelViewHolder, position: Int) {
        val record = records[position]
        holder.tvTitle.text = record.title
        holder.tvDate.text = record.date
        bindThumbnail(holder.ivThumbnail, record.photoPath)
        holder.itemView.setOnClickListener { onItemClick(record) }
        holder.itemView.tag = record
        onRegisterContextMenu(holder.itemView, record)
    }

    override fun getItemCount(): Int = records.size

    fun updateList(newRecords: List<TravelRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }

    private fun bindThumbnail(imageView: ImageView, photoPath: String) {
        if (photoPath.isNotBlank() && File(photoPath).exists()) {
            val bitmap = BitmapFactory.decodeFile(photoPath)
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.setImageResource(R.drawable.ic_photo_placeholder)
        }
    }
}
