package org.twodee.backlog

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PhotoHolder(view: View) : RecyclerView.ViewHolder(view) {
  val yearLabel: TextView = view.findViewById(R.id.yearLabel)
  val photoView: ImageView = view.findViewById(R.id.photoView)
}