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

package com.groupon.katmaps.katmaps_library.model

import com.google.android.gms.maps.model.CameraPosition
import com.groupon.katmaps.katmaps_library.util.clipTo
import com.groupon.katmaps.katmaps_library.util.degreesToRadians
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Defines a geographic bound
 * @param center the geographic center of the bound
 * @param radius the expansion distance of the bound from the center
 * @param bearing the degree of rotation normalized to be between 0-360 degrees
 * @param tilt the degree angle in which the camera faces the earth between 0-90 degrees
 */
class MapBounds private constructor(val center: GeoCoordinate, val radiusX: Length, val radiusY: Length, val bearing: Float, val tilt: Float, val padding: Padding, val scaleStrategy: ScaleStrategy) {
    companion object {
        private val EARTH_CIRCUMFERENCE = 40007.863.kilometers
        private val EARTH_HALF_CIRCUMFERENCE = EARTH_CIRCUMFERENCE / 2.0
        private val DEFAULT_RADIUS = 7.5.kilometers

        private const val GOOGLE_MAP_BASE_ZOOM_DP = 256.0
        private const val LONGITUDE_CORRECTION_FACTOR = 1.35

        private const val EARTH_LATITUDE_DEGREES = 180.0
        private const val EARTH_LONGITUDE_DEGREES = 360.0

        private const val FIFTY_PERCENT = 0.5
        private const val QUARTER_CIRCLE_RADIANS = PI / 2.0

        private const val DEFAULT_BEARING = 0f
        private const val DEFAULT_TILT = 0f

        private const val GOOGLE_MAP_MIN_ZOOM_LEVEL = 1f
        private const val GOOGLE_MAP_MAX_ZOOM_LEVEL = 21f

        const val DEFAULT_PADDING_PROPORTION = 0.05f
        val DEFAULT_PADDING = Padding(DEFAULT_PADDING_PROPORTION, DEFAULT_PADDING_PROPORTION, DEFAULT_PADDING_PROPORTION, DEFAULT_PADDING_PROPORTION)
        val NO_PADDING = Padding(0f, 0f, 0f, 0f)

        @JvmStatic
        @JvmOverloads
        fun fromCenter(center: GeoCoordinate, radius: Length = DEFAULT_RADIUS, bearing: Float = DEFAULT_BEARING, tilt: Float = DEFAULT_TILT, padding: Padding = DEFAULT_PADDING, scaleStrategy: ScaleStrategy = ScaleStrategy.FIT): MapBounds =
            MapBounds(center, radius, radius, bearing, tilt, padding, scaleStrategy)

        internal fun fromCameraPosition(cameraPosition: CameraPosition, viewWidth: Double, viewHeight: Double): MapBounds {
            val center = cameraPosition.target.geoCoordinate
            return MapBounds(
                center = center,
                radiusX = calculateDistanceFromZoom(center, viewWidth, cameraPosition.zoom) / 2.0,
                radiusY = calculateDistanceFromZoom(center, viewHeight, cameraPosition.zoom) / 2.0,
                bearing = cameraPosition.bearing,
                tilt = cameraPosition.tilt,
                padding = NO_PADDING,
                scaleStrategy = ScaleStrategy.FIT
            )
        }

        internal fun fromCameraPositionWithoutPadding(cameraPosition: CameraPosition, scaleStrategy: ScaleStrategy = ScaleStrategy.FIT, padding: Padding, viewWidth: Double, viewHeight: Double): MapBounds {
            val center = cameraPosition.target.geoCoordinate
            val bounds = MapBounds(
                center = center,
                radiusX = calculateDistanceFromZoom(center, viewWidth, cameraPosition.zoom) / 2.0,
                radiusY = calculateDistanceFromZoom(center, viewHeight, cameraPosition.zoom) / 2.0,
                bearing = cameraPosition.bearing,
                tilt = cameraPosition.tilt,
                padding = padding,
                scaleStrategy = ScaleStrategy.FIT
            )

            val (translatedTarget, transformedRadiusX, transformedRadiusY) = transformCameraToPadding(bounds, viewWidth, viewHeight, true)
            return MapBounds(
                center = translatedTarget,
                radiusX = transformedRadiusX,
                radiusY = transformedRadiusY,
                bearing = cameraPosition.bearing,
                tilt = cameraPosition.tilt,
                padding = padding,
                scaleStrategy = scaleStrategy
            )
        }

        private fun transformCameraToPadding(originalBounds: MapBounds, viewWidth: Double, viewHeight: Double, inverse: Boolean = false): Triple<GeoCoordinate, Length, Length> {
            val radius = when (originalBounds.scaleStrategy) {
                ScaleStrategy.WIDTH -> originalBounds.radiusX
                ScaleStrategy.HEIGHT -> originalBounds.radiusY
                ScaleStrategy.FIT -> min(originalBounds.radiusX, originalBounds.radiusY)
                ScaleStrategy.FILL -> max(originalBounds.radiusX, originalBounds.radiusY)
            }

            val visibleProportionX = (1.0 - originalBounds.padding.left - originalBounds.padding.right).clipTo(0.0, 1.0)
            val visibleProportionY = (1.0 - originalBounds.padding.top - originalBounds.padding.bottom).clipTo(0.0, 1.0)

            val transformedRadiusX = if (inverse) originalBounds.radiusX * visibleProportionX else radius / visibleProportionX
            val transformedRadiusY = if (inverse) originalBounds.radiusY * visibleProportionY else radius / visibleProportionY

            // Find proportional offset of new target
            val midpointX = (1.0 + originalBounds.padding.left - originalBounds.padding.right) / 2.0
            val midpointY = (1.0 + originalBounds.padding.top - originalBounds.padding.bottom) / 2.0
            val xMidpointDeltaProportion = midpointX - FIFTY_PERCENT
            val yMidpointDeltaProportion = midpointY - FIFTY_PERCENT

            // Calculate pixel dimensions for region with padding added
            val xScreenPixels = viewWidth * visibleProportionX
            val yScreenPixels = viewHeight * visibleProportionY
            val radiusRestriction = if (xScreenPixels < yScreenPixels) ScaleStrategy.WIDTH else ScaleStrategy.HEIGHT

            // Visible distance shown in each respective direction on the screen
            val xScreenDistance = if (inverse) {
                if (radiusRestriction == ScaleStrategy.WIDTH) originalBounds.radiusX * 2.0 else originalBounds.radiusY * 2.0 * (viewWidth / viewHeight)
            } else {
                if (radiusRestriction == ScaleStrategy.WIDTH) transformedRadiusX * 2.0 else transformedRadiusY * 2.0 * (viewWidth / viewHeight)
            }
            val yScreenDistance = if (inverse) {
                if (radiusRestriction == ScaleStrategy.HEIGHT) originalBounds.radiusY * 2.0 else originalBounds.radiusX * 2.0 * (viewHeight / viewWidth)
            } else {
                if (radiusRestriction == ScaleStrategy.HEIGHT) transformedRadiusY * 2.0 else transformedRadiusX * 2.0 * (viewHeight / viewWidth)
            }

            // Distance offset on where the new translated target should be relative to screen
            val xDistanceShift = xScreenDistance * xMidpointDeltaProportion
            val yDistanceShift = yScreenDistance * yMidpointDeltaProportion

            val (latOffset, longOffset) = distanceToLatLongOffset(originalBounds.bearing, xDistanceShift, yDistanceShift)
            val translatedTarget = if (inverse) {
                GeoCoordinate(originalBounds.center.latitude - latOffset, originalBounds.center.longitude - longOffset)
            } else {
                GeoCoordinate(originalBounds.center.latitude + latOffset, originalBounds.center.longitude + longOffset)
            }

            return Triple(translatedTarget, transformedRadiusX, transformedRadiusY)
        }

        private fun distanceToLatLongOffset(bearing: Float, xDistanceShift: Length, yDistanceShift: Length): Pair<Double, Double> {
            // Convert bearing to follow direction of unit circle. Rotation will be based on delta rather than absolute so offset isn't important.
            val rotation = -1.0 * bearing.degreesToRadians()

            // Figure out the influence of x, y offset distance on latitude/longitude individually
            val rXLat = xDistanceShift * cos(rotation + QUARTER_CIRCLE_RADIANS)
            val rXLong = xDistanceShift * sin(rotation + QUARTER_CIRCLE_RADIANS)
            val rYLat = yDistanceShift * cos(rotation)
            val rYLong = yDistanceShift * sin(rotation)

            val latDistOffset = rXLat + rYLat
            val longDistOffset = rXLong + rYLong

            val latOffset = latFromDistance(latDistOffset)
            val longOffset = -1.0 * longFromDistance(longDistOffset)

            return Pair(latOffset, longOffset)
        }

        private fun mercatorDistortionCorrection(latitude: Double): Double = cos(latitude.degreesToRadians())

        private fun latFromDistance(distance: Length): Double = EARTH_LATITUDE_DEGREES * distance.meters / EARTH_HALF_CIRCUMFERENCE.meters

        private fun longFromDistance(distance: Length): Double = LONGITUDE_CORRECTION_FACTOR * EARTH_LONGITUDE_DEGREES * distance.meters / EARTH_CIRCUMFERENCE.meters

        private fun calculateCameraZoom(center: GeoCoordinate, screenDimenDp: Double, distance: Length): Float =
            log2(screenDimenDp * mercatorDistortionCorrection(center.latitude) * EARTH_CIRCUMFERENCE.meters / (distance.meters * GOOGLE_MAP_BASE_ZOOM_DP)).toFloat()

        private fun calculateDistanceFromZoom(center: GeoCoordinate, screenDimenDp: Double, zoom: Float): Length =
            EARTH_CIRCUMFERENCE * screenDimenDp * mercatorDistortionCorrection(center.latitude) / (GOOGLE_MAP_BASE_ZOOM_DP * 2f.pow(zoom))
    }

    /**
     * Defines the proportion of padding on each side.
     * Ex. 0.05 represents a padding of 5% of the total respective width/height of the map
     */
    data class Padding(
        val top: Float,
        val bottom: Float,
        val left: Float,
        val right: Float
    )

    enum class ScaleStrategy {
        WIDTH, // Scale using width
        HEIGHT, // Scale using height
        FIT, // Expand to fit (inclusive area)
        FILL // Expand to fill view area
    }

    val radius: Length
        get() = when (scaleStrategy) {
            ScaleStrategy.WIDTH -> radiusX
            ScaleStrategy.HEIGHT -> radiusY
            ScaleStrategy.FIT -> min(radiusX, radiusY)
            ScaleStrategy.FILL -> max(radiusX, radiusY)
        }

    /**
     * Returns a North-West and South-East coordinate representing the boundaries of MapBounds
     */
    val corners: Pair<GeoCoordinate, GeoCoordinate>
        get() {
            val south = center.latitude - latFromDistance(radius)
            val north = center.latitude + latFromDistance(radius)
            val east = center.longitude + longFromDistance(radius)
            val west = center.longitude - longFromDistance(radius)
            return Pair(GeoCoordinate(north, west), GeoCoordinate(south, east))
        }

    // Transformations for Java consumption
    fun newCenter(center: GeoCoordinate): MapBounds = fromCenter(center, radius, bearing, tilt, padding, scaleStrategy)

    fun newRadius(radius: Length): MapBounds = fromCenter(center, radius, bearing, tilt, padding, scaleStrategy)

    fun newBearing(bearing: Float): MapBounds = fromCenter(center, radius, bearing, tilt, padding, scaleStrategy)

    fun newTilt(tilt: Float): MapBounds = fromCenter(center, radius, bearing, tilt, padding, scaleStrategy)

    fun newPadding(padding: Padding): MapBounds = fromCenter(center, radius, bearing, tilt, padding, scaleStrategy)

    fun newScaleStrategy(scaleStrategy: ScaleStrategy): MapBounds = fromCenter(center, radius, bearing, tilt, padding, scaleStrategy)

    internal fun getCameraPosition(viewWidth: Double, viewHeight: Double): CameraPosition {
        val (translatedTarget, transformedRadiusX, transformedRadiusY) = transformCameraToPadding(this, viewWidth, viewHeight)

        val zoomX = calculateCameraZoom(translatedTarget, viewWidth, transformedRadiusX * 2.0)
        val zoomY = calculateCameraZoom(translatedTarget, viewHeight, transformedRadiusY * 2.0)
        val zoomLevel = when (scaleStrategy) {
            ScaleStrategy.WIDTH -> zoomX
            ScaleStrategy.HEIGHT -> zoomY
            ScaleStrategy.FIT -> min(zoomX, zoomY)
            ScaleStrategy.FILL -> max(zoomX, zoomY)
        }.clipTo(GOOGLE_MAP_MIN_ZOOM_LEVEL, GOOGLE_MAP_MAX_ZOOM_LEVEL)

        return CameraPosition.Builder()
            .target(translatedTarget.latLng)
            .zoom(zoomLevel)
            .bearing(bearing)
            .tilt(tilt)
            .build()
    }
}
