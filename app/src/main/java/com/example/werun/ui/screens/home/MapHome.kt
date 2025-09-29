package com.example.werun.ui.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource

@Composable
fun MapHome(
    lastLat: Double,
    lastLng: Double,
    modifier: Modifier = Modifier
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                mapView = this
                mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
                    // Thêm marker tại vị trí cuối cùng (nếu tọa độ hợp lệ)
                    if (lastLat != 0.0 && lastLng != 0.0) {
                        val pointSource = geoJsonSource("last-run-point") {
                            feature(Feature.fromGeometry(Point.fromLngLat(lastLng, lastLat)))
                        }
                        style.addSource(pointSource)

                        val pointLayer = symbolLayer("last-run-layer", "last-run-point") {
                            iconImage("marker-15")
                            iconSize(1.5)
                        }
                        style.addLayer(pointLayer)

                        // Zoom vào vị trí ngay sau khi style load
                        mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(lastLng, lastLat))
                                .zoom(15.0)
                                .build()
                        )
                    } else {
                        // Default zoom nếu không có tọa độ
                        mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(106.6297, 10.8231))  // Ví dụ: TP.HCM làm default
                                .zoom(10.0)
                                .build()
                        )
                    }
                }
            }
        },
        update = { view ->
            // Optional: Update nếu tọa độ thay đổi (recompose)
            if (lastLat != 0.0 && lastLng != 0.0 && view.mapboxMap.isStyleLoaded()) {
                view.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(lastLng, lastLat))
                        .zoom(15.0)
                        .build()
                )
            }
        },
        modifier = modifier
            .size(180.dp, 140.dp)
            .clip(RoundedCornerShape(16.dp))
    )
}