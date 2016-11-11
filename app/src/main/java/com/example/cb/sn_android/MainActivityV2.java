package com.example.cb.sn_android;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.graphics.Point;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
//import com.baidu.mapapi.model.inner.Point;

import net.named_data.jndn.Data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivityV2 extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,SendWSNInterestTask.Callback {


    private final String TAG="sn-android main";

    private MapView mapView;
    private BaiduMap baiduMap;
    private LocationManager locationManager;
    private String provider;
    private  boolean isFirstLocate = true;

    private Button startNDNService;
    private Button sendInterest;
    private DisplayMetrics dm ;
    private LatLng latLng;
    String serverAddress;

    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();
    public BDLocation BaiduLocation;


    HashMap<Integer,WSNLocation> locationBase;
    HashMap<Integer,Integer> topoBase;


    private int selectedItem=0;
    private InfoWindow mInfoWindow;
    MarkerOptions nodeSet[]=new MarkerOptions[10];


    @Override
    public void updateUI(HashMap<Integer,Data> data) {
        Log.i(TAG, "updateUI: ready to update UI");

        if (data.size()==1) {
            Log.i(TAG, data.get(0).getContent().toString());
            String tempData[] = data.get(0).getContent().toString().split("\\$+");
            String tp1[] = tempData[0].split("/");
            String tp2[];
            if (tempData.length<1) {
                tp2=new String[]{"0","0","0","0","0"};

            }

            else {
                tp2 = tempData[1].split("/");
            }

            if(tp1[1].equals(tp2[1])) {
                Button button = new Button(getApplicationContext());
                button.setBackgroundResource(R.drawable.popup2);
                InfoWindow.OnInfoWindowClickListener listener = null;
                button.setText(tp1[3] + ": " + tp1[4]);


//            button.setTextColor(0x0F0F0F);
//            button.setHintTextColor(0x0F0F0F);
                //       button.setBackgroundColor( 0x666666 );
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
//                marker.setIcon(bd);
                        baiduMap.hideInfoWindow();
                    }
                });
                LatLng ll = nodeSet[selectedItem].getPosition();
                mInfoWindow = new InfoWindow(button, ll, -47);
                baiduMap.showInfoWindow(mInfoWindow);
            }
            else {
                AlertDialog.Builder infoDialog= new AlertDialog.Builder(MainActivityV2.this);
                infoDialog.setTitle("Sensory Data of Selected Node");
                infoDialog.setItems(tempData,null);
                infoDialog.setPositiveButton("OK",null);
                infoDialog.show();
            }

        }


        else if (data.size()>1){
            String info[]=new String[data.size()];
            AlertDialog.Builder infoDialog= new AlertDialog.Builder(MainActivityV2.this);
            infoDialog.setTitle("Sensory Data of Every Node");
            for(int i=0;i<data.size();i++){
                Data tpdata=data.get(i);
                Log.i(TAG, tpdata.getContent().toString());
                String tempData[] = tpdata.getContent().toString().split("\\$+");
//                info[i]=new String();
                info[i]=tempData[0];
//                String tp[] = tempData[0].split("/");
//                Log.i(TAG, "i:"+i+"; tempData[0]:"+tempData[0].toString()+"; tp:"+tp.length);
//                Button button = new Button(getApplicationContext());
//                button.setBackgroundResource(R.drawable.popup2);
//                InfoWindow.OnInfoWindowClickListener listener = null;
//                button.setText(tp[3] + ": " + tp[4]);
////            button.setTextColor(0x0F0F0F);
////            button.setHintTextColor(0x0F0F0F);
//                //       button.setBackgroundColor( 0x666666 );
//                button.setOnClickListener(new View.OnClickListener() {
//                    public void onClick(View v) {
////                marker.setIcon(bd);
//                        baiduMap.hideInfoWindow();
//                    }
//                });
//                String ttp[]=tp[1].split(",");
//
//                //wait for modify...
//                Iterator iterator=locationBase.entrySet().iterator();
//                while(iterator.hasNext()) {
//                    Map.Entry entry = (Map.Entry) iterator.next();
//                    int j=(int)entry.getKey();
//                    Bundle tempB = nodeSet[j].getExtraInfo();
//                    int id[] = tempB.getIntArray("rl");
//                    if (id[1]==Integer.valueOf(ttp[0])&&id[2]==Integer.valueOf(ttp[1])) {
//                        LatLng ll = nodeSet[j].getPosition();
//                        mInfoWindow = new InfoWindow(button, ll, -47);
//                        baiduMap.showInfoWindow(mInfoWindow);
//                        break;
//                    }
//                }


            }
            infoDialog.setItems(info,null);
            infoDialog.setPositiveButton("OK",null);
            infoDialog.show();

        }
        else if(data.size()==0){
            AlertDialog.Builder infoDialog=new AlertDialog.Builder(MainActivityV2.this);
            infoDialog.setTitle("Failed!")
                    .setPositiveButton("OK",null)
                    .setMessage("There is no Data come back from sensor device!")
                    .show();
        }


    }

    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            //Receive Location
            BaiduLocation=location;

            StringBuffer sb = new StringBuffer(256);
            sb.append("time : ");
            sb.append(location.getTime());
            sb.append("\nerror code : ");
            sb.append(location.getLocType());
            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());
            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());
            sb.append("\nradius : ");
            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());// 单位：公里每小时
                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());
                sb.append("\nheight : ");
                sb.append(location.getAltitude());// 单位：米
                sb.append("\ndirection : ");
                sb.append(location.getDirection());// 单位度
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                //运营商信息
                sb.append("\noperationers : ");
                sb.append(location.getOperators());
                sb.append("\ndescribe : ");
                sb.append("网络定位成功");
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            }
            sb.append("\nlocationdescribe : ");
            sb.append(location.getLocationDescribe());// 位置语义化信息
            List<Poi> list = location.getPoiList();// POI数据
            if (list != null) {
                sb.append("\npoilist size = : ");
                sb.append(list.size());
                for (Poi p : list) {
                    sb.append("\npoi= : ");
                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
                }
            }
            Log.i("BaiduLocationApiDem", sb.toString());



            //显示当前位置
            if(BaiduLocation!=null) {
                MyLocationData locData = new MyLocationData.Builder().accuracy(BaiduLocation.getRadius())                // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(100).latitude(BaiduLocation.getLatitude())
                        .longitude(BaiduLocation.getLongitude()).build()

                        ;
// 设置定位数据
                baiduMap.setMyLocationData(locData);
                Log.i(TAG, "set my location on map success!!");
                //设置当前位置为中心

            }
            else {
                Log.i(TAG, "BaiduLocation==null!!");
            }
            if(isFirstLocate){
                if(BaiduLocation!=null) {
                    Log.i(TAG, "This is first locate!");
                    latLng = new LatLng(BaiduLocation.getLatitude(), BaiduLocation.getLongitude());
                    Log.i(TAG, "My location is " + latLng.toString());
                    MapStatusUpdate update=MapStatusUpdateFactory.newLatLng(latLng);
                    Log.i(TAG, "reset map center...");
                    baiduMap.animateMapStatus(update);
//                    baiduMap.setMaxAndMinZoomLevel(20,16);

//                    Log.i(TAG, "reset zomm to 18");
//                    update = MapStatusUpdateFactory.zoomTo(18);
//                    baiduMap.animateMapStatus(update);
                    isFirstLocate = false;
                }
                else {
                    Log.i(TAG, "BaiduLocation is null!");
                }
            }
            else {
                Log.i(TAG, "This is not first locate!");
            }

        }
    }

    //initiate location  mode
    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
//        option.setCoorType("gcj02");
        int span=1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        mLocationClient.setLocOption(option);
    }






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SDKInitializer.initialize(getApplicationContext());
        super.onCreate(savedInstanceState);


        Intent intent=getIntent();
        String userName=intent.getStringExtra("userName");
        Log.i(TAG, "get user name:"+userName);
        serverAddress=intent.getStringExtra("serverAddress");
        Log.i(TAG, "get server address:"+serverAddress);

        setContentView(R.layout.activity_main_activity_v2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);




//       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        // Baidu locate initialize
        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener( myListener );    //注册监听函数
        initLocation();//初始化
        mLocationClient.start();//开始获取服务



        //Baidu map initialize
        mapView = (MapView) findViewById(R.id.map_view);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);





// 设置定位图层的配置（定位模式，是否允许方向信息，用户自定义定位图标）
//        mCurrentMarker = BitmapDescriptorFactory
//                .fromResource(R.drawable.icon_geo);
//        MyLocationConfiguration config = new MyLocationConfiguration(mCurrentMode, true, mCurrentMarker);
//        mBaiduMap.setMyLocationConfiguration();


//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        //get location provider;
//        List<String> providerList = locationManager.getAllProviders();
//        Log.d(TAG, "Begin to get provider");
//        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
//            Log.d(TAG, "provider is GPS");
//            provider = LocationManager.GPS_PROVIDER;
//            //make provider a GPS provider;
//            //send probider to location;
//            Location templocation = locationManager.getLastKnownLocation(provider);
//            if (templocation == null) {
//                if (providerList.contains(LocationManager.PASSIVE_PROVIDER)){
//                    Log.d(TAG, "provider is passive");
//                    provider = LocationManager.NETWORK_PROVIDER;
//                }
//                else  {
//                    if ((providerList.contains(LocationManager.NETWORK_PROVIDER)))
//                    Log.d(TAG, "provider is network");
//                    provider = LocationManager.NETWORK_PROVIDER;
//                }
//            } else {
//                Log.d(TAG, "no provider");
//                Toast.makeText(this, "No location provider to use", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            Location location = locationManager.getLastKnownLocation(provider);
//            if (location != null) {
//                Log.d(TAG, "location!=null");
//                Toast.makeText(this, "location!=null", Toast.LENGTH_SHORT).show();
//                navigateTo(location);
//            } else {
//                Log.d(TAG, "location=null， do not go into navigate");
//            }
//            locationManager.requestLocationUpdates(provider, 5000, 1, locationListener);
//        }



//        //bind service
//        startNDNService=(Button)findViewById(R.id.start_ndn_service);
//        sendInterest=(Button)findViewById(R.id.send_Interest);
//        startNDNService.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent startNDNIntent = new Intent (MainActivityV2.this, NDN_service.class);
//                bindService(startNDNIntent,NDNServiceConnection,BIND_AUTO_CREATE);
//                startService(startNDNIntent);
//                Log.i(TAG, "start NDN service");
//
//            }
//        });
//        sendInterest.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                //send interest service here;
//                Intent startSensoryIntent=new Intent(MainActivityV2.this,Sensory_service.class);
//                bindService(startSensoryIntent,SensoryServiceConnection,BIND_AUTO_CREATE);
//                startService(startSensoryIntent);
//                Log.i(TAG, "start sensory service");
//            }
//        });



    }



//    private void navigateTo(Location location){
//        Log.d(TAG,"get into navigateTo");
//        if(isFirstLocate){
//            latLng=new LatLng(location.getLatitude(),location.getLongitude());
//            MapStatusUpdate update= MapStatusUpdateFactory.newLatLng(latLng);
//            baiduMap.animateMapStatus(update);
//            Log.d(TAG,"set zomm to 16");
//            update=MapStatusUpdateFactory.zoomTo(16);
//            baiduMap.animateMapStatus(update);
//            isFirstLocate=false;
//        }
//        Log.d(TAG,"gonna set my location in the map");
//        MyLocationData.Builder locationBuilder =new MyLocationData.Builder();
//        locationBuilder.latitude(location.getLatitude());
//        locationBuilder.longitude(location.getLongitude());
//        MyLocationData locationData=locationBuilder.build();
//        baiduMap.setMyLocationData(locationData);
//    }

    LocationListener locationListener= new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(location!=null){
                Log.d(TAG,"gonna get into navigateTo because of location changed");
//                navigateTo(location);
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














//menu control

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
            Intent startNDNIntent = new Intent (MainActivityV2.this, NDN_service.class);
            bindService(startNDNIntent,NDNServiceConnection,BIND_AUTO_CREATE);
            startService(startNDNIntent);
            Log.i(TAG, "start NDN service");

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void initiateLocation(HashMap<Integer,WSNLocation> Base){
        Log.i(TAG, "initiateLocation: begining to set node in the map...");
//        MarkerOptions nodeSet[]=new MarkerOptions[10];
//        int i=0;
        Iterator iterator=Base.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            WSNLocation tempWSNLocationEntry =(WSNLocation) entry.getValue();
            double latitude=Double.valueOf("39.96"+ tempWSNLocationEntry.getLatitude());
            double longitude=Double.valueOf("116.36"+ tempWSNLocationEntry.getLongitude());
            Log.i(TAG, "nodeSet["+entry.getKey()+"] start initiate...");
            int i=(int)entry.getKey();
            nodeSet[i] = new MarkerOptions();
            BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);
            nodeSet[i].position(new LatLng(latitude,longitude)).icon(bitmap);
            //set marker option extro info of relative position
            Bundle tempBundle=new Bundle();
            int tempD[]={(int)entry.getKey(), tempWSNLocationEntry.getLatitude(), tempWSNLocationEntry.getLongitude()};
            tempBundle.putIntArray("rl",tempD);
            nodeSet[i].extraInfo(tempBundle);
            baiduMap.addOverlay(nodeSet[i]);
            baiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(20).build()));
        }


//        for(int i=0;i<Base.size();i++){
//            double latitude=Double.valueOf("39.9663"+Base.get(i).getLatitude());
//            double longitude=Double.valueOf("116.3630"+Base.get(i).getLatitude());
//            nodeSet[i].position(new LatLng(latitude,longitude));
//            baiduMap.addOverlay(nodeSet[i]);
//        }
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(final Marker arg0) {
                // TODO Auto-generated method stub
                //send Interest of a node
                final String[] Items={"Temperature","Illumination","humidity"};
                new AlertDialog.Builder(MainActivityV2.this)
                        .setTitle("Interest type:")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setSingleChoiceItems(Items, -1, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        /*int index=0;
                                        Log.i(TAG, "click send interest");
                                        if (which >= 0)
                                        {
                                            Log.i(TAG, "choose item!");

                                            //如果单击的是列表项，将当前列表项的索引保存在index中。

                                            //如果想单击列表项后关闭对话框，可在此处调用dialog.cancel()

                                            //或是用dialog.dismiss()方法。

                                            index = which;

                                        }*/
//                                        if(which==DialogInterface.BUTTON_NEUTRAL){
                                        //dialog.dismiss();
                                        if (which == 0) {
                                            Bundle tempB = arg0.getExtraInfo();
                                            int id[] = tempB.getIntArray("rl");
                                            selectedItem=id[0];
                                            Log.i(TAG, "1_onClick: type is " + Items[which] + ". ID is " + id[0] + "Relative position is:" + id[1] + "," + id[2]);
                                            HashMap<Integer, WSNLocation> tempHashMap = new HashMap<Integer, WSNLocation>();
                                            tempHashMap.put(id[0], new WSNLocation(id[1], id[2],"temp"));
                                            new SendWSNInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);


                                        } else if (which == 1) {
                                            Bundle tempB = arg0.getExtraInfo();
                                            int id[] = tempB.getIntArray("rl");
                                            selectedItem=id[0];
                                            Log.i(TAG, "2_onClick: type is " + Items[which] + ". ID is " + id[0] + "Relative position is:" + id[1] + "," + id[2]);
                                            HashMap<Integer, WSNLocation> tempHashMap = new HashMap<Integer, WSNLocation>();
                                            tempHashMap.put(id[0], new WSNLocation(id[1], id[2],"light"));
                                            new SendWSNInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);


                                        } else if (which == 2) {
                                            Bundle tempB = arg0.getExtraInfo();
                                            int id[] = tempB.getIntArray("rl");
                                            selectedItem=id[0];
                                            Log.i(TAG, "3_onClick: type is " + Items[which] + ". ID is " + id[0] + "Relative position is:" + id[1] + "," + id[2]);
                                            HashMap<Integer, WSNLocation> tempHashMap = new HashMap<Integer, WSNLocation>();
                                            tempHashMap.put(id[0], new WSNLocation(id[1], id[2],"humidity"));
//                                                new SendInterestTask().execute(tempHashMap);
                                            new SendWSNInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);
                                        } else {
                                            Log.i(TAG, "onClick: type is null");
                                        }
                                        dialog.dismiss();
                                    }
                                        /*else if (which==DialogInterface.BUTTON_NEGATIVE){
                                            Toast.makeText(MainActivityV2.this, "You choose nothing! Try again.",

                                                    Toast.LENGTH_LONG);
                                        }
*/
//                                    }
                                }

                        )
                        .setNegativeButton("cancel", null)
//                        .setPositiveButton("OK", null)
                        .show();
                //Toast.makeText(getApplicationContext(), "Marker被点击了！", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

    }

    public void initiateTopo(HashMap<Integer,Integer> Base){
        //show topo here waiting for finishing later        ...
    }
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private LatLng getSite(int type) {

        dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int statusBarHeight = getStatusBarHeight();

        Point pt = new Point();
        if (type == 1) {
            //左上角
            pt.x = 0;
            pt.y = statusBarHeight+60;
        }
        if (type == 2) {
            //左下角
            pt.x = 0;
            pt.y = dm.heightPixels;
        }
        if (type == 3) {
            //右上角
            pt.x = dm.widthPixels;
            pt.y = statusBarHeight+60;
        }
        if (type == 4) {
            //右下角
            pt.x = dm.widthPixels;
            pt.y = dm.heightPixels;
        }
        LatLng ll;
        ll = baiduMap.getProjection().fromScreenLocation(pt);

        return ll;

    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_node) {
            // refresh the node location
        } else if (id == R.id.nav_topo) {
            //show topo on the map

        } else if (id == R.id.nav_getAll) {
            //send Interest of view
            final String[] Items={"Temperature","Illumination","humidity"};
            new AlertDialog.Builder(MainActivityV2.this)
                    .setTitle("Interest type:")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setSingleChoiceItems(Items, -1, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                        /*int index=0;
                                        Log.i(TAG, "click send interest");
                                        if (which >= 0)
                                        {
                                            Log.i(TAG, "choose item!");

                                            //如果单击的是列表项，将当前列表项的索引保存在index中。

                                            //如果想单击列表项后关闭对话框，可在此处调用dialog.cancel()

                                            //或是用dialog.dismiss()方法。

                                            index = which;

                                        }*/
//                                        if(which==DialogInterface.BUTTON_NEUTRAL){
                                    //dialog.dismiss();
                                    if (which == 0) {
//                                        Bundle tempB = arg0.getExtraInfo();
//                                        int id[] = tempB.getIntArray("rl");
//                                        selectedItem=id[0];
//                                        Log.i(TAG, "1_onClick: type is " + Items[which] + ". ID is " + id[0] + "Relative position is:" + id[1] + "," + id[2]);
                                        HashMap<Integer, WSNLocation> tempHashMap = locationBase;
//                                        tempHashMap.put(id[0], new WSNLocation(id[1], id[2],"temp"));
                                        Iterator iterator=tempHashMap.entrySet().iterator();
                                        while(iterator.hasNext()){
                                            Map.Entry entry = (Map.Entry) iterator.next();
                                            WSNLocation tempWSNLocation=(WSNLocation)entry.getValue();
                                            tempWSNLocation.setDataType("temp");
                                            entry.setValue(tempWSNLocation);
                                        }

                                        new SendWSNInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);


                                    } else if (which == 1) {
//                                        Bundle tempB = arg0.getExtraInfo();
//                                        int id[] = tempB.getIntArray("rl");
//                                        selectedItem=id[0];
//                                        Log.i(TAG, "2_onClick: type is " + Items[which] + ". ID is " + id[0] + "Relative position is:" + id[1] + "," + id[2]);
//                                        HashMap<Integer, WSNLocation> tempHashMap = new HashMap<Integer, WSNLocation>();
//                                        tempHashMap.put(id[0], new WSNLocation(id[1], id[2],"light"));
//                                        new SendInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);
//
                                        HashMap<Integer, WSNLocation> tempHashMap = locationBase;
//                                        tempHashMap.put(id[0], new WSNLocation(id[1], id[2],"temp"));
                                        Iterator iterator=tempHashMap.entrySet().iterator();
                                        while(iterator.hasNext()){
                                            Map.Entry entry = (Map.Entry) iterator.next();
                                            WSNLocation tempWSNLocation=(WSNLocation)entry.getValue();
                                            tempWSNLocation.setDataType("light");
                                            entry.setValue(tempWSNLocation);
                                        }
                                        new SendWSNInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);

                                    } else if (which == 2) {
//                                        Bundle tempB = arg0.getExtraInfo();
//                                        int id[] = tempB.getIntArray("rl");
//                                        selectedItem=id[0];
//                                        Log.i(TAG, "3_onClick: type is " + Items[which] + ". ID is " + id[0] + "Relative position is:" + id[1] + "," + id[2]);
//                                        HashMap<Integer, WSNLocation> tempHashMap = new HashMap<Integer, WSNLocation>();
//                                        tempHashMap.put(id[0], new WSNLocation(id[1], id[2],"humidity"));
////                                                new SendInterestTask().execute(tempHashMap);
//                                        new SendInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);

                                        HashMap<Integer, WSNLocation> tempHashMap = locationBase;
//                                        tempHashMap.put(id[0], new WSNLocation(id[1], id[2],"temp"));
                                        Iterator iterator=tempHashMap.entrySet().iterator();
                                        while(iterator.hasNext()){
                                            Map.Entry entry = (Map.Entry) iterator.next();
                                            WSNLocation tempWSNLocation=(WSNLocation)entry.getValue();
                                            tempWSNLocation.setDataType("humidity");
                                            entry.setValue(tempWSNLocation);
                                        }
                                        new SendWSNInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);
                                    } else {
                                        Log.i(TAG, "onClick: type is null");
                                    }
                                    dialog.dismiss();
                                }
                                        /*else if (which==DialogInterface.BUTTON_NEGATIVE){
                                            Toast.makeText(MainActivityV2.this, "You choose nothing! Try again.",

                                                    Toast.LENGTH_LONG);
                                        }
*/
//                                    }
                            }

                    )
                    .setNegativeButton("cancel", null)
//                        .setPositiveButton("OK", null)
                    .show();

        } else if (id == R.id.nav_getPoint) {
            final String[] Items={"Temperature","Illumination","humidity"};
            new AlertDialog.Builder(MainActivityV2.this)
                    .setTitle("Interest type:")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setSingleChoiceItems(Items, -1, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                        /*int index=0;
                                        Log.i(TAG, "click send interest");
                                        if (which >= 0)
                                        {
                                            Log.i(TAG, "choose item!");

                                            //如果单击的是列表项，将当前列表项的索引保存在index中。

                                            //如果想单击列表项后关闭对话框，可在此处调用dialog.cancel()

                                            //或是用dialog.dismiss()方法。

                                            index = which;

                                        }*/
//                                        if(which==DialogInterface.BUTTON_NEUTRAL){
                                    //dialog.dismiss();
                                    if (which == 0) {
//                                        Bundle tempB = arg0.getExtraInfo();
//                                        int id[] = tempB.getIntArray("rl");
//                                        selectedItem=id[0];
//                                        Log.i(TAG, "1_onClick: type is " + Items[which] + ". ID is " + id[0] + "Relative position is:" + id[1] + "," + id[2]);
                                        HashMap<Integer, WSNLocation> tempHashMap =new HashMap<>();
                                        LatLng leftDown=getSite(1);
                                        String tpLDLat[]=Double.toString(leftDown.latitude).split("\\.");
                                        String tpLDLng[]=Double.toString(leftDown.longitude).split("\\.");
                                        LatLng rightUp=getSite(4);
                                        String tpRULat[]=Double.toString(rightUp.latitude).split("\\.");
                                        String tpRULng[]=Double.toString(rightUp.longitude).split("\\.");
                                        String dataType="temp";
                                        WSNLocation tempWSNArea=new WSNLocation(Integer.valueOf(tpLDLat[1].substring(2,6)),
                                                Integer.valueOf(tpLDLng[1].substring(2,6)),
                                                Integer.valueOf(tpRULat[1].substring(2,6)),
                                                Integer.valueOf(tpRULng[1].substring(2,6)),
                                                dataType);
                                        //HashMap<Integer,WSNLocation> tempHashMap=new HashMap();
                                        tempHashMap.put(1000,tempWSNArea);

                                        new SendWSNInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);


                                    } else if (which == 1) {
//                                        Bundle tempB = arg0.getExtraInfo();
//                                        int id[] = tempB.getIntArray("rl");
//                                        selectedItem=id[0];
//                                        Log.i(TAG, "2_onClick: type is " + Items[which] + ". ID is " + id[0] + "Relative position is:" + id[1] + "," + id[2]);
//                                        HashMap<Integer, WSNLocation> tempHashMap = new HashMap<Integer, WSNLocation>();
//                                        tempHashMap.put(id[0], new WSNLocation(id[1], id[2],"light"));
//                                        new SendInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);
//
                                        HashMap<Integer, WSNLocation> tempHashMap =new HashMap<>();
                                        LatLng leftDown=getSite(1);
                                        String tpLDLat[]=Double.toString(leftDown.latitude).split("\\.");
                                        String tpLDLng[]=Double.toString(leftDown.longitude).split("\\.");
                                        LatLng rightUp=getSite(4);
                                        String tpRULat[]=Double.toString(rightUp.latitude).split("\\.");
                                        String tpRULng[]=Double.toString(rightUp.longitude).split("\\.");
                                        String dataType="light";
                                        WSNLocation tempWSNArea=new WSNLocation(Integer.valueOf(tpLDLat[1].substring(2,6)),
                                                Integer.valueOf(tpLDLng[1].substring(2,6)),
                                                Integer.valueOf(tpRULat[1].substring(2,6)),
                                                Integer.valueOf(tpRULng[1].substring(2,6)),
                                                dataType);
                                        //HashMap<Integer,WSNLocation> tempHashMap=new HashMap();
                                        tempHashMap.put(1000,tempWSNArea);

                                        new SendWSNInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);

                                    } else if (which == 2) {
//                                        Bundle tempB = arg0.getExtraInfo();
//                                        int id[] = tempB.getIntArray("rl");
//                                        selectedItem=id[0];
//                                        Log.i(TAG, "3_onClick: type is " + Items[which] + ". ID is " + id[0] + "Relative position is:" + id[1] + "," + id[2]);
//                                        HashMap<Integer, WSNLocation> tempHashMap = new HashMap<Integer, WSNLocation>();
//                                        tempHashMap.put(id[0], new WSNLocation(id[1], id[2],"humidity"));
////                                                new SendInterestTask().execute(tempHashMap);
//                                        new SendInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);

                                        HashMap<Integer, WSNLocation> tempHashMap =new HashMap<>();
                                        LatLng leftDown=getSite(1);
                                        String tpLDLat[]=Double.toString(leftDown.latitude).split("\\.");
                                        String tpLDLng[]=Double.toString(leftDown.longitude).split("\\.");
                                        LatLng rightUp=getSite(4);
                                        String tpRULat[]=Double.toString(rightUp.latitude).split("\\.");
                                        String tpRULng[]=Double.toString(rightUp.longitude).split("\\.");
                                        String dataType="humidity";
                                        WSNLocation tempWSNArea=new WSNLocation(Integer.valueOf(tpLDLat[1].substring(2,6)),
                                                Integer.valueOf(tpLDLng[1].substring(2,6)),
                                                Integer.valueOf(tpRULat[1].substring(2,6)),
                                                Integer.valueOf(tpRULng[1].substring(2,6)),
                                                dataType);
                                        //HashMap<Integer,WSNLocation> tempHashMap=new HashMap();
                                        tempHashMap.put(1000,tempWSNArea);

                                        new SendWSNInterestTask(MainActivityV2.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,tempHashMap);
                                    } else {
                                        Log.i(TAG, "onClick: type is null");
                                    }
                                    dialog.dismiss();
                                }
                                        /*else if (which==DialogInterface.BUTTON_NEGATIVE){
                                            Toast.makeText(MainActivityV2.this, "You choose nothing! Try again.",

                                                    Toast.LENGTH_LONG);
                                        }
*/
//                                    }
                            }

                    )
                    .setNegativeButton("cancel", null)
//                        .setPositiveButton("OK", null)
                    .show();

        } else if (id == R.id.nav_history) {
            // show the history  sensory data

        } else if (id == R.id.nav_tendency) {
            // show the tendency of sensory data

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
        mLocationClient.stop();
        baiduMap.setMyLocationEnabled(false);
        mapView.onDestroy();
//        if(locationManager!=null){
//            locationManager.removeUpdates(locationListener);
//        }
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


    //bind NDN service with main activity;
    private NDN_service.ServiceBinder serviceBinder;

    private ServiceConnection NDNServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            serviceBinder=(NDN_service.ServiceBinder)iBinder;
            boolean tagOfBind;
            int tryTime=0;

            do{
                tagOfBind=serviceBinder.startBind(latLng,serverAddress);
                try {
                    Thread.sleep(1000);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                tryTime++;

            } while(tagOfBind!=true&&tryTime<10);
            if (tryTime>=10){
                Log.i(TAG, "onServiceConnected: failed... try to tart app again!");
                return ;
            }
            Log.i(TAG, "NDN service initiate successful but wait for updata UI in main activity...");
            locationBase = serviceBinder.getLocationBase();
            Log.i(TAG, "get locationBase from NDN service size is "+locationBase.size());
            topoBase = serviceBinder.getTopoBase();
            Log.i(TAG, "get topoBase from NDN service size is "+topoBase.size());
            initiateLocation(locationBase);
            initiateTopo(topoBase);
            Log.i(TAG, "update UI success in mian acitivity!");
        }
    };

    //bidn sensory service with main activity;
    private Sensory_service.ServiceBinder SensoryServiceBinder;
    private ServiceConnection SensoryServiceConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            SensoryServiceBinder=(Sensory_service.ServiceBinder)iBinder;
            SensoryServiceBinder.startBind();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {


        }
    };










}
