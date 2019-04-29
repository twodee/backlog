package org.twodee.backlog

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class PhotoAdapter(private val photos: List<Photo>) : RecyclerView.Adapter<PhotoHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.photo, parent, false)
    return PhotoHolder(view)
  }

  override fun getItemCount(): Int {
    return photos.size
  }

  override fun onBindViewHolder(holder: PhotoHolder, i: Int) {
    holder.yearLabel.text = photos[i].year.toString()
    val bitmap = BitmapFactory.decodeFile(photos[i].file.absolutePath)
    holder.photoView.setImageBitmap(bitmap)
  }
}