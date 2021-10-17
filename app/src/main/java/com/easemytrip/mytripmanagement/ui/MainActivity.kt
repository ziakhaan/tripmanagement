package com.easemytrip.mytripmanagement.ui

import android.app.*
import android.content.*
import android.location.Location
import android.os.Build
import androidx.databinding.DataBindingUtil
import com.easemytrip.mytripmanagement.databinding.ActivityMainBinding

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RequiresApi
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.easemytrip.mytripmanagement.preferences.LocationPreferences
import com.easemytrip.mytripmanagement.R
import com.easemytrip.mytripmanagement.preferences.AppPreferences
import com.easemytrip.mytripmanagement.services.EXTRA_LOCATION_DATA
import com.easemytrip.mytripmanagement.services.INTENT_LOCATION_BROADCAST
import com.easemytrip.mytripmanagement.services.MyLocationService
import com.easemytrip.mytripmanagement.utils.ExportUtils
import com.easemytrip.mytripmanagement.utils.LocationUtils
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.gson.GsonBuilder


class MainActivity : AppCompatActivity() {


    private val TAG: String? = MainActivity::class.java.simpleName

    /**
     * viewbinding to the android widgets to avoid findviewbyid
     */
    private lateinit var viewBinding: ActivityMainBinding
    lateinit var mContext: Context
    private lateinit var mMainActivityObj: MainActivity

    /**
     * permission constants
     */
    val COARSE_LOCATION: String = android.Manifest.permission.ACCESS_COARSE_LOCATION
    val FINE_LOCATION: String = android.Manifest.permission.ACCESS_FINE_LOCATION

    @RequiresApi(Build.VERSION_CODES.Q)
    val BACKGROUND_LOCATION = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION

    /**
     * fine and coarse permission array
     */
    val FINE_COARSE_PERMS = arrayOf(
        LocationUtils.COARSE_LOCATION,
        LocationUtils.FINE_LOCATION
    )

    /**
     * location request for Location settings dialog
     */
    private lateinit var locationRequest: LocationRequest

    private val REQUEST_CHECK_SETTING = 1001

    /**
     * used to check if the service is bound or not
     */
    private var mBound = false

    /**
     * service object to bind with activity
     */
    private lateinit var mService: MyLocationService

    /**
     * broadcast to receive the location updates from the service
     */
    val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val location = intent?.getParcelableExtra<Location>(EXTRA_LOCATION_DATA)
            viewBinding.tvLatitude.text = location?.latitude.toString()
            viewBinding.tvLongitude.text = location?.longitude.toString()
        }
    }

    /**
     * service connection object to bind with the activity
     */
    val serviceConnectionLocation = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MyLocationService.LocationBinder
            mService = binder.service
            mBound = true

            // register for location broadcast once the service is connected
            LocalBroadcastManager.getInstance(mContext).registerReceiver(
                locationReceiver,
                IntentFilter(INTENT_LOCATION_BROADCAST)
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }

    }

    /**
     * bind the location service as soon the activity is started
     */
    override fun onStart() {
        super.onStart()
        Intent(this, MyLocationService::class.java).also { intent ->
            bindService(intent, serviceConnectionLocation, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        mMainActivityObj = this;
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewBinding.tvPerDeny.visibility = View.GONE

        viewBinding.btnStartLocation.isEnabled =
            AppPreferences.getPreference(mContext, AppPreferences.START_BTN_ENABLE)
        viewBinding.btnStopLocation.isEnabled =
            AppPreferences.getPreference(mContext, AppPreferences.STOP_BTN_ENABLE)

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
            locationReceiver,
            IntentFilter(INTENT_LOCATION_BROADCAST)
        )

        //Enable and disable the start and stop buttons
        viewBinding.btnStartLocation.setOnClickListener {
            it.isEnabled = false
            viewBinding.btnStopLocation.isEnabled = true
            checkFineAndCoarsePermissions(this::startLocationUpdates)
            setPreferencesStartAndStopBtns()
        }

        viewBinding.btnStopLocation.setOnClickListener {
            it.isEnabled = false
            viewBinding.btnStartLocation.isEnabled = true
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(locationReceiver)
            setPreferencesStartAndStopBtns()
            if (mBound)
                mService.stopLocationUpdates()
        }

        // Export the shared preference tripRef objects to a json file
        viewBinding.btnExport.setOnClickListener {
            val gson = GsonBuilder().setPrettyPrinting().create()
            var jsonPrettyPrinter: String
            jsonPrettyPrinter = gson.toJson(
                LocationPreferences.getTripList(mContext),
                LocationPreferences.getTripRefType()
            )
            val directoryName = ExportUtils.writeStringAsFile(mContext, jsonPrettyPrinter)
            viewBinding.tvExportDirName.text = "File exported at $directoryName"
        }

    }


    override fun onStop() {
        super.onStop()
        if (mBound) {
            // Unbind from the service since the activity is not visible
            unbindService(serviceConnectionLocation)
            mBound = false
        }
        //unregister receiver
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(locationReceiver)

    }

    /**
     * start method to check for the permissions if success then the method passed a parameter will be called
     */
    private fun checkFineAndCoarsePermissions(startLocationUpdates: () -> Unit) {
        LocationUtils.checkFineAndCoarsePermissions(
            this,
            startLocationUpdates,
            this::fineAndCoarsePermissionDenied
        )
    }

    /**
     * forward the permission response to the location util class to handle the permission request, if coarse and fine are present the location updates will
     * start else fused location client will start location updates
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LocationUtils.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            this,
            this::startLocationUpdates,
            this::fineAndCoarsePermissionDenied
        )
    }


    /**
     * for creating the location settings dialog request
     */
    private fun startLocationUpdates() {
        createLocationSettingsRequest()
    }

    /**
     * Location settings request dialog
     */
    private fun createLocationSettingsRequest() {

        locationRequest = LocationUtils.getLocationRequest()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val result: Task<LocationSettingsResponse> =
            LocationServices.getSettingsClient(applicationContext)
                .checkLocationSettings(builder.build())

        result.addOnCompleteListener {
            try {
                val response: LocationSettingsResponse = it.getResult(ApiException::class.java)
                startLocationForeGroundService()
                Log.d(TAG, "createLocationSettingsRequest() GPS TURNED ON")
            } catch (e: ApiException) {
                when (e.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        val resolvableApiException = e as ResolvableApiException
                        resolvableApiException.startResolutionForResult(
                            this@MainActivity,
                            REQUEST_CHECK_SETTING
                        )
                        Log.d(TAG, "createLocationSettingsRequest(): RESOLUTION_REQUIRED")
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        // USER DEVICE DOES NOT HAVE LOCATION OPTION
                    }
                }
            }
        }
    }

    /**
     * start the foreground services
     */
    private fun startLocationForeGroundService() {
        viewBinding.tvPerDeny.visibility = View.GONE
        ContextCompat.startForegroundService(
            mContext,
            Intent(mContext, MyLocationService::class.java)
        )
    }

    /**
     * starts the fused location updates
     */
    private fun fineAndCoarsePermissionDenied() {
        viewBinding.btnStartLocation.isEnabled = true
        viewBinding.btnStopLocation.isEnabled = false
        setPreferencesStartAndStopBtns()
        viewBinding.tvPerDeny.visibility = View.VISIBLE
        viewBinding.tvPerDeny.text = "PERMISSIONS DENIED. UNABLE TO FETCH LOCATION"
    }

    fun setPreferencesStartAndStopBtns() {
        AppPreferences.writeBooleanPreference(
            mContext,
            AppPreferences.START_BTN_ENABLE,
            viewBinding.btnStartLocation.isEnabled
        )
        AppPreferences.writeBooleanPreference(
            mContext,
            AppPreferences.STOP_BTN_ENABLE,
            viewBinding.btnStopLocation.isEnabled
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTING -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        startLocationForeGroundService()
                    }
                    Activity.RESULT_CANCELED -> {
                        viewBinding.btnStartLocation.isEnabled = true
                        viewBinding.btnStopLocation.isEnabled = false
                        setPreferencesStartAndStopBtns()
                        viewBinding.tvPerDeny.visibility = View.VISIBLE
                        viewBinding.tvPerDeny.text = "GPS TURNED OFF.PLEASE ENABLE GPS"
                    }
                }
            }
        }
    }

}


