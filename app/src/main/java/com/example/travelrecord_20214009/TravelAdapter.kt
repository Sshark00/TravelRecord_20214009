package com.example.travelrecord_20214009

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TravelAdapter(
    private var records: List<TravelRecord>,
    private val coroutineScope: CoroutineScope,
    private val onItemClick: (TravelRecord) -> Unit,
    private val onRegisterContextMenu: (View, TravelRecord) -> Unit
) : RecyclerView.Adapter<TravelAdapter.TravelViewHolder>() {

    private val loadingJobs = mutableMapOf<TravelViewHolder, Job>()

    class TravelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        val progressThumbnail: ProgressBar = itemView.findViewById(R.id.progress_thumbnail)
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
        bindThumbnail(holder, record.photoPath)
        holder.itemView.setOnClickListener { onItemClick(record) }
        holder.itemView.tag = record
        onRegisterContextMenu(holder.itemView, record)
    }

    override fun onViewRecycled(holder: TravelViewHolder) {
        loadingJobs.remove(holder)?.cancel()
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = records.size

    fun updateList(newRecords: List<TravelRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }

    private fun bindThumbnail(holder: TravelViewHolder, photoPath: String) {
        loadingJobs.remove(holder)?.cancel()
        holder.ivThumbnail.setImageResource(R.drawable.ic_photo_placeholder)
        holder.ivThumbnail.tag = photoPath

        if (photoPath.isBlank() || !File(photoPath).exists()) {
            holder.progressThumbnail.visibility = View.GONE
            return
        }

        holder.progressThumbnail.visibility = View.VISIBLE
        loadingJobs[holder] = coroutineScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeSampledBitmap(photoPath)
            }

            if (holder.ivThumbnail.tag != photoPath) {
                return@launch
            }

            holder.progressThumbnail.visibility = View.GONE
            if (bitmap != null) {
                holder.ivThumbnail.setImageBitmap(bitmap)
            } else {
                holder.ivThumbnail.setImageResource(R.drawable.ic_photo_placeholder)
            }
        }
    }

    private fun decodeSampledBitmap(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (_: Exception) {
            null
        }
    }
}
