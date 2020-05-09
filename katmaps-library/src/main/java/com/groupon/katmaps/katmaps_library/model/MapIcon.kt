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

import android.animation.TimeInterpolator
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.animation.AnticipateOvershootInterpolator
import java.util.concurrent.TimeUnit

sealed class MapIcon {
    /**
     * Define a static icon
     * @param image the icon in Bitmap format
     */
    data class Image(val image: Bitmap) : MapIcon()

    /**
     * Define an animated icon
     * @param images the consecutive list of frames in Bitmap that make up an animation
     * @param duration time in milliseconds to playback the animations
     * @param expanded the state of animation. False = playback in reverse from the end. True = playback forward from the beginning.
     */
    data class AnimatedImage(val images: List<Bitmap>, val duration: Long, var expanded: Boolean) : MapIcon() {
        companion object {
            const val FRAME_RATE = 60L

            /**
             * Create an icon with a provided duration in milliseconds, time interpolator, and a function to generate bitmaps
             * @param durationMS the time in milliseconds this animation should complete
             * @param interpolator the interpolator used for inbetweening animations
             * @param generator the higher order function used for generating a frame of the animation given the progress from 0% - 100%
             */
            inline fun create(
                durationMS: Long,
                interpolator: TimeInterpolator = AnticipateOvershootInterpolator(),
                generator: (scaleProgress: Float) -> Bitmap
            ): AnimatedImage {
                val interval = TimeUnit.SECONDS.toMillis(1) / FRAME_RATE
                val steps = (durationMS / interval).toInt()

                val frames = (0..steps).map { step ->
                    val animationPosition = interpolator.getInterpolation(step.toFloat() / steps.toFloat())
                    generator(animationPosition)
                }

                return AnimatedImage(frames, durationMS, false)
            }
        }
    }
}

fun MapIcon.getSize(isExpanded: Boolean) = when (this) {
    is MapIcon.Image -> Rect(0, 0, image.width, image.height)
    is MapIcon.AnimatedImage -> {
        // get the largest (last) frame if expanded, otherwise get the smallest (first) frame if default size
        val image = if (isExpanded) images.last() else images.first()
        Rect(0, 0, image.width, image.height)
    }
}
