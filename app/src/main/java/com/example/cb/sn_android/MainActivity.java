package com.example.cb.sn_android;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.List;

public class MainActivity extends AppCompatActivity{
    private final String TAG="sn-android main";

    private MapView mapView;
    private BaiduMap baiduMap;
    private LocationManager locationManager;
    private String provider;
    private  boolean isFirstLocate = true;
    private Button startNDNService;
    private Button sendInterest;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        startNDNService=(Button)findViewById(R.id.start_ndn_service);
        sendInterest=(Button)findViewById(R.id.send_Interest);
        startNDNService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startIntent = new Intent (MainActivity.this, NDN_service.class);
                bindService(startIntent,NDNServiceConnection,BIND_AUTO_CREATE);
                startService(startIntent);
                Log.i(TAG, "start NDN service");
            }
        });
        sendInterest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //send interest service here;
            }
        });


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
                if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
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
            LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
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


    private NDN_service.ServiceBinder serviceBinder;

    private ServiceConnection NDNServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            serviceBinder=(NDN_service.ServiceBinder)iBinder;
            serviceBinder.startBind();
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
