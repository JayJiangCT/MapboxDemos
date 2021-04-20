package com.wonder.mapbox.featrues.ui

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.OnboardRouterOptions
import com.mapbox.navigation.ui.NavigationView
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.listeners.BannerInstructionsListener
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.wonder.mapbox.featrues.R
import com.wonder.mapbox.featrues.camera.CustomerCamera
import kotlinx.android.synthetic.main.activity_navigation.navigationView

/**
 * author jiangjay on  19-06-2020
 */
class NavigationActivity : BaseNavigationActivity(), NavigationListener,
    BannerInstructionsListener {

    override val layoutId: Int
        get() = R.layout.activity_navigation

    override val navView: NavigationView
        get() = navigationView

    override fun getInitialMapCameraPosition(): CameraPosition {
        val originCoordinate = route.routeOptions()?.coordinates()?.get(0)
        return CameraPosition.Builder()
            .target(LatLng(originCoordinate!!.latitude(), originCoordinate.longitude()))
            .zoom(15.0)
            .build()
    }

    override fun setNavigationViewOptions() {
        val optionsBuilder = NavigationViewOptions.builder(this)
        optionsBuilder.navigationListener(this)
        optionsBuilder.directionsRoute(route)
        // optionsBuilder.locationEngine(LocationEngineProvider.getBestLocationEngine(this)) //do not do this
        optionsBuilder.shouldSimulateRoute(autoDrive)
        optionsBuilder.bannerInstructionsListener(this)
        /*optionsBuilder.navigationOptions(
            defaultNavigationOptionsBuilder(
                this,
                getString(R.string.mapbox_access_token)
            ).build()
        )*/
        optionsBuilder.enableVanishingRouteLine(true)
        navView.retrieveNavigationMapboxMap()?.let { navigationMapboxMap ->
            navigationMapboxMap.retrieveMap().let {
                optionsBuilder.camera(CustomerCamera(it))
            }
        }
        optionsBuilder.navigationOptions(
            NavigationOptions.Builder(this)
                .onboardRouterOptions(OnboardRouterOptions.Builder().tilesVersion("2020_07_25-03_00_00").build())
                .build()
        )
        navView.startNavigation(optionsBuilder.build())
        val coordinates = route.routeOptions()?.coordinates()
        coordinates?.let { list ->
            if (list.size >= 3) {
                list.forEachIndexed { index, point ->
                    if (index != 0 && index != list.size - 1) {
                        addMarker(
                            LatLng(point.latitude(), point.longitude()),
                            "passing-point-$index",
                            ContextCompat.getDrawable(this@NavigationActivity, R.drawable.mid_pointer)!!
                        )
                    }
                }
            }
        }
    }

    private fun addMarker(latLng: LatLng, imageName: String, drawable: Drawable) {
        navigationMapboxMap.retrieveMap().style?.addImage(imageName, drawable)
        navigationMapboxMap.addCustomMarker(SymbolOptions().withLatLng(latLng).withIconImage(imageName))
    }

    override fun onNavigationFinished() {
    }

    override fun onNavigationRunning() {
    }

    override fun onCancelNavigation() {
        navView.stopNavigation()
        finish()
    }

    override fun willDisplay(instructions: BannerInstructions?): BannerInstructions {
        return instructions ?: BannerInstructions.builder().build()
    }
}