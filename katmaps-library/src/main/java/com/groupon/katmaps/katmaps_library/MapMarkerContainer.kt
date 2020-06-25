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

package com.groupon.katmaps.katmaps_library

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.groupon.katmaps.katmaps_library.model.MapIcon
import com.groupon.katmaps.katmaps_library.model.MapMarker
import com.groupon.katmaps.katmaps_library.model.MarkerViewState
import com.groupon.katmaps.katmaps_library.model.getSize
import com.groupon.katmaps.katmaps_library.model.latLng
import com.groupon.katmaps.katmaps_library.util.pxToDp
import com.groupon.katmaps.katmaps_library.util.toBitmap
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import kotlinx.android.synthetic.main.map_marker_label.view.*

internal class MapMarkerContainer(
    context: Context,
    googleMap: GoogleMap,
    val marker: MapMarker
) {
    companion object {
        internal const val MARKER_LABEL_HORIZONTAL_ANCHOR = 0.5f
        internal const val MARKER_LABEL_VERTICAL_ANCHOR = 0.0f
    }

    val mapPin: Marker
    val mapLabel: Marker?
    val labelBitmap: Bitmap?

    var viewState: MarkerViewState = MarkerViewState.PIN_AND_LABEL
        set(value) = value.takeIf { it != field }.let {
            val shouldAnimate = field == MarkerViewState.EXPANDED_WITH_LABEL || value == MarkerViewState.EXPANDED_WITH_LABEL
            if (shouldAnimate) {
                animateExpansion(value)
            }

            mapPin.zIndex = value.mapZIndex
            mapLabel?.zIndex = value.mapZIndex
            mapLabel?.isVisible = value != MarkerViewState.PIN_ONLY
            field = value
        }

    private var animationTimer: Timer? = null

    init {
        labelBitmap = generateMapLabelBitmap(context, marker)
        mapPin = createMapMarker(googleMap, marker)
        mapLabel = createMapLabel(googleMap, marker, labelBitmap)
    }

    fun remove() {
        animationTimer?.cancel()
        mapPin.remove()
        mapLabel?.remove()
    }

    fun getPinBounds(googleMap: GoogleMap, resources: Resources): Rect {
        val pinSize = marker.icon.getSize(viewState == MarkerViewState.EXPANDED_WITH_LABEL)
        val pinAnchorCoordinate = googleMap.projection.toScreenLocation(marker.position.latLng)

        val labelLeft = (pinAnchorCoordinate.x - pinSize.width() / 2).pxToDp(resources)
        val labelRight = (pinAnchorCoordinate.x + pinSize.width() / 2).pxToDp(resources)
        val labelTop = (pinAnchorCoordinate.y - pinSize.height()).pxToDp(resources)
        val labelBottom = pinAnchorCoordinate.y.pxToDp(resources)

        return Rect(labelLeft, labelTop, labelRight, labelBottom)
    }

    fun getLabelBounds(googleMap: GoogleMap, resources: Resources): Rect? {
        val labelAnchorCoordinate = googleMap.projection.toScreenLocation(marker.position.latLng)

        return labelBitmap?.run {
            val labelLeft = (labelAnchorCoordinate.x - labelBitmap.width / 2).pxToDp(resources)
            val labelRight = (labelAnchorCoordinate.x + labelBitmap.width / 2).pxToDp(resources)
            val labelTop = labelAnchorCoordinate.y.pxToDp(resources)
            val labelBottom = (labelAnchorCoordinate.y + labelBitmap.height).pxToDp(resources)
            return Rect(labelLeft, labelTop, labelRight, labelBottom)
        }
    }

    private fun animateExpansion(viewState: MarkerViewState) {
        if (marker.icon !is MapIcon.AnimatedImage) {
            return
        }

        val interval = TimeUnit.SECONDS.toMillis(1) / MapIcon.AnimatedImage.FRAME_RATE
        val frames = marker.icon.images.map { BitmapDescriptorFactory.fromBitmap(it) }
        val numFrames = marker.icon.images.size

        animationTimer?.cancel()

        var step = 0
        animationTimer = timer(period = interval) {
            val frameNumber = if (viewState == MarkerViewState.EXPANDED_WITH_LABEL) step else numFrames - step - 1
            Handler(Looper.getMainLooper()).post { mapPin.setIcon(frames[frameNumber]) }
            if (++step >= numFrames) cancel()
        }
    }

    private fun createMapMarker(googleMap: GoogleMap, marker: MapMarker): Marker {
        val iconBitmap = when (val asset = marker.icon) {
            is MapIcon.Image -> BitmapDescriptorFactory.fromBitmap(asset.image)
            is MapIcon.AnimatedImage -> BitmapDescriptorFactory.fromBitmap(asset.images.first())
        }

        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(marker.position.latLng)
                .icon(iconBitmap)
        )
        marker.tag = marker.tag
        return marker
    }

    private fun createMapLabel(googleMap: GoogleMap, marker: MapMarker, labelBitmap: Bitmap): Marker? {
        if (marker.labelTitle.isEmpty() && marker.labelDescription.isEmpty()) return null

        val label = googleMap.addMarker(
            MarkerOptions()
                .position(marker.position.latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(labelBitmap))
                .anchor(MARKER_LABEL_HORIZONTAL_ANCHOR, MARKER_LABEL_VERTICAL_ANCHOR)
        )
        label.tag = marker.tag
        return label
    }

    @SuppressLint("InflateParams")
    private fun generateMapLabelBitmap(context: Context, katmapsMarker: MapMarker): Bitmap {
        return LayoutInflater.from(context).inflate(R.layout.map_marker_label, null).apply {
            markerLabelTitle.text = katmapsMarker.labelTitle
            markerLabelDescription.text = katmapsMarker.labelDescription
        }.toBitmap()
    }
}
