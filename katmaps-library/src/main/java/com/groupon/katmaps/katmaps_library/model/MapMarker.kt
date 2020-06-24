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

/**
 * A representation of a map marker
 * @param tag the identifier or payload of a marker
 * @param position the location of the marker on a map
 * @param icon an image overlay representing the marker's point on a map
 * @param labelTitle a text label under the marker with a title
 * @param labelDescription a text label under the marker with a description
 */
data class MapMarker(
    val tag: Any,
    var position: GeoCoordinate,
    val icon: MapIcon,
    var labelTitle: String = "",
    var labelDescription: String = ""
)
