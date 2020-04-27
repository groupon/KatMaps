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

package com.groupon.katmaps.katmaps_library.util

import android.content.res.Resources
import android.graphics.Point
import android.graphics.Rect
import com.google.android.gms.maps.GoogleMap
import com.groupon.katmaps.katmaps_library.MapMarkerContainer
import com.groupon.katmaps.katmaps_library.model.GeoCoordinate
import com.groupon.katmaps.katmaps_library.model.KatMapsMarker
import com.groupon.katmaps.katmaps_library.model.latLng

internal class MapObjectSelectionHelper(
    private val map: GoogleMap,
    private val resources: Resources,
    private val markersSet: HashMap<Any, MapMarkerContainer>
) {

    companion object {
        private const val TOUCH_DIST_THRESHOLD_PX = 35
        private const val SET_SIMILARITY_THRESHOLD = 0.8
    }

    private fun GoogleMap.screenLocationOf(coordinate: GeoCoordinate): Point? = this.projection.toScreenLocation(coordinate.latLng)

    private val previousTouchedMarkers: LinkedHashSet<KatMapsMarker> = LinkedHashSet()
    private val toggledMarkers: HashSet<KatMapsMarker> = HashSet()

    fun findMarkerToSelect(mapTouchPosition: Point): KatMapsMarker? {
        val currentMarkersTouched = findMarkersTouched(mapTouchPosition)

        val currentMarkersTouchedCount = currentMarkersTouched.count()
        if (currentMarkersTouchedCount == 0) {
            return null
        }

        val markerToSelect: KatMapsMarker?
        val setSimilarity = (currentMarkersTouched - previousTouchedMarkers).count().toFloat() / currentMarkersTouchedCount
        if (setSimilarity > SET_SIMILARITY_THRESHOLD) {
            // Similar set, cycle between markers
            val nextToSelect = currentMarkersTouched.find { !toggledMarkers.contains(it.katmapsMarker) }
            markerToSelect = if (nextToSelect == null) {
                // Restart cycling
                toggledMarkers.clear()
                toggledMarkers.add(currentMarkersTouched.first().katmapsMarker)
                currentMarkersTouched.first().katmapsMarker
            } else {
                // move on to next marker
                toggledMarkers.add(nextToSelect.katmapsMarker)
                nextToSelect.katmapsMarker
            }
        } else {
            // New set, select marker
            toggledMarkers.clear()
            markerToSelect = currentMarkersTouched.first().katmapsMarker
        }
        previousTouchedMarkers.clear()
        previousTouchedMarkers.addAll(currentMarkersTouched.map { it.katmapsMarker })
        return markerToSelect
    }

    private fun findMarkersTouched(mapTouchPosition: Point): Sequence<MapMarkerContainer> {
        val xDp = mapTouchPosition.x.pxToDp(resources)
        val yDp = mapTouchPosition.y.pxToDp(resources)
        val dThresh = TOUCH_DIST_THRESHOLD_PX.pxToDp(resources)
        val clickBox = Rect(xDp - dThresh, yDp - dThresh, xDp + dThresh, yDp + dThresh)

        return markersSet.values
            .asSequence()
            .filter { Rect.intersects(clickBox, it.getPinBounds(map, resources)) }
            .mapNotNull { markerContainer ->
                val screenLocation = map.screenLocationOf(markerContainer.katmapsMarker.position)
                if (screenLocation != null) Pair(markerContainer, distance(mapTouchPosition, screenLocation)) else null
            }
            .sortedBy { it.second }
            .map { it.first }
    }
}
