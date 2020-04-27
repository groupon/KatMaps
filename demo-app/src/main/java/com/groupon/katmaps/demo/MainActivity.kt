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
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.groupon.katmaps.katmaps_library.KatMapsFragment
import com.groupon.katmaps.katmaps_library.model.*
import com.groupon.katmaps.katmaps_library.model.MapBounds
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val katmaps = KatMapsFragment()
    private val poisBottomSheetBehavior: BottomSheetBehavior<PoisBottomSheet> by lazy { BottomSheetBehavior.from(poisBottomSheet) }
    private val poisBottomSheetCollapsedRatio = 0.65f
    private var lastPadding = MapBounds.NO_PADDING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainLayout.addOnLayoutChangeListener(this::onMainLayoutChangeListener)
        supportFragmentManager.beginTransaction().add(R.id.map, katmaps).commit()

        setInitialMapSettings()
        setupBottomSheet()
    }

    private fun onMainLayoutChangeListener(v: View, left: Int, top: Int, right: Int, bottom: Int,
                                           oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
        if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) {
            return
        }
        poisBottomSheetBehavior.peekHeight = (poisBottomSheetCollapsedRatio * map.height).toInt()
    }

    private fun setupBottomSheet() {
        showListButton.setOnClickListener {
            poisBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            showListButton.visibility = GONE
        }
        poisBottomSheet.itemClickListener = { poi ->
            poisBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            katmaps.moveCamera(MapBounds.fromCenter(
                center = poi.position,
                radius = Length.fromMiles(5.0),
                padding = lastPadding
            ), true)
        }
        poisBottomSheetBehavior.isHideable = true
        poisBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        poisBottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            private var previousBound: MapBounds? = null
            private var allowResizeAnimation = false
            private var lastState = poisBottomSheetBehavior.state
            private var lastStationaryState = BottomSheetBehavior.STATE_HIDDEN

            private fun animateMap() {
                val prevBounds = previousBound ?: return

                val bottomSheetCoverProportion = 1f - (poisBottomSheet.top.toFloat() / map.height)
                val newPadding = MapBounds.Padding(0f, bottomSheetCoverProportion, 0f, 0f)
                val bounds = prevBounds.newPadding(newPadding)
                katmaps.moveCamera(bounds, false)
                lastPadding = newPadding
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset <= 0f && allowResizeAnimation) {
                    poisBottomSheet.alpha = (slideOffset + 1.25f) * 1.25f
                    animateMap()
                }
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Android's bottom sheets suck ass - hence all these checks
                // Save the current visible region bounds before the bottom sheet starts to slide
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    showListButton.visibility = VISIBLE
                }

                if (hasStartedSliding(lastState, newState) && lastState != BottomSheetBehavior.STATE_EXPANDED) {
                    val bottomSheetCoverProportion = 1f - (poisBottomSheet.top.toFloat() / map.height)
                    val startingBottomPadding = MapBounds.Padding(0f, bottomSheetCoverProportion, 0f, 0f)
                    previousBound = katmaps.getCameraPositionWithoutPadding(startingBottomPadding, MapBounds.ScaleStrategy.HEIGHT)
                    allowResizeAnimation = true
                    katmaps.areAllGesturesEnabled = false
                }

                // Add in final animation frame
                if (isFinishingSliding(lastStationaryState, lastState, newState)) {
                    animateMap()
                }

                // Unlock map if done animating
                if (newState != BottomSheetBehavior.STATE_DRAGGING && newState != BottomSheetBehavior.STATE_SETTLING) {
                    allowResizeAnimation = false
                    katmaps.areAllGesturesEnabled = true
                }


                lastState = newState
                if (newState != BottomSheetBehavior.STATE_DRAGGING && newState != BottomSheetBehavior.STATE_SETTLING) {
                    lastStationaryState = newState
                }
            }

            private fun isFinishingSliding(lastStationaryState: Int, lastState: Int, newState: Int) =
                (lastState == BottomSheetBehavior.STATE_DRAGGING || lastState == BottomSheetBehavior.STATE_SETTLING) &&
                        (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) && lastStationaryState != BottomSheetBehavior.STATE_EXPANDED

            private fun hasStartedSliding(lastState: Int, newState: Int) =
                (newState == BottomSheetBehavior.STATE_DRAGGING ||
                        newState == BottomSheetBehavior.STATE_SETTLING && lastState != BottomSheetBehavior.STATE_DRAGGING)
        })
    }

    private fun setInitialMapSettings() {
        katmaps.cameraPosition = MapBounds.fromCenter(
            center = GeoCoordinate(33.0, 24.0),
            radius = 800.miles
        )
        katmaps.deselectMarkerOnMapClick = true
        katmaps.markers = MapDataSource.markers
        katmaps.onMarkerClickListener = { toast("Marker clicked @ ${it.labelTitle}") }
        katmaps.onMapClickListener = { toast("Map clicked @ $it") }
        katmaps.onMapCameraMoveListener = ::handleCameraMove
    }

    private fun handleCameraMove(movementState: MovementState, movementReason: MovementReason) {
        if (movementState == MovementState.IDLE && (movementReason == MovementReason.GESTURE || movementReason == MovementReason.API_ANIMATION)) {
            showVisibleMarkers()
        }
    }

    private fun showVisibleMarkers() {
        val visibleMarkers = katmaps.markers.filter {
            katmaps.screenLocationOf(it.position)?.let { mapPoint ->
                mapPoint.x > 0 && mapPoint.x < map.width && mapPoint.y > 0 && mapPoint.y < map.height
            } ?: false
        }
        toast("${visibleMarkers.map { it.labelTitle }}")
    }

    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
