package com.groupon.katmaps.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.groupon.katmaps.katmaps_library.model.MapMarker
import kotlinx.android.synthetic.main.poi_card.view.*

class PoiListAdapter(private val clickListener: ((MapMarker) -> Unit)) : RecyclerView.Adapter<PoiListAdapter.VH>() {
    var items: List<MapMarker> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.poi_card, parent, false), clickListener)

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(view: View, private val clickListener: ((MapMarker) -> Unit)) : RecyclerView.ViewHolder(view) {
        fun bind(marker: MapMarker) {
            itemView.poiName.text = marker.labelTitle
            itemView.poiDescription.text = marker.labelDescription
            itemView.setOnClickListener { clickListener(marker) }
        }
    }
}
