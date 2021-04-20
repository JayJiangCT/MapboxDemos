package com.wonder.mapbox.featrues.ui

import android.Manifest.permission
import android.os.Bundle
import androidx.annotation.LayoutRes
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.wonder.mapbox.featrues.R
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

/**
 * author jiangjay on  19-04-2021
 */
abstract class BaseMapActivity : BaseActivity(), EasyPermissions.PermissionCallbacks {

    companion object {

        private const val RC_LOCATION = 0x70
        private val perms = arrayOf(
            permission.ACCESS_FINE_LOCATION,
            permission.ACCESS_COARSE_LOCATION,
        )
    }

    @get:LayoutRes
    protected abstract val layoutId: Int

    protected abstract val mapView: MapView

    open lateinit var mapboxMap: MapboxMap

    open val mapboxMapInitialized
        get() = ::mapboxMap.isInitialized

    private var permissionEnable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this@BaseMapActivity, getAccessToken())
        setContentView(layoutId)
        permissionCheck()
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            mapboxMap = it
            if (mapboxMapInitialized) {
                mapReady()
            }
        }
    }

    @AfterPermissionGranted(RC_LOCATION)
    private fun permissionCheck() {
        if (!EasyPermissions.hasPermissions(
                this@BaseMapActivity,
                *perms
            )
        ) {
            requestPermissions()
        } else {
            permissionEnable = true
        }
    }

    private fun requestPermissions() {
        EasyPermissions.requestPermissions(
            this@BaseMapActivity,
            "We need your location, storage read and write permissions, please open it",
            RC_LOCATION,
            *perms
        )
    }

    @Throws(UninitializedPropertyAccessException::class)
    @AfterPermissionGranted(RC_LOCATION)
    protected abstract fun mapReady()

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun getAccessToken(): String = getString(R.string.mapbox_access_token)
}