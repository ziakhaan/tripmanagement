package com.easemytrip.mytripmanagement.services

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import android.location.Criteria
import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.concurrent.TimeUnit
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationResult

import com.google.android.gms.location.LocationCallback
import android.os.Looper
import androidx.core.content.ContextCompat
import com.easemytrip.mytripmanagement.R
import com.easemytrip.mytripmanagement.preferences.LocationPref
import com.easemytrip.mytripmanagement.preferences.LocationPreferences
import com.easemytrip.mytripmanagement.preferences.TripPref
import com.easemytrip.mytripmanagement.ui.MainActivity
import com.easemytrip.mytripmanagement.utils.LocationUtils
import com.google.android.gms.location.LocationRequest


const val INTENT_LOCATION_BROADCAST = "location_broadcast_filer";
const val EXTRA_LOCATION_DATA = "location_data";

/**
 * Frequency for location to be fetched
 */
val MIN_TIME_BW_UPDATES: Long = TimeUnit.SECONDS.toMillis(5)

class MyLocationService : Service(), LocationListener {


    private val TAG: String? = MyLocationService::class.java.simpleName

    /**
     * Location request to create a location request for the fused location provided
     */
    private lateinit var mLocationRequest: LocationRequest

    /**
     * fused location services provider callback which will return the location
     */
    private lateinit var mLocationCallback: LocationCallback

    /**
     * notification id for showing the foreground service notifiation
     */
    private val NOTIFICATION_ID: Int = 100

    /**
     * Channel Id for notification above Android Oreo
     */
    private val CHANNEL_ID: String = "CH_01"

    /**
     *  update location if the device moves the specified distance
     */
    private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 10f


    /**
     * Binder object to connect to the activity
     */
    private val mBinder: IBinder = LocationBinder()

    /**
     * Location manager for GPS and network location fetch
     */
    lateinit var locationManager: LocationManager

    /**
     * object to store current location
     */
    lateinit var currentLocation: Location

    /**
     * fused location object declaration
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    /**
     * tripref model object to hold the trip information
     */
    lateinit var tripRef: TripPref

    /**
     * A list to hold the locations fetched from the start to the stop
     */
    val locationList: ArrayList<LocationPref> = arrayListOf<LocationPref>()

    /**
     * boolean to check if GPS provider is available or not
     */
    var checkGPS = false

    /**
     * boolean to check if NETWORK provider is available or not
     */
    var checkNetwork = false

    /**
     * location provider object
     */
    lateinit var locationProviderStr: String

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    /**
     * Fused location client objects are initialized when the service is created
     */
    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                sendLocationBroadCast(locationResult.lastLocation)
                addLocationToTripPref(locationResult.lastLocation)
            }
        }
        createLocationRequest()
    }

    /**
     * A method to add the locations to the arraylist
     */
    fun addLocationToTripPref(location: Location) {
        location.let {
            val locationPref =
                LocationPref(it.latitude, it.longitude, it.accuracy, LocationPreferences.getDate())
            locationList.add(locationPref)
            Log.d(TAG, "latitude: ${location.latitude}")
            Log.d(TAG, "longitude: ${location.longitude}")
        }
    }

    /**
     * The foreground service notification and location updates start here, since we will have to listen to locations
     * continuously this service has to be re-created even if it is killed
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MyLocationService", "Foreground service started")
        startLocationForeGroundService()
        return START_STICKY
    }

    /**
     * display the notification and start foreground and also create a trip object to be saved to shared preference
     */
    private fun startLocationForeGroundService() {
        lateinit var notifObj: Notification
        var notificationBuilderObj: Notification.Builder
        var notificationCompatBuilder: NotificationCompat.Builder

        val pendingIntent: PendingIntent =
            Intent(applicationContext, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MY_LOCATION_DEF_NOTIF"
            val descriptionText = "Location Services"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            mChannel.description = descriptionText
            notificationManager.createNotificationChannel(mChannel)

            notificationBuilderObj = Notification.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_message))
                .setContentIntent(pendingIntent)
                .setTicker("some probe in notif")
                .setSmallIcon(R.mipmap.ic_launcher)

            notifObj = notificationBuilderObj.build()

        } else {
            notificationCompatBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_message))
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.notification_message))
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(getText(R.string.notification_message))
            notifObj = notificationCompatBuilder.build()
        }
        startForeground(NOTIFICATION_ID, notifObj)
        notificationManager.notify(NOTIFICATION_ID, notifObj)
        startLocation()
        tripRef = LocationPreferences.createTrip(applicationContext)
    }

    /**
     * A method that starts the location
     */
    private fun startLocation() {
        checkGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        checkNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!checkNetwork && !checkGPS)
            startFusedLocationUpdates()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        if (checkGPS) {
            locationProviderStr = LocationManager.GPS_PROVIDER
        } else
            locationProviderStr = LocationManager.NETWORK_PROVIDER
        locationManager.requestLocationUpdates(
            locationProviderStr,
            MIN_TIME_BW_UPDATES,
            MIN_DISTANCE_CHANGE_FOR_UPDATES,
            this
        )
        var locationCpy = locationManager.getLastKnownLocation(locationProviderStr)
        /**
         * if the location manager returns null then the fused location service will start to get the location.In some cases
         * the location fetched by location manager might return null. The location is locally broadcasted and show to the activity
         * if it is bound and registered for this broadcast
         */

        if (locationCpy != null) {
            Log.d(TAG, "FROM LOCATION MANAGER")
            currentLocation = locationManager.getLastKnownLocation(locationProviderStr)!!
            val locationIntent = Intent(INTENT_LOCATION_BROADCAST)
            locationIntent.putExtra(EXTRA_LOCATION_DATA, locationCpy)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(locationIntent)
            addLocationToTripPref(locationCpy)
        } else {
            startFusedLocationUpdates()
        }

        /* NullCheckUtils.safeLet(locationManager,locationManager.getLastKnownLocation(locationProviderStr))
         { locationManager,location->

         }*/
    }

    /**
     * Broadcast location and add the locations to the trip reference object
     */
    override fun onLocationChanged(location: Location) {
        sendLocationBroadCast(location)
        addLocationToTripPref(location)
    }

    /**
     * utility function to add location to the list from either fused location callback or location manager callback
     */
    fun sendLocationBroadCast(location: Location) {
        location.apply {
            val locationIntent = Intent(INTENT_LOCATION_BROADCAST)
            locationIntent.putExtra(EXTRA_LOCATION_DATA, location)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(locationIntent)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }


    /**
     * Stop the service, removes foreground location of both location manager and fused location
     */
    fun stopLocationUpdates() {
        this.stopSelf()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
        mFusedLocationClient?.removeLocationUpdates(mLocationCallback)
        stopForeground(true)

        if (::tripRef.isInitialized) {
            tripRef.locations = locationList
            tripRef.end_time = LocationPreferences.getDate()
            LocationPreferences.updateTrip(applicationContext, tripRef)
        }

    }


    inner class LocationBinder : Binder() {
        val service: MyLocationService
            get() = this@MyLocationService
    }

    /**
     * start the fused location updates
     */
    fun startFusedLocationUpdates() {

        try {
            mFusedLocationClient!!.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback, Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
        }

        // remove updates from the location manager as it will drain battery if both the location services are used, the intention is to use only one location provider
        locationManager.removeUpdates(this)
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationUtils.getLocationRequest()
        /*LocationRequest.create().apply {
            interval = MIN_TIME_BW_UPDATES
            fastestInterval = 1000 * 10
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            maxWaitTime= 1000
        }*/
    }

    /**
     * function to check if the service is in foreground or not
     */
    fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (javaClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }


}