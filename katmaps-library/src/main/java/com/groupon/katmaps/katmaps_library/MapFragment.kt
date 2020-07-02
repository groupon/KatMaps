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

import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.MapStyleOptions
import com.groupon.katmaps.katmaps_library.model.GeoCoordinate
import com.groupon.katmaps.katmaps_library.model.MapBounds
import com.groupon.katmaps.katmaps_library.model.MapMarker
import com.groupon.katmaps.katmaps_library.model.MarkerViewState
import com.groupon.katmaps.katmaps_library.model.MovementReason
import com.groupon.katmaps.katmaps_library.model.MovementState
import com.groupon.katmaps.katmaps_library.model.geoCoordinate
import com.groupon.katmaps.katmaps_library.model.latLng
import com.groupon.katmaps.katmaps_library.util.MapLabelOverlapHider
import com.groupon.katmaps.katmaps_library.util.MapObjectSelectionHelper
import com.groupon.katmaps.katmaps_library.util.pxToDp
import com.groupon.katmaps.katmaps_library.views.InternalMapView

/**
 * KatMaps Map Fragment
 */
class MapFragment : Fragment() {
    companion object {
        private const val LABEL_HIDE_THROTTLING_MS = 100
    }

    private var googleMap: GoogleMap? = null
    private lateinit var googleMapView: MapView
    private var markersMap = HashMap<Any, MapMarkerContainer>()
    private var deferredMarkers = emptyList<MapMarker>()
    private var deferredSelectedTag: Any? = null
    private var selectedMarker: MapMarkerContainer? = null
    private var deferredCameraPosition: MapBounds? = null
    private var movementReason: MovementReason = MovementReason.UNKNOWN

    private var lastLabelHideTime = 0L
    private var lastLabelHideZoom = 0f

    var showCurrentLocation = false
        set(value) {
            field = value
            googleMap?.apply {
                isMyLocationEnabled = value
                uiSettings.isMyLocationButtonEnabled = !value
            }
        }

    var deselectMarkerOnMapClick = false

    var onMapClickListener: ((GeoCoordinate) -> Unit)? = null
    var onMapLongClickListener: ((GeoCoordinate) -> Unit)? = null
    var onMarkerClickListener: ((MapMarker) -> Unit)? = null
    var onMarkerDeselectedListener: ((MapMarker) -> Unit)? = null
    var onMapCameraMoveListener: ((MovementState, MovementReason) -> Unit)? = null

    var cameraPosition: MapBounds?
        get() {
            val googleMap = googleMap
            return if (googleMap == null) {
                deferredCameraPosition
            } else {
                val view = view ?: return null
                val mapWidthDp = view.width.pxToDp(resources).toDouble()
                val mapHeightDp = view.height.pxToDp(resources).toDouble()
                return MapBounds.fromCameraPosition(googleMap.cameraPosition, mapWidthDp, mapHeightDp)
            }
        }
        set(value) {
            if (value != null) {
                moveCamera(value, false)
            }
        }

    var mapStyle: Int? = null
        set(value) {
            googleMap?.run {
                if (value != null) {
                    this.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, value))
                } else {
                    this.setMapStyle(null)
                }
            }
            field = value
        }

    var markers: List<MapMarker>
        get() = markersMap.values.map { it.marker }
        set(value) {
            googleMap.run {
                if (this == null) {
                    deferredMarkers = value
                } else {
                    removeAllMarkers()
                    val context = context ?: return
                    markersMap.putAll(
                        value.map { markerModel ->
                            markerModel.tag to MapMarkerContainer(context, this, markerModel)
                        }.toMap()
                    )
                    hideLabelsWhenOverlap()
                }
            }
        }

    var selectedMarkerTag: Any?
        set(value) {
            if (googleMap == null) {
                deferredSelectedTag = value
            } else {
                val marker = markersMap[value] ?: return
                selectMapMarker(marker)
            }
        }
        get() = selectedMarker?.marker?.tag

    var areAllGesturesEnabled: Boolean = true
        set(value) {
            field = value
            googleMap?.uiSettings?.setAllGesturesEnabled(value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        googleMapView = InternalMapView(context).apply {
            getMapAsync { gMap ->
                googleMap = gMap
                initializeAdvancedMapView()
                val selectionHelper = MapObjectSelectionHelper(gMap, resources, markersMap)
                onClickListener = { mapTouchPosition ->
                    val markerToSelect = selectionHelper.findMarkerToSelect(mapTouchPosition)
                    if (markerToSelect == null) {
                        handleGoogleMapClick(mapTouchPosition)
                    } else {
                        handleGoogleMarkerClick(markerToSelect)
                    }
                }
            }
            onCreate(savedInstanceState)
        }
    }

    override fun onStart() {
        super.onStart()
        googleMapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        googleMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        googleMapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        googleMapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        googleMapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        googleMapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        googleMapView.onLowMemory()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = googleMapView

    fun getCameraPositionWithoutPadding(padding: MapBounds.Padding, scaleStrategy: MapBounds.ScaleStrategy): MapBounds? {
        val googleMap = googleMap ?: return null
        val view = view ?: return null
        val mapWidthDp = view.width.pxToDp(resources).toDouble()
        val mapHeightDp = view.height.pxToDp(resources).toDouble()
        return MapBounds.fromCameraPositionWithoutPadding(googleMap.cameraPosition, scaleStrategy, padding, mapWidthDp, mapHeightDp)
    }

    @JvmOverloads
    fun moveCamera(mapBounds: MapBounds, animated: Boolean = true) {
        val googleMap = googleMap
        if (googleMap == null) {
            deferredCameraPosition = mapBounds
        } else {
            val view = view ?: return
            val mapWidthDp = view.width.pxToDp(resources).toDouble()
            val mapHeightDp = view.height.pxToDp(resources).toDouble()
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(mapBounds.getCameraPosition(mapWidthDp, mapHeightDp))
            if (animated) {
                googleMap.animateCamera(cameraUpdate)
            } else {
                googleMap.moveCamera(cameraUpdate)
            }
        }
    }

    fun getMarkerFromTag(tag: Any): MapMarker? = markersMap[tag]?.marker

    fun screenLocationOf(coordinate: GeoCoordinate): Point? = googleMap?.projection?.toScreenLocation(coordinate.latLng)

    fun geoCoordinateOf(point: Point): GeoCoordinate? = googleMap?.projection?.fromScreenLocation(point)?.geoCoordinate

    // AdvancedMapFragment internal
    private fun initializeAdvancedMapView() {
        mapStyle = mapStyle
        markers = deferredMarkers
        cameraPosition = deferredCameraPosition
        selectedMarkerTag = deferredSelectedTag
        showCurrentLocation = showCurrentLocation
        areAllGesturesEnabled = areAllGesturesEnabled
        googleMap?.setOnMapLongClickListener { onMapLongClickListener?.invoke(it.geoCoordinate) }
        googleMap?.setOnMarkerClickListener { true }
        googleMap?.setOnCameraMoveStartedListener { reason -> handleCameraMoved(reason) }
        googleMap?.setOnCameraIdleListener { onMapCameraMoveListener?.invoke(MovementState.IDLE, movementReason) }
        googleMap?.setOnCameraMoveListener(::handleGoogleCameraMove)
    }

    private fun handleCameraMoved(reason: Int) {
        movementReason = MovementReason.fromGoogleMapReason(reason)
        onMapCameraMoveListener?.invoke(MovementState.STARTED, movementReason)
    }

    private fun handleGoogleMapClick(clickedPoint: Point) {
        if (deselectMarkerOnMapClick) {
            selectedMarker?.let { deselectMarker(it) }
            hideLabelsWhenOverlap()
        }
        val clickedLocation = geoCoordinateOf(clickedPoint)
        if (clickedLocation != null) {
            onMapClickListener?.invoke(clickedLocation)
        }
    }

    private fun handleGoogleCameraMove() {
        val currentTime = System.currentTimeMillis()
        val currentZoom = googleMap?.cameraPosition?.zoom ?: 0f
        if (currentTime > lastLabelHideTime + LABEL_HIDE_THROTTLING_MS && currentZoom != lastLabelHideZoom) {
            hideLabelsWhenOverlap()
            lastLabelHideTime = currentTime
            lastLabelHideZoom = currentZoom
        }
    }

    private fun handleGoogleMarkerClick(marker: MapMarker): Boolean {
        val clickedMarker = markersMap[marker.tag] ?: return true
        selectMapMarker(clickedMarker)
        onMarkerClickListener?.invoke(clickedMarker.marker)
        return true
    }

    private fun selectMapMarker(mapMarker: MapMarkerContainer) {

        val currentSelectedMarker = selectedMarker
        if (currentSelectedMarker != mapMarker) {
            mapMarker.viewState = MarkerViewState.EXPANDED_WITH_LABEL
            if (currentSelectedMarker != null) {
                deselectMarker(currentSelectedMarker)
            }
        }
        selectedMarker = mapMarker
        hideLabelsWhenOverlap()
    }

    private fun deselectMarker(deselectedMarker: MapMarkerContainer) {
        deselectedMarker.viewState = MarkerViewState.PIN_ONLY // The label will get shown if needed on the next round of hideLabelsWhenOverlap()

        selectedMarker = null
        onMarkerDeselectedListener?.invoke(deselectedMarker.marker)
    }

    private fun removeAllMarkers() {
        selectedMarker?.let { onMarkerDeselectedListener?.invoke(it.marker) }
        selectedMarker = null
        markersMap.values.forEach { it.remove() }
        markersMap.clear()
    }

    private fun hideLabelsWhenOverlap() {
        val googleMap = googleMap ?: return
        val labelsToShow = MapLabelOverlapHider.findLabelsToShow(googleMap, resources, markersMap.values.toSet(), selectedMarker)

        for (marker in markersMap.values) {
            val newViewState = if (labelsToShow.contains(marker)) MarkerViewState.PIN_AND_LABEL else MarkerViewState.PIN_ONLY

            if (marker.mapLabel?.isVisible != labelsToShow.contains(marker)) {
                marker.viewState = newViewState
            }
        }
    }
}
