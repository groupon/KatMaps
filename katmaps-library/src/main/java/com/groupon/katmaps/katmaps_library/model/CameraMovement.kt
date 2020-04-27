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

import com.google.android.gms.maps.GoogleMap

enum class MovementReason {
    UNKNOWN,
    GESTURE,
    API_ANIMATION,
    DEVELOPER_ANIMATION;

    companion object {
        fun fromGoogleMapReason(reason: Int) = when (reason) {
            GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE -> GESTURE
            GoogleMap.OnCameraMoveStartedListener.REASON_API_ANIMATION -> API_ANIMATION
            GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION -> DEVELOPER_ANIMATION
            else -> UNKNOWN
        }
    }
}

enum class MovementState {
    STARTED,
    IDLE
}
