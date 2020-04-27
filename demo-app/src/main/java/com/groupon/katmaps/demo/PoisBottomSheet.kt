/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package com.groupon.katmaps.demo

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.groupon.katmaps.katmaps_library.model.KatMapsMarker
import kotlinx.android.synthetic.main.pois_list_bottom_sheet.view.*

class PoisBottomSheet(context: Context, attributeSet: AttributeSet?) :
    FrameLayout(context, attributeSet) {

    private val itemClickListenerInternal: ((KatMapsMarker) -> Unit) = { itemClickListener?.invoke(it) }
    private var poiListAdapter: PoiListAdapter = PoiListAdapter(itemClickListenerInternal)
    var itemClickListener: ((KatMapsMarker) -> Unit)? = null

    init {
        inflate(context, R.layout.pois_list_bottom_sheet, this)
        poisList.run {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(
                DividerItemDecoration(context, VERTICAL).apply {
                    context.getDrawable(R.drawable.poi_card_spacer)?.let { setDrawable(it) }
                }
            )
            adapter = poiListAdapter
        }

        poiListAdapter.items = MapDataSource.markers
    }
}
