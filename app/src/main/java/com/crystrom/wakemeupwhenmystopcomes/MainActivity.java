package com.crystrom.wakemeupwhenmystopcomes;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;

import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    public static final int REQUEST_ACCESS_LOCATION = 1;
    public static final int REQUEST_VIBRATE = 2;
    GoogleMap mMap;
    LatLng destination;
    Button setBtn;
    SeekBar radius;
    Switch ring, vibrate;
    boolean isRingChecked = false, isVibrateChecked = false;
    LocationManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);

        manager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);



        radius = (SeekBar) findViewById(R.id.seekBar);
        // Setting max radius of 10 kms
        radius.setMax(10);
        radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (destination != null) {
                    if (mMap != null) {
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(destination));
                        CircleOptions circle = new CircleOptions();
                        mMap.addCircle(circle.center(destination).radius(progress * 500).fillColor(Color.parseColor("#1500BFFF")));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        ring = (Switch) findViewById(R.id.switch2);
        ring.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isRingChecked = isChecked;
            }
        });

        vibrate = (Switch) findViewById(R.id.switch1);
        vibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isVibrateChecked = isChecked;
            }
        });


        setBtn = (Button) findViewById(R.id.setBtn);

        setBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (setBtn.getText().equals("Set trigger")) {
                    Log.d("Set trigger", "Pressed");
                    if (destination != null) {
                        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            // Highlight the broadcast item to indicate that the user is broadcasting their location.

                            int locationState = checkPermission("android.permission.ACCESS_FINE_LOCATION", Binder.getCallingPid(), Binder.getCallingUid());
                            if ((locationState != PackageManager.PERMISSION_GRANTED)) {//check if permission is granted, if not ask for permission
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_LOCATION);
                                Log.d("Set trigger", "Location permission granted");
                            }

                            Intent intent = new Intent(getBaseContext(), LocationListenerService.class);
                            Bundle bundle = new Bundle();
                            bundle.putDouble("latitude",destination.latitude);
                            bundle.putDouble("longitude", destination.longitude);
                            //bundle.putDouble("latitude", 7.802739593747642);
                            //bundle.putDouble("longitude", 14.9198392778635);
                            bundle.putBoolean("vibrate", isVibrateChecked);
                            bundle.putBoolean("ring", isRingChecked);
                            if ((isVibrateChecked | isRingChecked) == true) {
                                Log.d("Set trigger", "check point 1");
                                int vibrateState = checkPermission("android.permission.VIBRATE", Binder.getCallingPid(), Binder.getCallingUid());
                                if ((vibrateState != PackageManager.PERMISSION_GRANTED)) {//check if permission is granted, if not ask for permission
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.VIBRATE}, REQUEST_VIBRATE);
                                    Log.d("Set trigger", "vibrate permission not granted");
                                }

                                if (radius.getProgress() == 0) {
                                    Toast.makeText(MainActivity.this, "Radius should not be equal to 0, please find minium 1 km radius to proceed", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.d("Set trigger", "check point 2");
                                    bundle.putInt("radius", radius.getProgress());
                                    intent.putExtras(bundle);
                                    intent.setAction("HODOR");
                                    setBtn.setText("Stop Trigger");
                                    startService(intent);
                                    // TODO: Start and bound the service. As long as the acitivity is in front,
                                    // TODO: you should show the current location of user in the shown map.
                                    // TODO: Once the activity is not visible or home is pressed, the service should
                                    // TODO: run in the background.


                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Either vibrate or Ring should be selected for us to notify you!", Toast.LENGTH_SHORT).show();
                            }


                        } else {
                            Toast.makeText(MainActivity.this, "Please enable location!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Please set a location before proceeding!", Toast.LENGTH_SHORT).show();

                    }


                } else {
                    Intent intent = new Intent(MainActivity.this, LocationListenerService.class);
                    intent.setAction("KILL_SERVICE");
                    //startService(intent);
                    stopService(intent);
                    setBtn.setText("Set trigger");
                    Log.d("Trigger", "Stopping service");
                }
            }
        });


        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        if (preferences.getBoolean("first_time", true) == false) {
            // App ran for the first time
            // TODO: Here you show the overlay tutorial and ask for permissions.
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("first_time", false);
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) { switch(item.getItemId()) {
        case R.id.about:
            // Start the about activity
            Intent i = new Intent();
            i.setClass(this, AboutActivity.class);
            startActivity(i);
            return(true);

        case R.id.share:
            //Send the app link in playstore for users to share
            Intent i1 = new Intent();
            i1.setAction(Intent.ACTION_SEND);
            i1.setType("text/plain");
            String text = " message you want to share..";
            // change with required  application package

            i1.setPackage("com.whatsapp");
            i1.setPackage("com.facebook.katana");
            if (i1 != null) {
                i1.putExtra(Intent.EXTRA_TEXT, text);//
                startActivity(Intent.createChooser(i1, text));
            } else {

                Toast.makeText(this, "App not found", Toast.LENGTH_SHORT)
                        .show();
            }

    }
        return(super.onOptionsItemSelected(item));
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        // Get the intialised map
        mMap = googleMap;

        // Get the location of the point in which the user clicked
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Store the location on the global variable.
                mMap.clear();
                destination = latLng;
                Toast.makeText(MainActivity.this, "Got location" + latLng.toString(), Toast.LENGTH_SHORT).show();
                mMap.addMarker(new MarkerOptions().position(destination));
                CircleOptions circle = new CircleOptions();
                mMap.addCircle(circle.center(destination).radius(radius.getProgress() * 500).fillColor(Color.parseColor("#1500BFFF")));
            }
        });
    }
}
