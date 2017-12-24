package com.crystrom.wakemeupwhenmystopcomes;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.location.LocationListener;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


/**
 * Created by Marcus Khan on 12/4/2017.
 */

public class LocationListenerService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    static int startIds = 0;
    GoogleApiClient googleApi;
    Location destination;
    int radius;
    boolean vibrate, ring;
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotifyMgr;
    // Sets an ID for the notification
    int mNotificationId = 001;
    boolean stopService = false;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("service", "In onStartCommand");

        if(intent.getAction().equals("KILL_SERVICE")){
            Log.d("Service","Killing service intent obtained");
            stopService = true;
            if(googleApi != null){
                if(googleApi.isConnected() == true){
                    googleApi.disconnect();
                    googleApi = null;
                }
            }
            mNotifyMgr.cancelAll();
            stopService(new Intent(getBaseContext(), LocationListenerService.class));
            this.stopSelf(startIds);

        }else {
            startIds = startId;
            Log.d("Service Intent", "Starting a service");
            vibrate = intent.getExtras().getBoolean("vibrate");
            ring = intent.getExtras().getBoolean("ring");

            double longitude = intent.getExtras().getDouble("longitude");
            double latitude = intent.getExtras().getDouble("latitude");
            destination = new Location("");
            destination.setLatitude(latitude);
            destination.setLongitude(longitude);
            radius = intent.getExtras().getInt("radius");
            Log.d("radius", radius + "");
            Log.d("latitude", destination.getLatitude() + "");
            Log.d("longitude", destination.getLongitude() + "");
            Log.d("vibrate", vibrate + "");
            Log.d("ring", ring + "");
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (stopService == false) {
            // Initialize the googleapi client
            googleApi = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            googleApi.connect();
            Log.d("GoogleAPI connected?", googleApi.isConnected() + "");

            mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_location_searching_black_24px)
                    .setContentTitle("Location Alarm Set")
                    .setContentText("We will notify you when you are near your destination ;)")
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(false);

            // Gets an instance of the NotificationManager service
            mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // Builds the notification and issues it.
            mNotifyMgr.notify(mNotificationId, mBuilder.build());

        }
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.d("Current location", "Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude());
        // Once the current location is inside the radius in meters, wake up the user.

        if(destination != null) {
            if (destination.distanceTo(location) < (radius * 500)) {
                Log.d("Distance to destination", "" + destination.distanceTo(location));
                // TODO: Ring and vibrate and show notification.
                // TODO: Once the user clicks on notification and kills it.
                // TODO: Kill this service

                Intent intent = new Intent(this, LocationListenerService.class);
                intent.setAction("KILL_SERVICE");
                PendingIntent pIntent = PendingIntent.getService(this,
                        (int) System.currentTimeMillis(), intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_location_searching_black_24px)
                        .setContentTitle("Location Alarm Triggered!")
                        .setContentText("you are nearing your destination. Please wake up ;)")
                        .setOngoing(false)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true)
                        .setLights(Color.BLUE, 200, 200)
                        .addAction(R.drawable.ic_location_searching_black_24px, "Discard notification", pIntent);


                if(vibrate == true){
                    mBuilder.setVibrate(new long[]{2000, 2000, 2000, 2000, 2000});
                }

                if(ring == true){
                    mBuilder.setSound(Uri.parse("android.resource://com.crystrom.wakemeupwhenmystopcomes/" + R.raw.alarm));

                }

                mBuilder.setDeleteIntent(pIntent);


                // Builds the notification and issues it.
                Log.d("LocationServiceMsg", "location reached!");
                mNotifyMgr.notify(mNotificationId, mBuilder.build());
            }
        }


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("googleapi connected?", googleApi.isConnected() +"");

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5); // Update location every  5 second
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApi, mLocationRequest, this);
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApi);
            if (mLastLocation != null) {
                String lat = String.valueOf(mLastLocation.getLatitude());
                String lon = String.valueOf(mLastLocation.getLongitude());
                Log.d("Location", "Latitude = " + lat + "Longitude" + lon);


            }
        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.d("Error", "Error at onConnected(): " + e.toString());
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Location update has been paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        // Disconnect googleapi to stop updating location
        if(googleApi != null) {
            if (googleApi.isConnected() == true) {
                googleApi.disconnect();
                Log.d("Service", "Stopped");
            }
        }

        if(mNotifyMgr!= null){
           mNotifyMgr.cancelAll();
        }
        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
