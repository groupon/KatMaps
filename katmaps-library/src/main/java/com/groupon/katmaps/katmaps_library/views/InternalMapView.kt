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

package com.groupon.katmaps.katmaps_library.views

import android.content.Context
import android.graphics.Point
import android.view.GestureDetector
import android.view.MotionEvent
import com.google.android.gms.maps.MapView

/**
 * A modified version of Google's MapView that adds extra functionality
 * for use internally.
 */
internal class InternalMapView(context: Context?) : MapView(context) {
    var onClickListener: ((Point) -> Unit)? = null

    private val gestureDetector = GestureDetector(this.context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClickListener?.invoke(Point(e.x.toInt(), e.y.toInt()))
            return super.onSingleTapUp(e)
        }
    })

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(e)
        return super.dispatchTouchEvent(e)
    }
}
