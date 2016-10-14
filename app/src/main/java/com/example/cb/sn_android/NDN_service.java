package com.example.cb.sn_android;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.baidu.mapapi.model.LatLng;

import net.named_data.jndn.Data;

import net.named_data.jndn.Face;

import net.named_data.jndn.Interest;

import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;

import net.named_data.jndn.OnData;

import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encrypt.ConsumerDb;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.util.Blob;

import java.io.IOException;

/**
 * Created by cb on 16-9-26.NDN_Servie class provide NDN service for SN-android. 
 * Including register to the IOT gateway, send/receive Interest and receive/send Data.
 */
public class NDN_service extends Service{
    private static final String TAG = "ndn-service";
    private Face face;
    String registerName = "wifi/location/ID";
    Name deviceName= new Name(registerName);
    incomingData incomD = new incomingData();
    public int callbackCount= 0;
    private  int registerSignal=0;
    private LatLng deviceLatLng;
    private ServiceBinder serviceBinder=new ServiceBinder();
    private String HOST="10.103.242.144";
    private int PORT=6353;
    // dealing with come back data and time out
    private class incomingData implements OnData,OnTimeout{

        @Override
        public void onData(Interest interest, Data data) {
            callbackCount++;
            Log.i(TAG, "Got data packet with name" + data.getName().toUri());
            String msg=data.getContent().toString();
            if(msg!=null){
                Log.i(TAG, "onData: "+msg+"register success!");
                registerSignal=1;
            }

        }

        @Override
        public void onTimeout(Interest interest) {
            callbackCount++;
            Log.i(TAG, "Time out for interest" + interest.getName().toUri());
            //Log.i(TAG, "register in gateway failed!");
        }
    }
    //dealing with incoming Interest and package the data then send back to gateway
    private  class incomingInterest implements OnInterestCallback{
        @Override
        public void onInterest(Name name, Interest interest, Face face, long l, InterestFilter interestFilter) {
            Log.i(TAG, "Get interest:" + interest.getName().toUri()+" from face:"+face.toString());
            /*
            Dealing with interest and package the data here...
             */
            try {
                Name tempName=interest.getName();
                Data backData=new Data(tempName);
                Blob blob=new Blob("sensor info from device"+deviceLatLng.latitude+deviceLatLng.longitude);
                backData.setContent(blob);
                face.putData(backData);

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
        //face = new Face(HOST,PORT);//temporary face name of gateway
        face = new Face();//temporary face name of gateway

        NetThread thread = new NetThread();

        thread.start();
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
    }


    public class ServiceBinder extends Binder {
        public void startBind(LatLng ll) {
            deviceLatLng=ll;
            Log.i(TAG, "Start binding...success!");
            Log.i(TAG, "get latlng argument:"+deviceLatLng.toString());

            //register device success
            if(registerSignal==0){
                //Face faceListen=new Face("gatewayIP");
                incomingInterest incomI= new incomingInterest();
                //onregisterfailed regfailed=new onregisterfailed();

                double lat=deviceLatLng.latitude;
                double lng=deviceLatLng.longitude;
                //set filter of this device
                Name incomInName=new Name("wifi/"+lat+"/"+lng);
                Log.i(TAG, "name initiate");
                try{
                    Log.i(TAG, "setInterestFilter initiate...");
                    face.setInterestFilter(incomInName,incomI);
                    Log.i(TAG, "initiate setInterestFilter success!");
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




            //send Interest of selected area here;
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
