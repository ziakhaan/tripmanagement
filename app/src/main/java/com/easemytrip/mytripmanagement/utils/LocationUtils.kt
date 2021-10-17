package com.easemytrip.mytripmanagement.utils

import android.Manifest
import android.app.Activity

import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.easemytrip.mytripmanagement.services.MIN_TIME_BW_UPDATES
import com.google.android.gms.location.LocationRequest

object LocationUtils {


    @RequiresApi(Build.VERSION_CODES.Q)
    const val BACKGROUND_LOCATION = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    /**
     *
     */
    private const val FOREGROUND_ONLY_REQ_CODE = 1
    private const val FOREGROUND_AND_BACKGROUND_REQ_CODE = 2
    const val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    val FINE_COARSE_PERMS = arrayOf(
        COARSE_LOCATION,
        FINE_LOCATION
    )


    /**
     * check if the specified permission is granted
     */
    fun isPermissionGranted(context: Context, permissionName: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permissionName
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * check if the specified permission is denied
     */
    fun isPermissionDenied(context: Context, permissionName: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permissionName
        ) == PackageManager.PERMISSION_DENIED
    }

    /**
     * Showing the dialog if permissions are not granted
     */
    fun showFineAndCoarseDialog(activity: Activity, fineAndCoarsePermissionDenied: () -> Unit) {
        val alertDialogBuilder = AlertDialog.Builder(activity)
        alertDialogBuilder.setTitle("Location Permission")
        alertDialogBuilder.setMessage("We would need location permission in-order for the apps intended use of location functionality")
        alertDialogBuilder.setPositiveButton("GRANT") { dialogInterface: DialogInterface, i: Int ->
            requestCoarseAndFinePermissions(
                activity
            )
        }
        alertDialogBuilder.setNegativeButton("DENY") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
            fineAndCoarsePermissionDenied()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog!!.show()
    }

    /**
     * Dialog to show after the location permission has been granted
     */
    fun showBackgroundLocationDialog(activity: Activity, startLocationUpdates: () -> Unit) {
        val alertDialogBuilder = AlertDialog.Builder(activity)
        alertDialogBuilder.setTitle("Background Location Permission")
        alertDialogBuilder.setMessage("We would need to access location in background to provide services even if you are outside the app")
        alertDialogBuilder.setPositiveButton("GRANT") { dialogInterface: DialogInterface, i: Int ->
            requestBackGroundLocationPermission(
                activity
            )
        }
        alertDialogBuilder.setNegativeButton("DENY") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
            startLocationUpdates()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog!!.show()
    }

    /**
     * dialog to show if none of the permissinons are granted
     */
    fun showNoPermissionGrantedDialog(
        activity: Activity,
        fineAndCoarsePermissionDenied: () -> Unit
    ) {
        val alertDialogBuilder = AlertDialog.Builder(activity)
        alertDialogBuilder.setTitle("Location permission unavailable")
        alertDialogBuilder.setMessage("We do not have none of the location permissions please grant us the permissions ")
        alertDialogBuilder.setPositiveButton("GRANT") { dialogInterface: DialogInterface, i: Int ->
            requestCoarseAndFinePermissions(
                activity
            )
        }
        alertDialogBuilder.setNegativeButton("DENY") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
            fineAndCoarsePermissionDenied()

        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog!!.show()
    }

    /**
     * to check if both fine and coarse permissions are granted
     */
    fun hasBothFineAndCoarsePermission(context: Context): Boolean {
        return isPermissionGranted(
            context,
            COARSE_LOCATION
        ) && isPermissionGranted(context, FINE_LOCATION)
    }

    /**
     * check if fine location and coarse location permission is denied  if rationale is to be shown the rationale dialog is shown
     * else the permissions are requested, if both the permissions are there location update is started if none of them are there
     * background location permission is requested at-least prompting the user to give the permission
     */
    fun checkFineAndCoarsePermissions(
        activity: Activity,
        startLocationUpdates: () -> Unit,
        fineAndCoarsePermissionDenied: () -> Unit
    ) {
        if (isPermissionDenied(
                activity,
                FINE_LOCATION
            ) && isPermissionDenied(activity, COARSE_LOCATION)
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, COARSE_LOCATION)
                && ActivityCompat.shouldShowRequestPermissionRationale(activity, FINE_LOCATION)
            ) {
                showFineAndCoarseDialog(activity, fineAndCoarsePermissionDenied)
            } else {
                requestCoarseAndFinePermissions(activity)
            }
        } else if (hasBothFineAndCoarsePermission(activity)) {
            startLocationUpdates()
        } else {
            checkForBackGroundPermissions(activity)
        }


    }

    /**
     * request only foreground permissions
     */
    private fun requestCoarseAndFinePermissions(activity: Activity) {
        ActivityCompat.requestPermissions(activity, FINE_COARSE_PERMS, FOREGROUND_ONLY_REQ_CODE)
    }


    /**
     * check to see if background permissions are present
     */
    private fun checkForBackGroundPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isPermissionDenied(activity, BACKGROUND_LOCATION)) {
                requestBackGroundLocationPermission(activity)
            }
        }
    }

    /**
     * reqiestoomg background location permissions
     */
    private fun requestBackGroundLocationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(BACKGROUND_LOCATION),
                FOREGROUND_AND_BACKGROUND_REQ_CODE
            )
        }

    }

    /**
     * if GPS and network locations are granted background location dialog is requested for continous tracking of location, if background permission is provided
     * then the location service is started
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        activity: Activity,
        startLocationUpdates: () -> Unit,
        fineAndCoarsePermissionDenied: () -> Unit
    ) {
        when (requestCode) {
            FOREGROUND_ONLY_REQ_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showBackgroundLocationDialog(activity, startLocationUpdates)
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        COARSE_LOCATION
                    )
                    && ActivityCompat.shouldShowRequestPermissionRationale(activity, FINE_LOCATION)
                ) {
                    showFineAndCoarseDialog(activity, fineAndCoarsePermissionDenied)
                } else {
                    fineAndCoarsePermissionDenied()
                    /*activity.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", activity.packageName, null),
                        ),
                    )*/
                }
            }
            FOREGROUND_AND_BACKGROUND_REQ_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && hasBothFineAndCoarsePermission(
                        activity
                    )
                ) {
                    startLocationUpdates()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    var shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        BACKGROUND_LOCATION
                    )
                    if (shouldShowRationale)
                        showBackgroundLocationDialog(activity, fineAndCoarsePermissionDenied)
                    else
                        startLocationUpdates()
                } else if (hasBothFineAndCoarsePermission(activity)) {
                    startLocationUpdates()
                } else {
                    showNoPermissionGrantedDialog(activity, fineAndCoarsePermissionDenied)
                }
            }
        }
    }

    fun getLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest.create().apply {
            interval = MIN_TIME_BW_UPDATES
            fastestInterval = 1000 * 10
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            maxWaitTime = 2000
        }
        return locationRequest
    }

}


