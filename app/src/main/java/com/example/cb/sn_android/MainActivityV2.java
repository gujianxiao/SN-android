package com.example.cb.sn_android;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.List;

public class MainActivityV2 extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    private final String TAG="sn-android main";

    private MapView mapView;
    private BaiduMap baiduMap;
    private LocationManager locationManager;
    private String provider;
    private  boolean isFirstLocate = true;
    private LatLng latLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SDKInitializer.initialize(getApplicationContext());


        setContentView(R.layout.activity_main_activity_v2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);




        //Baidu map initialize
        mapView = (MapView) findViewById(R.id.map_view);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //get location provider;
        List<String> providerList = locationManager.getAllProviders();
        Log.d(TAG, "Begin to get provider");
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "provider is GPS");
            provider = LocationManager.GPS_PROVIDER;
            //make provider a GPS provider;
            //send probider to location;
            Location templocation = locationManager.getLastKnownLocation(provider);
            if (templocation == null) {
                if (providerList.contains(LocationManager.PASSIVE_PROVIDER)){
                    Log.d(TAG, "provider is passive");
                    provider = LocationManager.NETWORK_PROVIDER;
                }
                else  {
                    if ((providerList.contains(LocationManager.NETWORK_PROVIDER)))
                    Log.d(TAG, "provider is network");
                    provider = LocationManager.NETWORK_PROVIDER;
                }
            } else {
                Log.d(TAG, "no provider");
                Toast.makeText(this, "No location provider to use", Toast.LENGTH_SHORT).show();
                return;
            }
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                Log.d(TAG, "location!=null");
                Toast.makeText(this, "location!=null", Toast.LENGTH_SHORT).show();
                navigateTo(location);
            } else {
                Log.d(TAG, "location=null， do not go into navigate");
            }
            locationManager.requestLocationUpdates(provider, 5000, 1, locationListener);
        }
    }



    private void navigateTo(Location location){
        Log.d(TAG,"get into navigateTo");
        if(isFirstLocate){
            latLng=new LatLng(location.getLatitude(),location.getLongitude());
            MapStatusUpdate update= MapStatusUpdateFactory.newLatLng(latLng);
            baiduMap.animateMapStatus(update);
            Log.d(TAG,"set zomm to 16");
            update=MapStatusUpdateFactory.zoomTo(16);
            baiduMap.animateMapStatus(update);
            isFirstLocate=false;
        }
        Log.d(TAG,"gonna set my location in the map");
        MyLocationData.Builder locationBuilder =new MyLocationData.Builder();
        locationBuilder.latitude(location.getLatitude());
        locationBuilder.longitude(location.getLongitude());
        MyLocationData locationData=locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }

    LocationListener locationListener= new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(location!=null){
                Log.d(TAG,"gonna get into navigateTo because of location changed");
                navigateTo(location);
            }
        }

        @Override
        public void onStatusChanged(String s, int status
                , Bundle bundle) {
            switch (status) {
                //GPS状态为可见时
                case LocationProvider.AVAILABLE:
                    Log.d(TAG, "GPS on service");
                    break;
                //GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d(TAG, "GPS out of service");
                    break;
                //GPS状态为暂停服务时
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d(TAG, "GPS temporarily not on service");
                    break;
            }

        }





        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };
















    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_v2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

//        unbindService(NDNServiceConnection);
//        unbindService(SensoryServiceConnection);
        baiduMap.setMyLocationEnabled(false);
        mapView.onDestroy();
        if(locationManager!=null){
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }








}
