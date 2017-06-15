package com.example.cb.sn_android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.telecom.GatewayInfo;
import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.google.protobuf.ByteString;

import net.named_data.jndn.Data;

import net.named_data.jndn.Face;

import net.named_data.jndn.Interest;

import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;

import net.named_data.jndn.OnData;

import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.ProtobufTlvOfCB;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.KeyType;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;
import net.named_data.jndn.tests.ControlParametersProto;
import net.named_data.jndn.tests.FaceQueryFilterProto;
import net.named_data.jndn.tests.FaceStatusProto;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SegmentFetcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by cb on 16-9-26.NDN_Servie class provide NDN service for SN-android.
 * Including register to the IOT gateway, send/receive Interest and receive/send Data.
 */
public class NDN_service extends Service{
    private static final String TAG = "ndn-service";
    private Face face;
    String registerUri;
    Name deviceName;
    WiFiLocation gatewayArea;
    String gatewayAreaString;
    incomingData incomD = new incomingData();
    public int callbackCount= 0;
    private  int registerSignal=1;
    private LatLng deviceLatLng;
    private String userID;
    private ServiceBinder serviceBinder=new ServiceBinder();
    private String HOST;
    private int PORT=6353;


    private MapView mapView;
    private BaiduMap baiduMap;
    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();
    public BDLocation BaiduLocation;



    private SensorManager sensorManager;
    private Sensor light;
    private Sensor temperature;
    private Sensor accelerometer;
    private String sensorType;
    private String sensorValueOfTemp;
    private String sensorValueOfLight;
    private String sensorValueOfAcceler;
    //    private SensorEventListener listener;
    private SensorEventListener listener=new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float value=sensorEvent.values[0];
            if (sensorType.equals("temp")) {
                Log.i(TAG, "onSensorChanged: type is " + sensorType + ", value is:" + value);
                sensorValueOfTemp = Float.toString(value);
            }
            else if (sensorType.equals("light")){
                Log.i(TAG, "onSensorChanged: type is " + sensorType + ", value is:" + value);
                sensorValueOfLight = Float.toString(value);
            }
            else if (sensorType.equals("acceler")){
                Log.i(TAG, "onSensorChanged: type is " + sensorType + ", value is:" + value);
                sensorValueOfAcceler = "x:"+Float.toString(sensorEvent.values[0]);
                sensorValueOfAcceler+=" y:"+Float.toString(sensorEvent.values[1]);
                sensorValueOfAcceler+=" z:"+Float.toString(sensorEvent.values[2]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };


    HashMap<Integer,Integer> topoBase=new HashMap<>();
    HashMap<Integer,Integer> topoBaseMobile=new HashMap<>();
    //initiate topo into a hashmap
    public void initiateTopo(String msg){
        Log.i(TAG, "initiateTopo...");
        String topoSet[]=msg.split("\\$+");
        for(String topo:topoSet){
            String temp[]=topo.split("->");
            topoBase.put(Integer.parseInt(temp[0]),Integer.parseInt(temp[1]));
            Log.i(TAG, "update topo base "+Integer.parseInt(temp[0])+" to "+topoBase.get(Integer.parseInt(temp[0])));
        }
    }
    //initiate topo of mobile into topobaseMobile
    public void initiateTopoMobile(String msg){
        Log.i(TAG, "initiateTopoMobile...");
        String topoSet[]=msg.split("\\$+");
        for(String topo:topoSet){
            String temp[]=topo.split("->");
            topoBaseMobile.put(Integer.parseInt(temp[0]),Integer.parseInt(temp[1]));
            Log.i(TAG, "update topo base mobile "+Integer.parseInt(temp[0])+" to "+topoBaseMobile.get(Integer.parseInt(temp[0])));
        }
    }

    HashMap<Integer,WSNLocation> locationBase=new HashMap<>();
    HashMap<Integer,WiFiLocation> locationBaseMobile=new HashMap<>();
    //initiate location into a hashmap
    public void initiateLocation(String msg){
        Log.i(TAG, "initiateLocation...");
        String locationSet[]=msg.split("\\$+");

        for(String location:locationSet){
            String temp[]=location.split("->|\\(|\\)|,");
            WSNLocation tempWSNLocation =new WSNLocation(Integer.parseInt(temp[3]),Integer.parseInt(temp[2]));
            locationBase.put(Integer.parseInt(temp[0]), tempWSNLocation);
            Log.i(TAG, "update location base "+String.valueOf(temp[0])+" to "+locationBase.get(Integer.parseInt(temp[0])).getLatitude()+","+locationBase.get(Integer.parseInt(temp[0])).getLongitude());
        }
    }
    //initiate locationMobile into locationbaseMobile
    public void initiateLocationMobile(String msg){
        Log.i(TAG, "initiateLocationMobile...");
        String locationSet[]=msg.split("\\$+");

        for(String location:locationSet){
            String temp[]=location.split("->|\\(|\\)|,");
            //waiting for modifying to dealing lat lng and node ID problem...
            WiFiLocation tempWiFiLocation =new WiFiLocation(new LatLng(Double.valueOf(temp[3]),Double.valueOf(temp[2])),temp[0]);
            locationBaseMobile.put(Integer.parseInt(temp[0]), tempWiFiLocation);
            Log.i(TAG, "update location base "+String.valueOf(temp[0])+" to "+locationBaseMobile.get(Integer.parseInt(temp[0])).getPoint().latitude+","+locationBaseMobile.get(Integer.parseInt(temp[0])).getPoint().longitude);
        }
    }






    // dealing with come back data and time out
    private class incomingData implements OnData,OnTimeout{

        @Override
        public void onData(Interest interest, Data data) {
            callbackCount++;
            Log.i(TAG, "Got data packet with name:" + data.getName().toUri());
            String msg=data.getContent().toString();
            if(msg!=null){
                Log.i(TAG, "onData: "+msg);
                String tempName= data.getName().toString();
                if (tempName.contains("/wsn/topo")) {
                    initiateTopo(msg);
                }
                else if (tempName.contains("/wsn/location")){
                    initiateLocation(msg);
                }
                else if(tempName.contains("/wifi/topo")){
                    //initiateTopoMobile;
                    //maybe bugs
                    initiateTopoMobile(msg);
                }
                else if(tempName.contains("/wifi/location")){
                    //initiateLocationMobile;
                    //maybe bugss
                    initiateLocationMobile(msg);
                }
                else {
                    if (tempName.contains("NDN-IOT-REGISTER")){
                        registerSignal=0;
                        String tempArea[]=data.getContent().toString().split("/");
                        LatLng tpLeftDown=new LatLng(Double.valueOf(tempArea[2]),Double.valueOf(tempArea[1]));
                        LatLng tpRightUp=new LatLng(Double.valueOf(tempArea[4]),Double.valueOf(tempArea[3]));
                        gatewayArea=new WiFiLocation(tpLeftDown,tpRightUp,"Gateway");
                        gatewayAreaString=new String(tempArea[1]+"/"+tempArea[2]+"/"+tempArea[3]+"/"+tempArea[4]);
                    }
                    Log.i(TAG, "this Data is not topo or location");
                }


//                registerSignal=1;
            }

        }

        @Override
        public void onTimeout(Interest interest) {
            callbackCount++;
            Log.i(TAG, "Time out for interest:" + interest.getName().toUri());
            //Log.i(TAG, "register in gateway failed!");
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
 //           Log.i("BaiduLocationApiDem", sb.toString());



            //显示当前位置
            if(BaiduLocation!=null) {
                MyLocationData locData = new MyLocationData.Builder().accuracy(BaiduLocation.getRadius())                // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(100).latitude(BaiduLocation.getLatitude())
                        .longitude(BaiduLocation.getLongitude()).build()

                        ;
// 设置定位数据
//                baiduMap.setMyLocationData(locData);
                //       mMapView.refresh();
 //               Log.i(TAG, "get my location success!!");
                //设置当前位置为中心
            }
            else {
                Log.i(TAG, "BaiduLocation==null!!");
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

//    public Boolean isBelongto(WiFiLocation location,WiFiLocation area){
//        if (location.getLeftDown().latitude>=area.getLeftDown().latitude&&location.getLeftDown().longitude>=area.getLeftDown().longitude
//             &&location.getRightUp().latitude<=area.getRightUp().latitude&&location.getRightUp().longitude<=area.getRightUp().longitude
//                )
//        {
//            return true;
//        }
//        else
//            return false;
//    }



    //dealing with incoming Interest and package the data then send back to gateway
    private  class incomingInterest implements OnInterestCallback{
        @Override
        public void onInterest(Name name, Interest interest, Face face, long l, InterestFilter interestFilter) {
            Log.i(TAG, "Get interest:" + interest.getName().toString());
            /*
            Dealing with interest and package the data here...
             */
            //get location of device for every incoming interest

// judge if device in the area interest ask for
 //           LatLng tempLatLng = new LatLng(BaiduLocation.getLatitude(), BaiduLocation.getLongitude());
            String temp[]=interest.getName().toString().split("/");
            if (BaiduLocation==null){
                Log.e(TAG, "onInterest: BaiduLocation located failed!" );
                return;
            }
            else {
                Log.i(TAG, "onInterest: BaiduLocation not null, start to match...");
//                String tpLeftDown[]= {temp[2],temp[3]};
//                String tpRightUp[]={temp[4],temp[5]};
//                if (!(Double.valueOf(temp[2])<=BaiduLocation.getLongitude()&&Double.valueOf(temp[3])<=BaiduLocation.getLatitude()
//                    &&Double.valueOf(temp[4])>=BaiduLocation.getLongitude()&&Double.valueOf(temp[5])>=BaiduLocation.getLatitude()
//                        ))
                //modify if you want to set fixed point for experiments
                if (!(Double.valueOf(temp[2])<=116.363055&&Double.valueOf(temp[3])<=39.966977
                            &&Double.valueOf(temp[4])>=116.363055&&Double.valueOf(temp[5])>=39.966977
                ))
                {
                    Log.i(TAG, "onInterest: not matching in mobile's location, discard interest.");
                    return;
                }
            }


            try {
                String TAG="WiFiManagement";
                WifiManager wifiManager;
                List<ScanResult> list;


                    wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//                    openWifi();
                    list = wifiManager.getScanResults();
//        ListView listView = (ListView) findViewById(R.id.listView);
                    if (list == null) {
                        Log.i(TAG, "WiFi: wifi closed");
//            Toast.makeText(this, "wifi未打开！", Toast.LENGTH_LONG).show();
                    }else {
                        Log.i(TAG, "WiFi: multiple connection covering！ 共"+list.size()+"个。");
                        Log.i(TAG, "Available Name of WiFi gateway:");
                        Iterator iterator=list.iterator();
                        while (iterator.hasNext()){
                            Map.Entry entry = (Map.Entry) iterator.next();
                            Log.i(TAG, entry.getKey().toString()+entry.getValue().toString());
                        }
//            Toast.makeText(this, "多wifi覆盖！ 共"+list.size()+"个。", Toast.LENGTH_LONG).show();
//            listView.setAdapter(new MyAdapter(this,list));
                    }







                Log.i(TAG, "onInterest: start to construct the Data packet...");
                long currentTimeStart=System.currentTimeMillis()/1000;
                long currentTimeEnd=currentTimeStart+10;

                Name tempName=interest.getName();
//                String temp[]=interest.getName().toString().split("/");
                MetaInfo metaInfo=new MetaInfo();
                metaInfo.setFreshnessPeriod(1000);
                if (temp[temp.length-2].equals("temp")){
                    sensorType="temp";
                    sensorManager.registerListener(listener,temperature,SensorManager.SENSOR_DELAY_UI);
                    Data backData=new Data(tempName);
//                    Blob blob=new Blob(deviceLatLng.latitude+"," +deviceLatLng.longitude+currentTimeStart+currentTimeEnd+"/temp/"+sensorValue);
                    Blob blob=new Blob("/"+BaiduLocation.getLatitude()+"," +BaiduLocation.getLongitude()+"/"+currentTimeStart+"/"+currentTimeEnd+"/temp/"+sensorValueOfTemp+list.toString());
                    backData.setContent(blob);
                    backData.setMetaInfo(metaInfo);
                    face.putData(backData);
                    Log.i(TAG, "send Data:"+backData.getContent()+" of Interest"+interest.getName()+" success!");

                }
                else if (temp[temp.length-2].equals("light")){
                    sensorType="light";
                    sensorManager.registerListener(listener,light,SensorManager.SENSOR_DELAY_UI);
                    Data backData=new Data(tempName);
//                    Blob blob=new Blob("/"+deviceLatLng.latitude+"," +deviceLatLng.longitude+currentTimeStart+currentTimeEnd+"/light/"+sensorValue);
                    Blob blob=new Blob("/"+BaiduLocation.getLatitude()+"," +BaiduLocation.getLongitude()+"/"+currentTimeStart+"/"+currentTimeEnd+"/light/"+sensorValueOfLight);
                    backData.setContent(blob);
                    backData.setMetaInfo(metaInfo);
                    face.putData(backData);
                    Log.i(TAG, "send Data:"+backData.getContent()+" of Interest"+interest.getName()+" success!");
                }
                else if (temp[temp.length-2].equals("acceler")){
                    sensorType="acceler";
                    sensorManager.registerListener(listener,accelerometer,SensorManager.SENSOR_DELAY_UI);
                    Data backData=new Data(tempName);
//                    Blob blob=new Blob(deviceLatLng.latitude+"," +deviceLatLng.longitude+currentTimeStart+currentTimeEnd+"/acceler/"+sensorValue);
                    Blob blob=new Blob("/"+BaiduLocation.getLatitude()+"," +BaiduLocation.getLongitude()+"/"+currentTimeStart+"/"+currentTimeEnd+"/acceler/"+sensorValueOfAcceler);
                    backData.setContent(blob);
                    backData.setMetaInfo(metaInfo);
                    face.putData(backData);
                    Log.i(TAG, "send Data:"+backData.getContent()+" of Interest"+interest.getName()+" success!");
                }
                else if (temp[temp.length-2].equals("location")){
                    Data backData=new Data(tempName);
//                    Blob blob=new Blob(deviceLatLng.latitude+"," +deviceLatLng.longitude+currentTimeStart+currentTimeEnd+"/acceler/"+sensorValue);
                    Blob blob=new Blob("/"+BaiduLocation.getLatitude()+"," +BaiduLocation.getLongitude()+"/"+currentTimeStart+"/"+currentTimeEnd+"/location");
                    backData.setContent(blob);
                    backData.setMetaInfo(metaInfo);
                    face.putData(backData);
                    Log.i(TAG, "send Data:"+backData.getContent()+" of Interest"+interest.getName()+" success!");
                }
//                    sensorManager.unregisterListener(listener);


            }
            catch (Exception e){
                Log.i(TAG, "send data exception:"+e.getMessage());
                e.printStackTrace();
            }

        }

    }


    //register prefix filter failed
    private class onregisterfailed implements OnRegisterFailed{
        @Override
        public void onRegisterFailed(Name name) {
            Log.i(TAG, "register failed of name prefix"+name.toUri());
        }
    }



    //register device in gateway and create face to receive Interest for this device and send back data
    @Override
    public void onCreate(){
        super.onCreate();
        //operate network in main not in Asyntask;
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
                .penaltyLog().penaltyDeath().build());




        //face = new Face(HOST,PORT);//temporary face name of gateway
        face = new Face();//temporary face name of gateway
//        Looper.prepare();
        //get location client of map for locating
        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener( myListener );    //注册监听函数
        initLocation();//初始化
        mLocationClient.start();//开始获取服务
        Log.i(TAG, "location client start: "+mLocationClient.isStarted());

        //get sensor service of device for sensory data
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);

        light =sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        temperature=sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Log.i(TAG, "sensor manager start...");
        

        /*String registerName="wifi/location/ID";
        Name deviceName=new Name(registerName);
        incomingData incomD=new incomingData();*/
        /*Message msg = new Message();

        msg.what = 200; // Result Code ex) Success code: 200 , Fail Code:
        // 400 ...

        msg.obj = null; // Result Object

        actionHandler.sendMessage(msg);*/
        /*try {


            Log.i(TAG, "Express name " + deviceName.toUri());

            while (callbackCount< 1) {

                face.processEvents();

                // We need to sleep for a few milliseconds so we don't use
                // 100% of

                // the CPU.

                Thread.sleep(10);

            }

            //register device success
            if(registerSignal==1){
                //Face faceListen=new Face("gatewayIP");
                incomingInterest incomI= new incomingInterest();
                onregisterfailed regfailed=new onregisterfailed();
                Name incomInName=new Name("wifi/location/");
                face.registerPrefix(incomInName,incomI,regfailed);
                //face.setInterestFilter(new Name("/wifi/location/"),incomI);
                while (true) {

                    face.processEvents();

                    // We need to sleep for a few milliseconds so we don't use
                    // 100% of

                    // the CPU.

                    Thread.sleep(10);

                }
            }
            else{
                Log.i(TAG, "register device in gateway failed, try later...");
            }


        }
        catch (Exception e)
        {
            System.out.printf("aaaa");
            Log.e(TAG, "express Interest exception: " + e.getMessage());
            e.printStackTrace();
        }*/



    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "onStartCommand: ");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //release location service
        mLocationClient.stop();
        //release sensor service
        sensorManager.unregisterListener(listener);

//        Looper.loop();
    }

    private static ByteBuffer
    toBuffer(int[] array)
    {
        ByteBuffer result = ByteBuffer.allocate(array.length);
        for (int i = 0; i < array.length; ++i)
            result.put((byte)(array[i] & 0xff));

        result.flip();
        return result;
    }

    private static final ByteBuffer DEFAULT_RSA_PUBLIC_KEY_DER = toBuffer(new int[] {
            0x30, 0x82, 0x01, 0x22, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01,
            0x01, 0x05, 0x00, 0x03, 0x82, 0x01, 0x0f, 0x00, 0x30, 0x82, 0x01, 0x0a, 0x02, 0x82, 0x01, 0x01,
            0x00, 0xb8, 0x09, 0xa7, 0x59, 0x82, 0x84, 0xec, 0x4f, 0x06, 0xfa, 0x1c, 0xb2, 0xe1, 0x38, 0x93,
            0x53, 0xbb, 0x7d, 0xd4, 0xac, 0x88, 0x1a, 0xf8, 0x25, 0x11, 0xe4, 0xfa, 0x1d, 0x61, 0x24, 0x5b,
            0x82, 0xca, 0xcd, 0x72, 0xce, 0xdb, 0x66, 0xb5, 0x8d, 0x54, 0xbd, 0xfb, 0x23, 0xfd, 0xe8, 0x8e,
            0xaf, 0xa7, 0xb3, 0x79, 0xbe, 0x94, 0xb5, 0xb7, 0xba, 0x17, 0xb6, 0x05, 0xae, 0xce, 0x43, 0xbe,
            0x3b, 0xce, 0x6e, 0xea, 0x07, 0xdb, 0xbf, 0x0a, 0x7e, 0xeb, 0xbc, 0xc9, 0x7b, 0x62, 0x3c, 0xf5,
            0xe1, 0xce, 0xe1, 0xd9, 0x8d, 0x9c, 0xfe, 0x1f, 0xc7, 0xf8, 0xfb, 0x59, 0xc0, 0x94, 0x0b, 0x2c,
            0xd9, 0x7d, 0xbc, 0x96, 0xeb, 0xb8, 0x79, 0x22, 0x8a, 0x2e, 0xa0, 0x12, 0x1d, 0x42, 0x07, 0xb6,
            0x5d, 0xdb, 0xe1, 0xf6, 0xb1, 0x5d, 0x7b, 0x1f, 0x54, 0x52, 0x1c, 0xa3, 0x11, 0x9b, 0xf9, 0xeb,
            0xbe, 0xb3, 0x95, 0xca, 0xa5, 0x87, 0x3f, 0x31, 0x18, 0x1a, 0xc9, 0x99, 0x01, 0xec, 0xaa, 0x90,
            0xfd, 0x8a, 0x36, 0x35, 0x5e, 0x12, 0x81, 0xbe, 0x84, 0x88, 0xa1, 0x0d, 0x19, 0x2a, 0x4a, 0x66,
            0xc1, 0x59, 0x3c, 0x41, 0x83, 0x3d, 0x3d, 0xb8, 0xd4, 0xab, 0x34, 0x90, 0x06, 0x3e, 0x1a, 0x61,
            0x74, 0xbe, 0x04, 0xf5, 0x7a, 0x69, 0x1b, 0x9d, 0x56, 0xfc, 0x83, 0xb7, 0x60, 0xc1, 0x5e, 0x9d,
            0x85, 0x34, 0xfd, 0x02, 0x1a, 0xba, 0x2c, 0x09, 0x72, 0xa7, 0x4a, 0x5e, 0x18, 0xbf, 0xc0, 0x58,
            0xa7, 0x49, 0x34, 0x46, 0x61, 0x59, 0x0e, 0xe2, 0x6e, 0x9e, 0xd2, 0xdb, 0xfd, 0x72, 0x2f, 0x3c,
            0x47, 0xcc, 0x5f, 0x99, 0x62, 0xee, 0x0d, 0xf3, 0x1f, 0x30, 0x25, 0x20, 0x92, 0x15, 0x4b, 0x04,
            0xfe, 0x15, 0x19, 0x1d, 0xdc, 0x7e, 0x5c, 0x10, 0x21, 0x52, 0x21, 0x91, 0x54, 0x60, 0x8b, 0x92,
            0x41, 0x02, 0x03, 0x01, 0x00, 0x01
    });

    // Java uses an unencrypted PKCS #8 PrivateKeyInfo, not a PKCS #1 RSAPrivateKey.
    private static final ByteBuffer DEFAULT_RSA_PRIVATE_KEY_DER = toBuffer(new int[] {
            0x30, 0x82, 0x04, 0xbf, 0x02, 0x01, 0x00, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7,
            0x0d, 0x01, 0x01, 0x01, 0x05, 0x00, 0x04, 0x82, 0x04, 0xa9, 0x30, 0x82, 0x04, 0xa5, 0x02, 0x01,
            0x00, 0x02, 0x82, 0x01, 0x01, 0x00, 0xb8, 0x09, 0xa7, 0x59, 0x82, 0x84, 0xec, 0x4f, 0x06, 0xfa,
            0x1c, 0xb2, 0xe1, 0x38, 0x93, 0x53, 0xbb, 0x7d, 0xd4, 0xac, 0x88, 0x1a, 0xf8, 0x25, 0x11, 0xe4,
            0xfa, 0x1d, 0x61, 0x24, 0x5b, 0x82, 0xca, 0xcd, 0x72, 0xce, 0xdb, 0x66, 0xb5, 0x8d, 0x54, 0xbd,
            0xfb, 0x23, 0xfd, 0xe8, 0x8e, 0xaf, 0xa7, 0xb3, 0x79, 0xbe, 0x94, 0xb5, 0xb7, 0xba, 0x17, 0xb6,
            0x05, 0xae, 0xce, 0x43, 0xbe, 0x3b, 0xce, 0x6e, 0xea, 0x07, 0xdb, 0xbf, 0x0a, 0x7e, 0xeb, 0xbc,
            0xc9, 0x7b, 0x62, 0x3c, 0xf5, 0xe1, 0xce, 0xe1, 0xd9, 0x8d, 0x9c, 0xfe, 0x1f, 0xc7, 0xf8, 0xfb,
            0x59, 0xc0, 0x94, 0x0b, 0x2c, 0xd9, 0x7d, 0xbc, 0x96, 0xeb, 0xb8, 0x79, 0x22, 0x8a, 0x2e, 0xa0,
            0x12, 0x1d, 0x42, 0x07, 0xb6, 0x5d, 0xdb, 0xe1, 0xf6, 0xb1, 0x5d, 0x7b, 0x1f, 0x54, 0x52, 0x1c,
            0xa3, 0x11, 0x9b, 0xf9, 0xeb, 0xbe, 0xb3, 0x95, 0xca, 0xa5, 0x87, 0x3f, 0x31, 0x18, 0x1a, 0xc9,
            0x99, 0x01, 0xec, 0xaa, 0x90, 0xfd, 0x8a, 0x36, 0x35, 0x5e, 0x12, 0x81, 0xbe, 0x84, 0x88, 0xa1,
            0x0d, 0x19, 0x2a, 0x4a, 0x66, 0xc1, 0x59, 0x3c, 0x41, 0x83, 0x3d, 0x3d, 0xb8, 0xd4, 0xab, 0x34,
            0x90, 0x06, 0x3e, 0x1a, 0x61, 0x74, 0xbe, 0x04, 0xf5, 0x7a, 0x69, 0x1b, 0x9d, 0x56, 0xfc, 0x83,
            0xb7, 0x60, 0xc1, 0x5e, 0x9d, 0x85, 0x34, 0xfd, 0x02, 0x1a, 0xba, 0x2c, 0x09, 0x72, 0xa7, 0x4a,
            0x5e, 0x18, 0xbf, 0xc0, 0x58, 0xa7, 0x49, 0x34, 0x46, 0x61, 0x59, 0x0e, 0xe2, 0x6e, 0x9e, 0xd2,
            0xdb, 0xfd, 0x72, 0x2f, 0x3c, 0x47, 0xcc, 0x5f, 0x99, 0x62, 0xee, 0x0d, 0xf3, 0x1f, 0x30, 0x25,
            0x20, 0x92, 0x15, 0x4b, 0x04, 0xfe, 0x15, 0x19, 0x1d, 0xdc, 0x7e, 0x5c, 0x10, 0x21, 0x52, 0x21,
            0x91, 0x54, 0x60, 0x8b, 0x92, 0x41, 0x02, 0x03, 0x01, 0x00, 0x01, 0x02, 0x82, 0x01, 0x01, 0x00,
            0x8a, 0x05, 0xfb, 0x73, 0x7f, 0x16, 0xaf, 0x9f, 0xa9, 0x4c, 0xe5, 0x3f, 0x26, 0xf8, 0x66, 0x4d,
            0xd2, 0xfc, 0xd1, 0x06, 0xc0, 0x60, 0xf1, 0x9f, 0xe3, 0xa6, 0xc6, 0x0a, 0x48, 0xb3, 0x9a, 0xca,
            0x21, 0xcd, 0x29, 0x80, 0x88, 0x3d, 0xa4, 0x85, 0xa5, 0x7b, 0x82, 0x21, 0x81, 0x28, 0xeb, 0xf2,
            0x43, 0x24, 0xb0, 0x76, 0xc5, 0x52, 0xef, 0xc2, 0xea, 0x4b, 0x82, 0x41, 0x92, 0xc2, 0x6d, 0xa6,
            0xae, 0xf0, 0xb2, 0x26, 0x48, 0xa1, 0x23, 0x7f, 0x02, 0xcf, 0xa8, 0x90, 0x17, 0xa2, 0x3e, 0x8a,
            0x26, 0xbd, 0x6d, 0x8a, 0xee, 0xa6, 0x0c, 0x31, 0xce, 0xc2, 0xbb, 0x92, 0x59, 0xb5, 0x73, 0xe2,
            0x7d, 0x91, 0x75, 0xe2, 0xbd, 0x8c, 0x63, 0xe2, 0x1c, 0x8b, 0xc2, 0x6a, 0x1c, 0xfe, 0x69, 0xc0,
            0x44, 0xcb, 0x58, 0x57, 0xb7, 0x13, 0x42, 0xf0, 0xdb, 0x50, 0x4c, 0xe0, 0x45, 0x09, 0x8f, 0xca,
            0x45, 0x8a, 0x06, 0xfe, 0x98, 0xd1, 0x22, 0xf5, 0x5a, 0x9a, 0xdf, 0x89, 0x17, 0xca, 0x20, 0xcc,
            0x12, 0xa9, 0x09, 0x3d, 0xd5, 0xf7, 0xe3, 0xeb, 0x08, 0x4a, 0xc4, 0x12, 0xc0, 0xb9, 0x47, 0x6c,
            0x79, 0x50, 0x66, 0xa3, 0xf8, 0xaf, 0x2c, 0xfa, 0xb4, 0x6b, 0xec, 0x03, 0xad, 0xcb, 0xda, 0x24,
            0x0c, 0x52, 0x07, 0x87, 0x88, 0xc0, 0x21, 0xf3, 0x02, 0xe8, 0x24, 0x44, 0x0f, 0xcd, 0xa0, 0xad,
            0x2f, 0x1b, 0x79, 0xab, 0x6b, 0x49, 0x4a, 0xe6, 0x3b, 0xd0, 0xad, 0xc3, 0x48, 0xb9, 0xf7, 0xf1,
            0x34, 0x09, 0xeb, 0x7a, 0xc0, 0xd5, 0x0d, 0x39, 0xd8, 0x45, 0xce, 0x36, 0x7a, 0xd8, 0xde, 0x3c,
            0xb0, 0x21, 0x96, 0x97, 0x8a, 0xff, 0x8b, 0x23, 0x60, 0x4f, 0xf0, 0x3d, 0xd7, 0x8f, 0xf3, 0x2c,
            0xcb, 0x1d, 0x48, 0x3f, 0x86, 0xc4, 0xa9, 0x00, 0xf2, 0x23, 0x2d, 0x72, 0x4d, 0x66, 0xa5, 0x01,
            0x02, 0x81, 0x81, 0x00, 0xdc, 0x4f, 0x99, 0x44, 0x0d, 0x7f, 0x59, 0x46, 0x1e, 0x8f, 0xe7, 0x2d,
            0x8d, 0xdd, 0x54, 0xc0, 0xf7, 0xfa, 0x46, 0x0d, 0x9d, 0x35, 0x03, 0xf1, 0x7c, 0x12, 0xf3, 0x5a,
            0x9d, 0x83, 0xcf, 0xdd, 0x37, 0x21, 0x7c, 0xb7, 0xee, 0xc3, 0x39, 0xd2, 0x75, 0x8f, 0xb2, 0x2d,
            0x6f, 0xec, 0xc6, 0x03, 0x55, 0xd7, 0x00, 0x67, 0xd3, 0x9b, 0xa2, 0x68, 0x50, 0x6f, 0x9e, 0x28,
            0xa4, 0x76, 0x39, 0x2b, 0xb2, 0x65, 0xcc, 0x72, 0x82, 0x93, 0xa0, 0xcf, 0x10, 0x05, 0x6a, 0x75,
            0xca, 0x85, 0x35, 0x99, 0xb0, 0xa6, 0xc6, 0xef, 0x4c, 0x4d, 0x99, 0x7d, 0x2c, 0x38, 0x01, 0x21,
            0xb5, 0x31, 0xac, 0x80, 0x54, 0xc4, 0x18, 0x4b, 0xfd, 0xef, 0xb3, 0x30, 0x22, 0x51, 0x5a, 0xea,
            0x7d, 0x9b, 0xb2, 0x9d, 0xcb, 0xba, 0x3f, 0xc0, 0x1a, 0x6b, 0xcd, 0xb0, 0xe6, 0x2f, 0x04, 0x33,
            0xd7, 0x3a, 0x49, 0x71, 0x02, 0x81, 0x81, 0x00, 0xd5, 0xd9, 0xc9, 0x70, 0x1a, 0x13, 0xb3, 0x39,
            0x24, 0x02, 0xee, 0xb0, 0xbb, 0x84, 0x17, 0x12, 0xc6, 0xbd, 0x65, 0x73, 0xe9, 0x34, 0x5d, 0x43,
            0xff, 0xdc, 0xf8, 0x55, 0xaf, 0x2a, 0xb9, 0xe1, 0xfa, 0x71, 0x65, 0x4e, 0x50, 0x0f, 0xa4, 0x3b,
            0xe5, 0x68, 0xf2, 0x49, 0x71, 0xaf, 0x15, 0x88, 0xd7, 0xaf, 0xc4, 0x9d, 0x94, 0x84, 0x6b, 0x5b,
            0x10, 0xd5, 0xc0, 0xaa, 0x0c, 0x13, 0x62, 0x99, 0xc0, 0x8b, 0xfc, 0x90, 0x0f, 0x87, 0x40, 0x4d,
            0x58, 0x88, 0xbd, 0xe2, 0xba, 0x3e, 0x7e, 0x2d, 0xd7, 0x69, 0xa9, 0x3c, 0x09, 0x64, 0x31, 0xb6,
            0xcc, 0x4d, 0x1f, 0x23, 0xb6, 0x9e, 0x65, 0xd6, 0x81, 0xdc, 0x85, 0xcc, 0x1e, 0xf1, 0x0b, 0x84,
            0x38, 0xab, 0x93, 0x5f, 0x9f, 0x92, 0x4e, 0x93, 0x46, 0x95, 0x6b, 0x3e, 0xb6, 0xc3, 0x1b, 0xd7,
            0x69, 0xa1, 0x0a, 0x97, 0x37, 0x78, 0xed, 0xd1, 0x02, 0x81, 0x80, 0x33, 0x18, 0xc3, 0x13, 0x65,
            0x8e, 0x03, 0xc6, 0x9f, 0x90, 0x00, 0xae, 0x30, 0x19, 0x05, 0x6f, 0x3c, 0x14, 0x6f, 0xea, 0xf8,
            0x6b, 0x33, 0x5e, 0xee, 0xc7, 0xf6, 0x69, 0x2d, 0xdf, 0x44, 0x76, 0xaa, 0x32, 0xba, 0x1a, 0x6e,
            0xe6, 0x18, 0xa3, 0x17, 0x61, 0x1c, 0x92, 0x2d, 0x43, 0x5d, 0x29, 0xa8, 0xdf, 0x14, 0xd8, 0xff,
            0xdb, 0x38, 0xef, 0xb8, 0xb8, 0x2a, 0x96, 0x82, 0x8e, 0x68, 0xf4, 0x19, 0x8c, 0x42, 0xbe, 0xcc,
            0x4a, 0x31, 0x21, 0xd5, 0x35, 0x6c, 0x5b, 0xa5, 0x7c, 0xff, 0xd1, 0x85, 0x87, 0x28, 0xdc, 0x97,
            0x75, 0xe8, 0x03, 0x80, 0x1d, 0xfd, 0x25, 0x34, 0x41, 0x31, 0x21, 0x12, 0x87, 0xe8, 0x9a, 0xb7,
            0x6a, 0xc0, 0xc4, 0x89, 0x31, 0x15, 0x45, 0x0d, 0x9c, 0xee, 0xf0, 0x6a, 0x2f, 0xe8, 0x59, 0x45,
            0xc7, 0x7b, 0x0d, 0x6c, 0x55, 0xbb, 0x43, 0xca, 0xc7, 0x5a, 0x01, 0x02, 0x81, 0x81, 0x00, 0xab,
            0xf4, 0xd5, 0xcf, 0x78, 0x88, 0x82, 0xc2, 0xdd, 0xbc, 0x25, 0xe6, 0xa2, 0xc1, 0xd2, 0x33, 0xdc,
            0xef, 0x0a, 0x97, 0x2b, 0xdc, 0x59, 0x6a, 0x86, 0x61, 0x4e, 0xa6, 0xc7, 0x95, 0x99, 0xa6, 0xa6,
            0x55, 0x6c, 0x5a, 0x8e, 0x72, 0x25, 0x63, 0xac, 0x52, 0xb9, 0x10, 0x69, 0x83, 0x99, 0xd3, 0x51,
            0x6c, 0x1a, 0xb3, 0x83, 0x6a, 0xff, 0x50, 0x58, 0xb7, 0x28, 0x97, 0x13, 0xe2, 0xba, 0x94, 0x5b,
            0x89, 0xb4, 0xea, 0xba, 0x31, 0xcd, 0x78, 0xe4, 0x4a, 0x00, 0x36, 0x42, 0x00, 0x62, 0x41, 0xc6,
            0x47, 0x46, 0x37, 0xea, 0x6d, 0x50, 0xb4, 0x66, 0x8f, 0x55, 0x0c, 0xc8, 0x99, 0x91, 0xd5, 0xec,
            0xd2, 0x40, 0x1c, 0x24, 0x7d, 0x3a, 0xff, 0x74, 0xfa, 0x32, 0x24, 0xe0, 0x11, 0x2b, 0x71, 0xad,
            0x7e, 0x14, 0xa0, 0x77, 0x21, 0x68, 0x4f, 0xcc, 0xb6, 0x1b, 0xe8, 0x00, 0x49, 0x13, 0x21, 0x02,
            0x81, 0x81, 0x00, 0xb6, 0x18, 0x73, 0x59, 0x2c, 0x4f, 0x92, 0xac, 0xa2, 0x2e, 0x5f, 0xb6, 0xbe,
            0x78, 0x5d, 0x47, 0x71, 0x04, 0x92, 0xf0, 0xd7, 0xe8, 0xc5, 0x7a, 0x84, 0x6b, 0xb8, 0xb4, 0x30,
            0x1f, 0xd8, 0x0d, 0x58, 0xd0, 0x64, 0x80, 0xa7, 0x21, 0x1a, 0x48, 0x00, 0x37, 0xd6, 0x19, 0x71,
            0xbb, 0x91, 0x20, 0x9d, 0xe2, 0xc3, 0xec, 0xdb, 0x36, 0x1c, 0xca, 0x48, 0x7d, 0x03, 0x32, 0x74,
            0x1e, 0x65, 0x73, 0x02, 0x90, 0x73, 0xd8, 0x3f, 0xb5, 0x52, 0x35, 0x79, 0x1c, 0xee, 0x93, 0xa3,
            0x32, 0x8b, 0xed, 0x89, 0x98, 0xf1, 0x0c, 0xd8, 0x12, 0xf2, 0x89, 0x7f, 0x32, 0x23, 0xec, 0x67,
            0x66, 0x52, 0x83, 0x89, 0x99, 0x5e, 0x42, 0x2b, 0x42, 0x4b, 0x84, 0x50, 0x1b, 0x3e, 0x47, 0x6d,
            0x74, 0xfb, 0xd1, 0xa6, 0x10, 0x20, 0x6c, 0x6e, 0xbe, 0x44, 0x3f, 0xb9, 0xfe, 0xbc, 0x8d, 0xda,
            0xcb, 0xea, 0x8f
    });



    public  void registerRouteInNFD(Name name, String faceUri){
        try {
            Log.i(TAG, "registerRouteInNFD: begin...");
            final Name prefix = name;
            // Route to aleph.ndn.ucla.edu.  Have to use the canonical name with
            // an IP address and port.
            final String uri = faceUri;

            // The default Face connects to the local NFD.
//            final Face face = new Face();

            // For now, when setting face.setCommandSigningInfo, use a key chain with
            //   a default private key instead of the system default key chain. This
            //   is OK for now because NFD is configured to skip verification, so it
            //   ignores the system default key chain.
            // On a platform which supports it, it would be better to use the default
            //   KeyChain constructor.
            MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
            MemoryPrivateKeyStorage privateKeyStorage = new MemoryPrivateKeyStorage();
            KeyChain keyChain = new KeyChain
                    (new IdentityManager(identityStorage, privateKeyStorage),
                            new SelfVerifyPolicyManager(identityStorage));
            keyChain.setFace(face);

            // Initialize the storage.
            Name keyName = new Name("/testname/DSK-123");
            Name certificateName = keyName.getSubName(0, keyName.size() - 1).append
                    ("KEY").append(keyName.get(-1)).append("ID-CERT").append("0");
            identityStorage.addKey(keyName, KeyType.RSA, new Blob(DEFAULT_RSA_PUBLIC_KEY_DER, false));
            privateKeyStorage.setKeyPairForKeyName
                    (keyName, KeyType.RSA, DEFAULT_RSA_PUBLIC_KEY_DER, DEFAULT_RSA_PRIVATE_KEY_DER);

            face.setCommandSigningInfo(keyChain, certificateName);

            // Create the /localhost/nfd/faces/query command interest, including the
            // FaceQueryFilter. Construct the FaceQueryFilter using the structure in
            // FaceQueryFilterProto.java which was produced by protoc.
            FaceQueryFilterProto.FaceQueryFilterMessage.Builder builder = FaceQueryFilterProto.FaceQueryFilterMessage.newBuilder();
            FaceQueryFilterProto.FaceQueryFilterMessage.FaceQueryFilter.Builder filterBuilder =
                    builder.addFaceQueryFilterBuilder();
            filterBuilder.setUri(uri);
            Blob encodedFilter = ProtobufTlvOfCB.encode(builder.build());

            Interest interest = new Interest(new Name("/localhost/nfd/faces/query"));
            interest.getName().append(encodedFilter);

            final boolean[] enabled = new boolean[] { true };
            SegmentFetcher.fetch
                    (face, interest, SegmentFetcher.DontVerifySegment,
                            new SegmentFetcher.OnComplete() {
                                public void onComplete(Blob content) {
                                    processFaceStatus(content, prefix, uri, face, enabled);
                                }},
                            new SegmentFetcher.OnError() {
                                public void onError(SegmentFetcher.ErrorCode errorCode, String message) {
                                    enabled[0] = false;
                                    Log.i(TAG,message);
                                }});

            // Loop calling processEvents until a callback sets enabled[0] = false.
            while (enabled[0]) {
                face.processEvents();

                // We need to sleep for a few milliseconds so we don't use 100% of
                //   the CPU.
                Thread.sleep(5);
            }
        }
        catch (Exception e) {
            Log.e(TAG,"exception in RegisterRouteInNFD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This is called when all the segments are received to decode the
     * encodedFaceStatus as a TLV FaceStatus message. If the face ID exists for
     * the face URL, use it to call registerRoute(), otherwise send a
     * /localhost/nfd/faces/create command to create the face.
     * @param encodedFaceStatus The TLV-encoded FaceStatus.
     * @param prefix The prefix name to register.
     * @param uri The remote URI in case we need to tell NFD to create a face.
     * @param face The Face which is used to sign the command interest and call
     * expressInterest.
     * @param enabled On success or error, set enabled[0] = false.
     */
    private static void
    processFaceStatus
    (Blob encodedFaceStatus, final Name prefix, String uri, final Face face,
     final boolean[] enabled)
    {
        try {
            if (encodedFaceStatus.size() == 0) {
                // No result, so we need to tell NFD to create the face.
                // Encode the ControlParameters.
                ControlParametersProto.ControlParametersTypes.ControlParametersMessage.Builder builder = ControlParametersProto.ControlParametersTypes.ControlParametersMessage.newBuilder();
                builder.getControlParametersBuilder().setUri(uri);
                Blob encodedControlParameters = ProtobufTlvOfCB.encode(builder.build());

                Interest interest = new Interest(new Name("/localhost/nfd/faces/create"));
                interest.getName().append(encodedControlParameters);
                interest.setInterestLifetimeMilliseconds(10000);

                // Sign and express the interest.
                face.makeCommandInterest(interest);
                face.expressInterest
                        (interest,
                                new OnData() {
                                    public void onData(Interest interest, Data data) {
                                        processCreateFaceResponse(data.getContent(), prefix, face, enabled);
                                    }},
                                new OnTimeout() {
                                    public void onTimeout(Interest interest) {
                                        enabled[0] = false;
                                        Log.i(TAG,"Face create command timed out.");
                                    }});
            }
            else {
                FaceStatusProto.FaceStatusMessage.Builder decodedFaceStatus = FaceStatusProto.FaceStatusMessage.newBuilder();
                ProtobufTlvOfCB.decode(decodedFaceStatus, encodedFaceStatus);

                long faceId = decodedFaceStatus.getFaceStatus(0).getFaceId();

                Log.i(TAG,"Found face ID " + faceId);
                registerRoute(prefix, faceId, face, enabled);
            }
        }
        catch (Exception e) {
            Log.e(TAG,"exception: processFaceStatus " + e.getMessage());
            e.printStackTrace();
            enabled[0] = false;
        }
    }

    /**
     * This is called when the face create command responds to decode the
     * encodedControlResonse as a TLV ControlResponse message containing one
     * ControlParameters. Get the face ID and call registerRoute().
     * @param encodedControlResponse The TLV-encoded ControlResponse.
     * @param prefix The prefix name to register.
     * @param face The Face which is used to sign the command interest and call
     * expressInterest.
     * @param enabled On success or error, set enabled[0] = false.
     */
    private static void
    processCreateFaceResponse
    (Blob encodedControlResponse, Name prefix, Face face, final boolean[] enabled)
    {
        try {
            ControlParametersProto.ControlParametersTypes.ControlParametersResponseMessage.Builder decodedControlResponse =
                    ControlParametersProto.ControlParametersTypes.ControlParametersResponseMessage.newBuilder();
            ProtobufTlvOfCB.decode(decodedControlResponse, encodedControlResponse);
            ControlParametersProto.ControlParametersTypes.ControlParametersResponse controlResponse =
                    decodedControlResponse.getControlResponse();

            final int lowestErrorCode = 400;
            if (controlResponse.getStatusCode() >= lowestErrorCode) {
                Log.e
                        (TAG,"Face create command got error, code " + controlResponse.getStatusCode() +
                                ": " + controlResponse.getStatusText());
                enabled[0] = false;
                return;
            }
            if (controlResponse.getControlParametersCount() != 1) {
                Log.e
                        (TAG,"Face create command response does not have one ControlParameters");
                enabled[0] = false;
                return;
            }

            long faceId = controlResponse.getControlParameters(0).getFaceId();

            Log.i(TAG,"Created face ID " + faceId);
            registerRoute(prefix, faceId, face, enabled);
        }
        catch (Exception e) {
            Log.e(TAG,"exception: processCreateFaceResponse " + e.getMessage());
            e.printStackTrace();
            enabled[0] = false;
        }
    }

    /**
     * Use /localhost/nfd/rib/register to register the prefix to the faceId.
     * @param prefix The prefix name to register.
     * @param faceId The face ID.
     * @param face The Face which is used to sign the command interest and call
     * expressInterest.
     * @param enabled On success or error, set enabled[0] = false.
     */
    private static void
    registerRoute(Name prefix, long faceId, Face face, final boolean[] enabled)
    {
        // Use default values;
        long origin = 255;
        long cost = 0;
        final long CHILD_INHERIT = 1;
        long flags = CHILD_INHERIT;

        try {
            ControlParametersProto.ControlParametersTypes.ControlParametersMessage.Builder builder = ControlParametersProto.ControlParametersTypes.ControlParametersMessage.newBuilder();
            ControlParametersProto.ControlParametersTypes.Name.Builder nameBuilder =
                    builder.getControlParametersBuilder().getNameBuilder();
            for (int i = 0; i < prefix.size(); ++i)
                nameBuilder.addComponent(ByteString.copyFrom(prefix.get(i).getValue().buf()));
            builder.getControlParametersBuilder().setFaceId(faceId);
            builder.getControlParametersBuilder().setOrigin(origin);
            builder.getControlParametersBuilder().setCost(cost);
            builder.getControlParametersBuilder().setFlags(flags);
            Blob encodedControlParameters = ProtobufTlvOfCB.encode(builder.build());

            Interest interest = new Interest(new Name("/localhost/nfd/rib/register"));
            interest.getName().append(encodedControlParameters);
            interest.setInterestLifetimeMilliseconds(10000);

            // Sign and express the interest.
            face.makeCommandInterest(interest);
            face.expressInterest
                    (interest,
                            new OnData() {
                                public void onData(Interest interest, Data data) {
                                    enabled[0] = false;
                                    processRegisterResponse(data.getContent());
                                }},
                            new OnTimeout() {
                                public void onTimeout(Interest interest) {
                                    enabled[0] = false;
                                    Log.i(TAG,"Register route command timed out.");
                                }});
        }
        catch (Exception e) {
            Log.e(TAG,"exception: registerRoute " + e.getMessage());
            e.printStackTrace();
            enabled[0] = false;
        }
    }

    /**
     * This is called when the register route command responds to decode the
     * encodedControlResponse as a TLV ControlParametersResponse message
     * containing one ControlParameters. On success, print the ControlParameters
     * values which should be the same as requested.
     * @param encodedControlResponse The TLV-encoded ControlParametersResponse.
     */
    private static void
    processRegisterResponse(Blob encodedControlResponse)
    {
        try {
            ControlParametersProto.ControlParametersTypes.ControlParametersResponseMessage.Builder decodedControlResponse =
                    ControlParametersProto.ControlParametersTypes.ControlParametersResponseMessage.newBuilder();
            ProtobufTlvOfCB.decode(decodedControlResponse, encodedControlResponse);
            ControlParametersProto.ControlParametersTypes.ControlParametersResponse controlResponse =
                    decodedControlResponse.getControlResponse();

            final int lowestErrorCode = 400;
            if (controlResponse.getStatusCode() >= lowestErrorCode) {
                Log.e
                        (TAG,"Face create command got error, code " + controlResponse.getStatusCode() +
                                ": " + controlResponse.getStatusText());
                return;
            }
            if (controlResponse.getControlParametersCount() != 1) {
                Log.e
                        (TAG,"Face create command response does not have one ControlParameters");
                return;
            }

            // Success. Print the ControlParameters response.
            ControlParametersProto.ControlParametersTypes.ControlParameters controlParameters =
                    controlResponse.getControlParameters(0);
            Log.i
                    (TAG,"Successful in name registration: ControlParameters(Name: " +
                            ProtobufTlvOfCB.toName(controlParameters.getName()).toUri() +
                            ", FaceId: " + controlParameters.getFaceId() +
                            ", Origin: " + controlParameters.getOrigin() +
                            ", Cost: " + controlParameters.getCost() +
                            ", Flags: " + controlParameters.getFlags() + ")");
        }
        catch (Exception e) {
            Log.e(TAG,"exception: processRegisterResponse" + e.getMessage());
            e.printStackTrace();
        }
    }








    public class ServiceBinder extends Binder {
        public boolean startBind(String userName,LatLng ll, String serverAddress) {



            deviceLatLng=ll;
            HOST=serverAddress;
            userID=userName;
            //set HOST uri;
            registerUri= "udp4://"+HOST+":6363";
            Log.i(TAG, "Start binding NDN service...success!");
            Log.i(TAG, "get userID argument: "+userID);
            Log.i(TAG, "get latlng argument: "+deviceLatLng.toString());
            Log.i(TAG, "get serverAddress argument: "+HOST);

            //Face faceListen=new Face("gatewayIP");

            //onregisterfailed regfailed=new onregisterfailed();

            double lat=deviceLatLng.latitude;
            double lng=deviceLatLng.longitude;
            //set filter of this device
            deviceName=new Name("/NDN-IOT-REGISTER/wifi/"+userID+"/"+lng+"/"+lat);
//            Name incomInName=new Name(deviceName);
            Log.i(TAG, "name initiate");

            //register route in NFD






            NetThread thread = new NetThread();

            thread.start();


            //register device success

            if (locationBase.size()>0&&topoBase.size()>0&&locationBaseMobile.size()>0&&topoBaseMobile.size()>0){
                return true;
            }
            else{
                Log.i(TAG, "startBind not finish topo and location initiate.");
                Log.i(TAG, "startBind: locationBase size:"+locationBase.size()+", topoBase size:"+topoBase.size()+", locationMobile size:"+locationBaseMobile.size()+"topoBaseMobile size:"+
                        topoBaseMobile.size());
                return false;
            }


        }



        public HashMap<Integer,WSNLocation> getLocationBase(){
            return locationBase;
        }


        public HashMap<Integer,Integer> getTopoBase(){
            return topoBase;
        }

        public HashMap<Integer,WiFiLocation> getLocationBaseMobile(){
            return locationBaseMobile;
        }

        public WiFiLocation getGatewayArea(){
            return gatewayArea;
        }


        public HashMap<Integer,Integer> getTopoBaseMobile(){
            return topoBaseMobile;
        }








    }

// UI controller

    /*private Handler actionHandler = new Handler() {

        public void handleMessage(Message msg) {

            String viewMsg = "Empty";

            switch (msg.what) { // Result Code

                case 200: // Result Code Ex) Success: 200

                    Log.i(TAG, "try to express Interest");
                    try {
                        face.expressInterest(deviceName, incomD, incomD);
                        System.out.printf("aaa");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    System.out.printf("");
                    break;

                default:
                    break;

            }


        }

    };*/

    //Net thread use for connect to NDN hub and register in gateway
    private class NetThread extends Thread {

        public NetThread() {

        }

        @Override
        public void run() {

            try {
                Log.i(TAG, "Run NetThread: construct /NDN-IOT/ prefix ");
                Name Request=new Name("/NDN-IOT/");
                Name InInterest=new Name("/wifi/");

                //use /wsn/topo /wifi/topo /wifi/location and /wsn/location to initiate on map and  use /wifi to register in remote NFD
                registerRouteInNFD(Request,registerUri);
                registerRouteInNFD(InInterest,registerUri);
                registerRouteInNFD(deviceName,registerUri);

                Log.i(TAG, "register route in NFD success!!");

                Log.i(TAG, "try to express Interest");
                try {
                    face.expressInterest(deviceName, incomD, incomD);
                    Log.i(TAG, "Express name " + deviceName.toUri());
                    while (callbackCount< 1) {

                        face.processEvents();

                        // We need to sleep for a few milliseconds so we don't use
                        // 100% of

                        // the CPU.

                        Thread.sleep(10);

                    }

//                    while(gatewayArea==null){
//                        Log.i(TAG, "run: do not receive gatewayArea...");
//                        Thread.sleep(10);
//                    }
                    Log.i(TAG, "run: gateway information is :"+gatewayArea.toString());
                    Name WSNtopoRequest=new Name("/NDN-IOT/"+gatewayAreaString+"/wsn/topo");
                    Name WiFitopoRequest=new Name("/NDN-IOT/"+gatewayAreaString+"/wifi/topo");
                    Name WiFilocationRequest=new Name("/NDN-IOT/"+gatewayAreaString+"/wifi/location");

                    Name WSNlocationRequest =new Name("/NDN-IOT/"+gatewayAreaString+"/wsn/location");


                    face.expressInterest(WSNlocationRequest, incomD, incomD);
                    Log.i(TAG, "Express name " + WSNlocationRequest.toUri());

                    face.expressInterest(WSNtopoRequest, incomD, incomD);
                    Log.i(TAG, "Express name " + WSNtopoRequest.toUri());

                    face.expressInterest(WiFilocationRequest, incomD, incomD);
                    Log.i(TAG, "Express name " + WiFilocationRequest.toUri());

                    face.expressInterest(WiFitopoRequest, incomD, incomD);
                    Log.i(TAG, "Express name " + WiFitopoRequest.toUri());
                    while (callbackCount< 4) {

                        face.processEvents();

                        // We need to sleep for a few milliseconds so we don't use
                        // 100% of

                        // the CPU.

                        Thread.sleep(10);

                    }

                    if(registerSignal==0){
                        incomingInterest incomI= new incomingInterest();

                        try{
                            Log.i(TAG, "setInterestFilter initiate...");
                            Name sensorInterest=new Name("/NDN-IOT/");
                            face.registerPrefix(sensorInterest,incomI,new OnRegisterFailed() {
                                public void onRegisterFailed(Name prefix) {
                                    Log.i(TAG, "onRegisterFailed: Failed to register the external forwarder: " + prefix.toUri());
                                    System.exit(1);
                                }
                            });
                            Log.i(TAG, "initiate setInterestFilter really success!");
//                    KeyChain keyChain=new KeyChain();
//                    Log.i(TAG, "keyChain initiate success");
//                    face.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
//                    Log.i(TAG, "sign info initiate success");
//                    face.registerPrefix(incomInName,incomI,regfailed);
                            new faceProcessEvent().execute();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            Log.e(TAG, "register prefix failed in device NFD "+e.getMessage());
                        }

                        //face.setInterestFilter(new Name("/wifi/location/"),incomI);

                    }
                    else{
                        Log.i(TAG, "register device in gateway failed, fail to create filter of this device try later...");
                    }



                } catch (IOException e) {
                    Log.e(TAG,"express Interest exception: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e){
                    Log.e(TAG, "express Interest exception: " + e.getMessage());
                    e.printStackTrace();
                }


            }

            catch (Exception e) {

                Log.i(TAG, "exception: " + e.getMessage());

                e.printStackTrace();

            }

        }

    }


    public class faceProcessEvent extends AsyncTask{
        @Override
        protected Object doInBackground(Object[] objects) {
            Log.i(TAG, "doInBackground: start face process event in loop!");
            try {
                while (true) {

                    face.processEvents();

                    // We need to sleep for a few milliseconds so we don't use
                    // 100% of

                    // the CPU.

                    Thread.sleep(10);

                }
            }
            catch (Exception e){
                Log.e(TAG, "doInBackground: initialize face process event failed..." );
                e.printStackTrace();
            }
            return null;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }
}


