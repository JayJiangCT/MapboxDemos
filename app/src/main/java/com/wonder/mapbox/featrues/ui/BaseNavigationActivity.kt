package com.wonder.mapbox.featrues.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.LayoutRes
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.location.modes.RenderMode.NORMAL
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState.ROUTE_COMPLETE
import com.mapbox.navigation.base.trip.model.RouteProgressState.ROUTE_INITIALIZED
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.arrival.ArrivalOptions
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.NavigationView
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.map.NavigationMapboxMap

/**
 * author jiangjay on  19-06-2020
 */
inline fun <reified T : BaseNavigationActivity> Context.startNavigation(
    json: String,
    autoDrive: Boolean
) {
    startActivity(
        Intent(this, T::class.java).putExtra("extra_route_json", json)
            .putExtra("auto_drive", autoDrive)
    )
}

abstract class BaseNavigationActivity : BaseActivity(), OnNavigationReadyCallback {

    @get:LayoutRes
    protected abstract val layoutId: Int

    protected abstract val navView: NavigationView

    protected lateinit var navigationMapboxMap: NavigationMapboxMap

    private lateinit var mapboxNavigation: MapboxNavigation

    open val route by lazy {
        DirectionsRoute.fromJson(intent.getStringExtra("extra_route_json") ?: "")
    }
    open val autoDrive by lazy {
        intent.getBooleanExtra("auto_drive", false)
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            routeProgress.let { progress ->
                when {
                    ROUTE_COMPLETE == progress.currentState -> {
                        Log.d("Navigation State", "ARRIVE: ${routeProgress.currentLegProgress?.distanceRemaining}")
                    }
                    ROUTE_INITIALIZED == progress.currentState && progress.distanceRemaining < 10 -> {
                    }
                    else -> Unit
                }
            }
        }
    }

    private val arrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
        }
    }

    private val arrivalController = object : ArrivalController {
        override fun arrivalOptions(): ArrivalOptions {
            return ArrivalOptions.Builder()
                .arrivalInMeters(10.0) //300 feet
                .build()
        }

        override fun navigateNextRouteLeg(routeLegProgress: RouteLegProgress): Boolean {
            return routeLegProgress.distanceRemaining < 10.0
        }
    }

    private val offRouteObserver = object : OffRouteObserver {
        override fun onOffRouteStateChanged(offRoute: Boolean) {
        }
    }

    protected abstract fun getInitialMapCameraPosition(): CameraPosition

    protected abstract fun setNavigationViewOptions()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)
        navView.onCreate(savedInstanceState)
        navView.initialize(
            this,
            getInitialMapCameraPosition()
        )
    }

    override fun onStart() {
        super.onStart()
        navView.onStart()
    }

    override fun onResume() {
        super.onResume()
        navView.onResume()
    }

    override fun onPause() {
        super.onPause()
        navView.onPause()
    }

    override fun onStop() {
        super.onStop()
        navView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
            mapboxNavigation.unregisterOffRouteObserver(offRouteObserver)
            mapboxNavigation.setArrivalController(null)
        }
        navView.retrieveMapboxNavigation()?.toggleHistory(false)
        navView.onDestroy()
    }

    override fun onBackPressed() {
        // If the navigation view didn't need to do anything, call super
        if (!navView.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        navView.onRestoreInstanceState(savedInstanceState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        navView.onLowMemory()
    }

    override fun onNavigationReady(isRunning: Boolean) {
        if (!isRunning && !::navigationMapboxMap.isInitialized) {
            navView.retrieveNavigationMapboxMap()?.let { navigationMapboxMap ->
                this.navigationMapboxMap = navigationMapboxMap
                this.navigationMapboxMap.updateTrafficVisibility(false)
                this.navigationMapboxMap.updateLocationLayerRenderMode(NORMAL)
                setNavigationViewOptions()
                navView.retrieveMapboxNavigation()?.let { mapboxNavigation ->
                    this.mapboxNavigation = mapboxNavigation
                    this.mapboxNavigation.setRoutes(arrayListOf(route))
                    this.mapboxNavigation.toggleHistory(true)
                    this.mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                    this.mapboxNavigation.registerArrivalObserver(arrivalObserver)
                    this.mapboxNavigation.setArrivalController(arrivalController)
                    this.mapboxNavigation.registerOffRouteObserver(offRouteObserver)
                }
            }
        }
    }
}