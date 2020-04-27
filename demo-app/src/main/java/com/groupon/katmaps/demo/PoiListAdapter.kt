package com.groupon.katmaps.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.groupon.katmaps.katmaps_library.model.KatMapsMarker
import kotlinx.android.synthetic.main.poi_card.view.*

class PoiListAdapter(private val clickListener: ((KatMapsMarker) -> Unit)) : RecyclerView.Adapter<PoiListAdapter.VH>() {
    var items: List<KatMapsMarker> = emptyList()
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

    class VH(view: View, private val clickListener: ((KatMapsMarker) -> Unit)): RecyclerView.ViewHolder(view) {
        fun bind(katmapsMarker: KatMapsMarker) {
            itemView.poiName.text = katmapsMarker.labelTitle
            itemView.poiDescription.text = katmapsMarker.labelDescription
            itemView.setOnClickListener { clickListener(katmapsMarker) }
        }
    }
}
