package com.wonder.mapbox.featrues.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.gestures.Utils
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.exceptions.InvalidLatLngBoundsException
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMapLongClickListener
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.internal.extensions.inferDeviceLanguage
import com.mapbox.navigation.base.internal.route.RouteUrl
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.ui.camera.CameraUpdateMode.OVERRIDE
import com.mapbox.navigation.ui.camera.NavigationCameraUpdate
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.route.OnRouteSelectionChangeListener
import com.wonder.mapbox.featrues.R
import com.wonder.mapbox.featrues.extension.getBitmap
import kotlinx.android.synthetic.main.activity_multiple_waypoints.clear_btn
import kotlinx.android.synthetic.main.activity_multiple_waypoints.location_button
import kotlinx.android.synthetic.main.activity_multiple_waypoints.map_view
import kotlinx.android.synthetic.main.activity_multiple_waypoints.progress_bar
import kotlinx.android.synthetic.main.activity_multiple_waypoints.start_direct
import kotlinx.android.synthetic.main.activity_multiple_waypoints.start_navigation
import kotlinx.android.synthetic.main.activity_multiple_waypoints.switch_button1
import kotlinx.android.synthetic.main.activity_multiple_waypoints.switch_button2
import kotlinx.android.synthetic.main.activity_multiple_waypoints.switch_button3
import kotlinx.android.synthetic.main.activity_multiple_waypoints.switch_button4

/**
 * author jiangjay on  19-04-2021
 */

private const val ANIMATION_DURATION = 1000
private const val INIT_ZOOM = 17.0
private const val MAX_SIZE = 3

class MultipleWaypointsActivity : BaseMapActivity(), OnRouteSelectionChangeListener, OnMapLongClickListener {

    private var mapboxNavigation: MapboxNavigation? = null

    private lateinit var navMapboxMap: NavigationMapboxMap

    private var numbers: Int = 0
        set(value) {
            if (value != field) {
                start_direct?.visibility = if (markers.isEmpty()) View.GONE else View.VISIBLE
                field = value
            }
        }

    private var route: DirectionsRoute? = null

    private val markers by lazy {
        mutableListOf<Marker>()
    }

    private val optionsBuilder by lazy {
        LocationComponentOptions.builder(mapView.context)
            .elevation(5.0f)
            .accuracyAlpha(.6f)
            .accuracyColor(Color.TRANSPARENT)
            .pulseEnabled(true)
    }

    override val layoutId: Int
        get() = R.layout.activity_multiple_waypoints

    override val mapView: MapView
        get() = map_view

    @SuppressLint("MissingPermission")
    override fun mapReady() {
        start_direct.setOnClickListener {
            fetchRoute()
        }
        start_navigation.setOnClickListener {
            route?.let {
                val json = it.toJson()
                startNavigation<NavigationActivity>(json, false)
            }
        }
        clear_btn.setOnClickListener {
            clear()
            start_direct.visibility = View.GONE
        }
        location_button.setOnClickListener {
            mapboxMap.locationComponent.lastKnownLocation?.let(::animateCamera)
        }
        configureDirectionOptions()
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            navMapboxMap = NavigationMapboxMap.Builder(mapView, mapboxMap, this).build()
            val activationOptionBuilder =
                LocationComponentActivationOptions.builder(this@MultipleWaypointsActivity, style)
                    .locationComponentOptions(optionsBuilder.build())
                    .useDefaultLocationEngine(true)
            mapboxMap.locationComponent.apply {
                activateLocationComponent(activationOptionBuilder.build())
                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.NORMAL
            }
            navMapboxMap.setOnRouteSelectionChangeListener(this)
        }
        mapboxMap.addOnMapLongClickListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Multiple Waypoints"
    }

    override fun onNewPrimaryRouteSelected(directionsRoute: DirectionsRoute?) {
        route = directionsRoute
    }

    override fun onMapLongClick(point: LatLng): Boolean {
        val p = Point.fromLngLat(point.longitude, point.latitude)
        if (switch_button2.isChecked && markers.size == MAX_SIZE - 1) {
            val firstMarker = markers.first()
            mapboxMap.style?.apply {
                removeImage(firstMarker.markerId)
                removeLayer(firstMarker.layerId)
                removeSource(firstMarker.sourceId)
            }
            markers.remove(firstMarker)
        }
        numbers++
        val marker = Marker(p, "waypoints_layer$numbers", "waypoints_source$numbers", "waypoints_marker$numbers")
        markers.add(marker)
        addMarker(marker)
        return true
    }

    @SuppressLint("MissingPermission")
    private fun configureDirectionOptions() {
        val navigationOptions =
            MapboxNavigation.defaultNavigationOptionsBuilder(
                this,
                getAccessToken()
            ).build()
        MapboxNavigation(navigationOptions).also {
            mapboxNavigation = it
        }
    }

    private fun addMarker(marker: Marker, @DrawableRes drawableId: Int = R.drawable.mid_pointer) {
        with(marker) {
            if (mapboxMap.style?.getLayer(marker.layerId) == null) {
                val geoJsonSource = GeoJsonSource(
                    sourceId,
                    Feature.fromGeometry(Point.fromLngLat(point.longitude(), point.latitude()))
                )
                mapboxMap.style?.apply {
                    try {
                        addImage(markerId, getBitmap(this@MultipleWaypointsActivity, drawableId))
                        addSource(geoJsonSource)
                        addLayer(
                            SymbolLayer(layerId, sourceId).withProperties(
                                PropertyFactory.iconImage(markerId),
                                PropertyFactory.iconIgnorePlacement(true),
                                PropertyFactory.iconAllowOverlap(true),
                                PropertyFactory.iconPadding(15.0f)
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                mapboxMap.style?.removeImage(markerId)
                mapboxMap.style?.removeLayer(layerId)
                mapboxMap.style?.removeSource(sourceId)
                addMarker(this)
            }
        }
    }

    private fun fetchRoute() {
        mapboxMap.locationComponent.lastKnownLocation?.let {
            progress_bar.visibility = View.VISIBLE
            val coordinates = markers.map { marker -> marker.point }.toMutableList()
            coordinates.add(0, Point.fromLngLat(it.longitude, it.latitude))
            val builder = RouteOptions.builder()
                .accessToken(getAccessToken())
                .coordinates(coordinates)
                .language(this.inferDeviceLanguage())
                .steps(true)
                .alternatives(switch_button3.isChecked)
                .continueStraight(switch_button4.isChecked)
                .voiceInstructions(true)
                .bannerInstructions(true)
                .voiceUnits(DirectionsCriteria.IMPERIAL)
                .geometries(RouteUrl.GEOMETRY_POLYLINE6)
                .baseUrl(RouteUrl.BASE_URL)
                .user(RouteUrl.PROFILE_DEFAULT_USER)
                .profile(if (switch_button2.isChecked) RouteUrl.PROFILE_DRIVING_TRAFFIC else RouteUrl.PROFILE_DRIVING)
                .requestUuid("")
            if (!switch_button1.isChecked) {
                builder.waypointIndices("0;${coordinates.size - 1}")
            }
            mapboxNavigation?.requestRoutes(builder.build(), object : RoutesRequestCallback {
                override fun onRoutesReady(routes: List<DirectionsRoute>) {
                    progress_bar.visibility = View.GONE
                    if (routes.isNotEmpty()) {
                        val r = routes.component1().also {
                            route = it
                        }
                        r.distance().let { distance ->
                            if (distance > 20.0) {
                                navMapboxMap.drawRoute(r)
                                bounceCameraToRoute(r)
                                start_navigation.visibility = View.VISIBLE
                            } else {
                                Snackbar.make(
                                    mapView,
                                    R.string.error_select_longer_route,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    progress_bar.visibility = View.GONE
                }

                override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
                    progress_bar.visibility = View.GONE
                }
            }
            )
        }
    }

    private fun bounceCameraToRoute(route: DirectionsRoute) {
        route.geometry()?.let { geo ->
            val coordinates =
                LineString.fromPolyline(geo, Constants.PRECISION_6).coordinates().map {
                    LatLng(it.latitude(), it.longitude())
                }
            if (coordinates.size > 1) {
                try {
                    val bounds = LatLngBounds.Builder().includes(coordinates).build()
                    animateCameraInBounds(bounds)
                } catch (exception: InvalidLatLngBoundsException) {
                    exception.printStackTrace()
                }
            }
        }
    }

    private fun animateCameraInBounds(bounds: LatLngBounds) {
        val cameraPos = mapboxMap.getCameraForLatLngBounds(
            bounds,
            intArrayOf(50, 100, 50, Utils.dpToPx(72f).toInt())
        )
        cameraPos?.let { position ->
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(position)
            navMapboxMap.retrieveCamera().update(NavigationCameraUpdate(cameraUpdate).apply {
                setMode(OVERRIDE)
            }, ANIMATION_DURATION)
        }
    }

    private fun animateCamera(location: Location) {
        mapboxMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(LatLng(location)).zoom(INIT_ZOOM).build()
            )
        )
    }

    private fun clear() {
        start_navigation.visibility = View.GONE
        markers.forEach { marker ->
            mapboxMap.style?.apply {
                removeImage(marker.markerId)
                removeSource(marker.sourceId)
                removeLayer(marker.layerId)
            }
        }
        markers.clear()
        navMapboxMap.hideRoute()
    }

    data class Marker(val point: Point, val layerId: String, val sourceId: String, val markerId: String)
}