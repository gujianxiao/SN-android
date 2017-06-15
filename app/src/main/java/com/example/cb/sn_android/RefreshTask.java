package com.example.cb.sn_android;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.baidu.mapapi.model.LatLng;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

import java.util.HashMap;

/**
 * Created by cb on 16-11-12.
 * Refresh topo or location on the map; running in the background, waiting for mobile func;
 */
public class RefreshTask extends AsyncTask<String,Integer,Boolean>{
    private final String TAG="RefreshTask";
    private Callback callback;
    private int callbackCount=0;
    private int dataCount=0;
    private String type;
    HashMap<Integer,Integer> topoBase=new HashMap<>();
    HashMap<Integer,WSNLocation> locationBase=new HashMap<>();
    HashMap<Integer,Integer> topoBaseMobile=new HashMap<>();
    HashMap<Integer,WiFiLocation> locationBaseMobile=new HashMap<>();
    private Face face = new Face();
    incomingData incomD = new incomingData();


    public RefreshTask(Callback callback) {
        this.callback = callback;
    }

    interface Callback{
        public void refreshUI(HashMap<Integer,WSNLocation> location, HashMap<Integer,Integer> topo,HashMap<Integer,WiFiLocation> locationMobile, HashMap<Integer,Integer> topoMobile,String type);
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        Log.i(TAG, "doInBackground: start to refresh "+params[0]);
        type=params[0];
        if (params[0].equals("refreshLocation")){
            Name locationRequest =new Name("/NDN-IOT/wsn/location");
            Name locationRequestMobile=new Name("/NDN-IOT/wifi/location");
            try {
                face.expressInterest(locationRequest, incomD, incomD);
                Log.i(TAG, "Express name " + locationRequest.toUri());
                face.expressInterest(locationRequestMobile, incomD, incomD);
                Log.i(TAG, "Express name " + locationRequestMobile.toUri());
                while (callbackCount< 1) {

                    face.processEvents();

                    // We need to sleep for a few milliseconds so we don't use
                    // 100% of

                    // the CPU.

                    Thread.sleep(10);

                }
            }
            catch (Exception e){
                e.printStackTrace();
                Log.e(TAG, "doInBackground: Express Interst error!");

            }




        }
        else if(params[0].equals("refreshTopo")){
            Name locationRequest =new Name("/NDN-IOT/wsn/location");
            Name topoRequest=new Name("/NDN-IOT/wsn/topo");
            Name locationRequestMobile =new Name("/NDN-IOT/wifi/location");
            Name topoRequestMobile=new Name("/NDN-IOT/wifi/topo");
            try {
                face.expressInterest(locationRequest, incomD, incomD);
                Log.i(TAG, "Express name " + locationRequest.toUri());
                face.expressInterest(topoRequest, incomD, incomD);
                Log.i(TAG, "Express name " + topoRequest.toUri());

                face.expressInterest(locationRequestMobile, incomD, incomD);
                Log.i(TAG, "Express name " + locationRequestMobile.toUri());
                face.expressInterest(topoRequestMobile, incomD, incomD);
                Log.i(TAG, "Express name " + topoRequestMobile.toUri());

                while (callbackCount< 2) {

                    face.processEvents();

                    // We need to sleep for a few milliseconds so we don't use
                    // 100% of

                    // the CPU.

                    Thread.sleep(10);

                }
            }
            catch (Exception e){
                e.printStackTrace();
                Log.e(TAG, "doInBackground: Express Interst error!");
            }
        }
        else {
            Log.i(TAG, "doInBackground: Wrong argument!");
        }



        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        Log.i(TAG, "onPostExecute: execute async task of send interest success!");
        if(this.callback!=null){
            this.callback.refreshUI(locationBase,topoBase,locationBaseMobile,topoBaseMobile,type);
        }
    }

    private class incomingData implements OnData,OnTimeout {
        @Override
        public void onTimeout(Interest interest) {
            callbackCount++;
            Log.i(TAG, "Time out for interest:" + interest.getName().toUri());
        }

        @Override
        public void onData(Interest interest, Data data) {
            callbackCount++;
            dataCount++;
            Log.i(TAG, "Got data packet with name:" + data.getName().toUri());
            String msg = data.getContent().toString();
            Log.i(TAG, "onData: " + msg);
            if (msg.length()==0) {
                Log.i(TAG, "Data is null");
            } else if (msg.length()>0) {
                    String tempName= data.getName().toString();
                    if (tempName.equals("/NDN-IOT/wsn/topo")) {
                        initiateTopo(msg);
                    }
                    else if (tempName.equals("/NDN-IOT/wsn/location")){
                        initiateLocation(msg);
                    }
                    else if(tempName.equals("/NDN-IOT/wifi/topo")){
                        //initiateTopoMobile;
                        //maybe bugs
                        initiateTopoMobile(msg);
                    }
                    else if(tempName.equals("/NDN-IOT/wifi/location")){
                        //initiateLocationMobile;
                        //maybe bugss
                        initiateLocationMobile(msg);
                    }
                    else {
                        Log.e(TAG, "onData: Wrong answer from gateway!");
                    }
//                registerSignal=1;

            }


        }

    }


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
    public void initiateLocationMobile(String msg){
        Log.i(TAG, "initiateLocationMobile...");
        String locationSet[]=msg.split("\\$+");

        for(String location:locationSet){
            String temp[]=location.split("->|\\(|\\)|,");
            WiFiLocation tempWiFiLocation =new WiFiLocation(new LatLng(Double.valueOf(temp[3]),Double.valueOf(temp[4])),temp[5]);
            locationBaseMobile.put(Integer.parseInt(temp[0]), tempWiFiLocation);
            Log.i(TAG, "update location base mobile "+String.valueOf(temp[0])+" to "+locationBaseMobile.get(Integer.parseInt(temp[0])).getPoint().latitude+","+locationBaseMobile.get(Integer.parseInt(temp[0])).getPoint().longitude);
        }
    }
    
    
}
