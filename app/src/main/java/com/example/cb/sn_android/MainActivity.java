package com.example.cb.sn_android;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private BaiduMap baiduMap;
    private LocationManager locationManager;
    private String provider;
    private  boolean isFirstLocate = true;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.map_view);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //get location provider;
        List<String> providerList = locationManager.getAllProviders();
        Log.d("sn-android", "Begin to get provider");
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            Log.d("sn-android", "provider is GPS");
            provider = LocationManager.GPS_PROVIDER;
            //make provider a GPS provider;
            //send probider to location;
            Location templocation = locationManager.getLastKnownLocation(provider);
            if (templocation == null) {
                if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
                    Log.d("sn-android", "provider is network");
                    provider = LocationManager.NETWORK_PROVIDER;
                }
            } else {
                Log.d("sn-android", "no provider");
                Toast.makeText(this, "No location provider to use", Toast.LENGTH_SHORT).show();
                return;
            }
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                Log.d("sn-android", "location!=null");
                Toast.makeText(this, "location!=null", Toast.LENGTH_SHORT).show();
                navigateTo(location);
            } else {
                Log.d("sn-android", "location=null， do not go into navigate");
            }
            locationManager.requestLocationUpdates(provider, 5000, 1, locationListener);
        }
    }

    private void navigateTo(Location location){
        Log.d("sn-android","get into navigateTo");
        if(isFirstLocate){
            LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
            MapStatusUpdate update= MapStatusUpdateFactory.newLatLng(latLng);
            baiduMap.animateMapStatus(update);
            Log.d("sn-android","set zomm to 16");
            update=MapStatusUpdateFactory.zoomTo(16);
            baiduMap.animateMapStatus(update);
            isFirstLocate=false;
        }
        Log.d("sn-android","gonna set my location in the map");
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
                Log.d("sn-android","gonna get into navigateTo because of location changed");
                navigateTo(location);
            }
        }

        @Override
        public void onStatusChanged(String s, int status
                , Bundle bundle) {
            switch (status) {
                //GPS状态为可见时
                case LocationProvider.AVAILABLE:
                    Log.d("sn-android", "GPS on service");
                    break;
                //GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d("sn-android", "GPS out of service");
                    break;
                //GPS状态为暂停服务时
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d("sn-android", "GPS temporarily not on service");
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
    protected void onDestroy() {
        super.onDestroy();
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
