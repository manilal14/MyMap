package com.example.mani.mymap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.mani.mymap.CommonVariablesAndFunctions.hideSoftKeyboard;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final String TAG = "MapsActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final float SEARCH_ZOOM = 20f;

    LatLng mOrgin = null;
    LatLng mDestination = null;

    //vars
    private GoogleMap mMap;
    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    MarkerOptions mMarkerOptions;

    //Widgets
    AutoCompleteTextView mSearchView;
    ImageView mGps;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Log.d(TAG,"OnCreate");

        mMarkerOptions = new MarkerOptions();

        mSearchView = findViewById(R.id.search_auto_complete);
        mGps        = findViewById(R.id.gps);

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });

        mSearchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                hideSoftKeyboard(MapsActivity.this);
                if(mLocationPermissionsGranted)
                    geoLocate();
                else
                    getLocationPermission();
                return true;
            }
        });

        // get location of mobile and move camera to it.
        getLocationPermission();

    }

    private void initMap(){
        Log.d(TAG, "initMap");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG,"OnMapReady");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {

            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setMapToolbarEnabled(false);

            //init();
        }

    }

    private void geoLocate()
    {
        Log.d(TAG,"geoLocate");

        String searchString = mSearchView.getText().toString().trim();

        Geocoder geocoder = new Geocoder(MapsActivity.this);

        List<Address> addressList = new ArrayList<>();

        try {
            addressList = geocoder.getFromLocationName(searchString,1);

        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage() );
        }

        if(addressList.size() == 0){
            Toast.makeText(MapsActivity.this,"No Location Found",Toast.LENGTH_SHORT).show();
            return;
        }

        Address address = addressList.get(0);
        mDestination = new LatLng(address.getLatitude(),address.getLongitude());

        Log.d(TAG, "geoLocate: found a location: " + address.toString());


        moveCamera(new LatLng(address.getLatitude(),address.getLongitude()),
                DEFAULT_ZOOM,address.getAddressLine(0));

    }



    /*-------------------------Supporting Functions  ----------------------------------*/

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission");

        String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }

            else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }

        else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Get device current location and move the camera to the currrent location
    private void getDeviceLocation()
    {
        Log.d(TAG, "getDeviceLocation and move the camera to location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mLocationPermissionsGranted){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            //Gehhting the current location
                            mOrgin = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());

                            moveCamera(new LatLng( currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM,
                                    "Origin");

                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.d(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    // What will happen after providing permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:
                {
                    if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    initMap();
                }
            }
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title){

        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        mMarkerOptions = mMarkerOptions.position(latLng).title(title);
        mMap.addMarker(mMarkerOptions);

        //hideSoftKeyboard();
    }



}
