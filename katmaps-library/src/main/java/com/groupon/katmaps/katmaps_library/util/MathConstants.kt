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

import android.graphics.Point
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

internal object MathConstants {
    const val CIRCLE_DEGREES = 360.0
    const val HALF_CIRCLE_DEGREES = CIRCLE_DEGREES / 2.0
}

internal fun Float.clipTo(min: Float, max: Float): Float = min(max(this, min), max)

internal fun Double.clipTo(min: Double, max: Double): Double = min(max(this, min), max)

internal fun Float.degreesToRadians(): Float = this * PI.toFloat() / MathConstants.HALF_CIRCLE_DEGREES.toFloat()

internal fun Double.degreesToRadians(): Double = this * PI / MathConstants.HALF_CIRCLE_DEGREES

internal fun distance(a: Point, b: Point) = sqrt((b.y - a.y).toFloat().pow(2) + (b.y - a.y).toFloat().pow(2))
