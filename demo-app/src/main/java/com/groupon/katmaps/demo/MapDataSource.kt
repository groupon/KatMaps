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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import com.groupon.katmaps.katmaps_library.model.GeoCoordinate
import com.groupon.katmaps.katmaps_library.model.MapIcon
import com.groupon.katmaps.katmaps_library.model.MapMarker

object MapDataSource {
    private val context = KatMapsDemoApplication.appContext
    private val resources = context.resources

    val pinIcon = MapIcon.AnimatedImage.create(350) { scaleProgress ->
        val normalScale = 3f
        val expandedScale = 4.5f
        val scaleDelta = expandedScale - normalScale
        val scale = scaleDelta * scaleProgress + normalScale

        resources.getDrawable(R.drawable.ic_place, null).getBitmap(context, scale)!!
    }

    var markers = listOf(
        MapMarker(
            tag = "Great Wall",
            position = GeoCoordinate(40.440037, 116.568343),
            icon = pinIcon,
            labelTitle = "Great Wall of China",
            labelDescription = "First wonder of the world"
        ),
        MapMarker(
            tag = "Petra",
            position = GeoCoordinate(30.328547, 35.444433),
            icon = pinIcon,
            labelTitle = "Petra",
            labelDescription = "Second wonder of the world"
        ),
        MapMarker(
            tag = "Christ",
            position = GeoCoordinate(-22.951807, -43.210584),
            icon = pinIcon,
            labelTitle = "Christ the Redeemer",
            labelDescription = "Third wonder of the  world"
        ),
        MapMarker(
            tag = "Machu",
            position = GeoCoordinate(-13.158054, -72.546828),
            icon = pinIcon,
            labelTitle = "Machu Picchu",
            labelDescription = "Fourth wonder of the world"
        ),
        MapMarker(
            tag = "Chichen",
            position = GeoCoordinate(20.684418, -88.567735),
            icon = pinIcon,
            labelTitle = "Chichen Itza",
            labelDescription = "Fifth wonder of the world"
        ),
        MapMarker(
            tag = "Colosseum",
            position = GeoCoordinate(41.890298, 12.492252),
            icon = pinIcon,
            labelTitle = "Colosseum",
            labelDescription = "Sixth wonder of the world"
        ),
        MapMarker(
            tag = "Taj",
            position = GeoCoordinate(27.175250, 78.042174),
            icon = pinIcon,
            labelTitle = "Taj Mahal",
            labelDescription = "Seventh wonder of the world"
        ),
        MapMarker(
            tag = "Giza",
            position = GeoCoordinate(29.979272, 31.134213),
            icon = pinIcon,
            labelTitle = "Great Pyramid of Giza",
            labelDescription = "Honorary candidate wonder of the world"
        )
    )

    private fun Drawable.getBitmap(context: Context, scale: Float = 1.0f): Bitmap? {
        return when (this) {
            is BitmapDrawable -> Bitmap.createScaledBitmap(bitmap, (intrinsicWidth * scale).toInt(), (intrinsicHeight * scale).toInt(), true)
            is VectorDrawable -> {
                val bitmap = Bitmap.createBitmap((intrinsicWidth * scale).toInt(), (intrinsicHeight * scale).toInt(), Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawFilter = PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG)
                setBounds(0, 0, (intrinsicWidth * scale).toInt(), (intrinsicHeight * scale).toInt())
                draw(canvas)
                bitmap
            }
            else -> null
        }
    }
}
