package com.wonder.mapbox.featrues.camera

import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.RouteInformation

class CustomerCamera(mapboxMap: MapboxMap) : DynamicCamera(mapboxMap) {
    override fun tilt(routeInformation: RouteInformation): Double {
        var tilt = super.tilt(routeInformation)
        return Math.min(tilt, 30.0)
    }

    override fun zoom(routeInformation: RouteInformation): Double {
        var zoom = super.zoom(routeInformation)
        return Math.max(zoom, 17.0);
    }
}