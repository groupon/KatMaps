# KatMaps

KatMaps is a declarative API wrapper that simplifies the usage of the Android Google Maps SDK. It is written in Kotlin and provides a Kotlin first API.

## Supported Google Maps Features

List of all Google Maps SDK features currently supported by KatMaps

1. Map markers (No z-index property yet)
2. Marker click listener
3. Map click listener
4. Map move listener
4. Map Positioning
3. Google Map Themes

## Additional Map Features

These are features added on top of the Google Maps SDK.

### Marker deselected listener

- Self explanatory - invokes a callback when a marker is deselected.

### Marker animations

- Selected/Deselected marker animations

### Fast marker click response

- Modifies Google map's marker click listener to respond instaneously to marker clicks

### Marker selector

- Improved capability of being able to differentiate markers packed close together on a map. Toggles between a group of markers close to where the user clicks on the map, starting with the marker closest to the user's tap.

### Marker labels/automatic label overlap hiding

- Map marker labels are implemented as part of the KatMaps library. It handles hiding labels that overlap other labels by default.

### Declarative interface

- Set it and forget it. No more waiting and checking for Google Maps to be initialized.
- Enhanced readability
	- Map properties are defined by simple data classes rather than complex builders
- Separation of concerns
	- Developer doesn't have to take into account Google Map's life cycle events

### Precise Map Controls

- Control map with center and radius instead of arbitrary zoom level
- Use padding to account for views obstructing the map's visibility

## Getting Started

### Integrating into your app

1. Import the `katmaps-library` Gradle module into your Android project.

2. Add the KatMaps library as a dependency to your app in `build.gradle`

	```groovy
	implementation project(":katmaps-library")
	```

3. Add your Google Maps API key to `AndroidManifest.xml`

	```xml
    <meta-data
	    android:name="com.google.android.geo.API_KEY"
	    android:value="YOUR_GOOGLE_MAPS_API_KEY_HERE"/>
	```
4. Create a layout with FrameLayout as the placeholder for the `MapFragment` that we'll be adding later

	```xml
	<FrameLayout
		android:id="@+id/mapPlaceholder"
		android:layout_width="match_parent"
		android:layout_height="match_parent" />
	```

5. Create instance of `MapFragment` in your activity

	```kotlin
	private val map = MapFragment()
	```

5. Add fragment to `mapPlaceholder` in your layout with the `MapFragment` object we just created

	```kotlin
	supportFragmentManager.beginTransaction().add(R.id.katmaps, mapPlaceholder).commit()
	```

6. You are now ready to use KatMaps


### Recommendations

- Avoid using the `onMapReady` listener! Being a declarataive API, KatMaps is ready to be used once the object is created.

## Walkthrough

Once you have KatMaps set up, you can begin manipulating the map. Here are some examples of what you can do.

### Setting markers

```kotlin
val pinIcon = MapIcon.Image(someBitmap)
val sevenWonders = listOf(
	MapMarker(
		tag = "Great Wall",
		position = GeoCoordinate(1.0, 1.0),
		icon = pinIcon,
		title = "The Great Wall of China"
	)
)

map.markers = sevenWonders
```

### Removing all markers

```kotlin
map.markers = emptyList()
```

### Setting the marker click listener

```kotlin
map.onMarkerClickListener = { marker ->
    Log.i("Demo", "Clicked: ${marker.labelTitle} - ${marker.labelDescription}")
}
```

### Setting the marker deselected listener

```kotlin
map.onMarkerDeselectedListener = { marker ->
    Log.i("Demo", "Deselected: ${marker.labelTitle} - ${marker.labelDescription}")
}
```

### Setting the map's position (simple)

```kotlin
map.cameraPosition = MapBounds.fromCenter(
    center = GeoCoordinate(41.9019876, -87.6561932),
    radius = 2.miles
)
```

### Show user's location on the map

Note: You must check for permissions or this won't work

```kotlin
map.showCurrentLocation = true
```

### Get/Set camera position via property

```kotlin
val previousCameraPosition: MapBound = map.cameraPosition

map.cameraPosition = MapBounds.fromCenter(
    center = previousCameraPosition.center,
    radius = 2.miles,
    tilt = 30f
)
```

### Setting the map's position with/without animation via function

```kotlin
map.moveCamera(
    mapBounds = MapBounds.fromCenter(
        center = GeoCoordinate(41.9019876, -87.6561932),
        radius = 2.miles
    ),
    animated = true
)
```

### Show a 2 mile radius with 10% padding from all edges

```kotlin
map.cameraPosition = MapBounds.fromCenter(
    center = GeoCoordinate(41.9019876, -87.6561932),
    radius = 2.miles,
    padding = MapBounds.Padding(0.1f, 0.1f, 0.1f, 0.1f)
)
```

### Show exactly 2 miles accounting for 20% of the map's bottom covered

```kotlin
map.cameraPosition = MapBounds.fromCenter(
    center = GeoCoordinate(41.9019876, -87.6561932),
    radius = 2.miles,
    padding = MapBounds.Padding(0f, 0.2f, 0f, 0f)
)
```

## Setting up the Demo Application

1. Add your Google Maps API Key (pick one):
	- In the Android Manifest of the `demo-app` module: 
	
		```xml
		<meta-data
			android:name="com.google.android.geo.API_KEY"
			android:value="PUT_YOUR_KEY_HERE"/>
		```
	
	- Add to your environment variable: 

		`export KATMAPS_DEMO_GOOGLE_KEY="PUT_YOUR_KEY_HERE"`

2. Import into Android Studio
3. Run `demo-app`

## Roadmap

1. Support multiple map providers through a common API
2. Multi-platform support via Kotlin Multiplatform
3. Map Circles/Polylines/Polygons
4. Info windows
5. Efficiently handle diffing changes to marker sets when setting markers rather than removing all markers and creating new ones
6. Allow for multiple markers to be selected/upper bound for markers selected
7. Anything else that's in Google Maps SDK that isn't in KatMaps
8. Animated drawables support for marker animation
9. Thoroughly test `MapBounds.ScaleStrategy`
