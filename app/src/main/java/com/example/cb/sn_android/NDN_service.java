package com.example.cb.sn_android;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
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
import net.named_data.jndn.util.Blob;

/**
 * Created by cb on 16-9-26.NDN_Servie class provide NDN service for SN-android. 
 * Including register to the IOT gateway, send/receive Interest and receive/send Data.
 */
public class NDN_service extends Service{
    private static final String TAG = "ndn-service";
    public int callbackCount= 0;
    private  int registerSignal=0;
    private LatLng deviceLatLng;
    private ServiceBinder serviceBinder=new ServiceBinder();
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
            Log.i(TAG, "register failed!");
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

                Data backData=new Data();
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
            Log.i(TAG, "register name prefix"+name.toUri());
        }
    }



    //register device in gateway and create face to receive Interest for this device and send back data
    @Override
    public  void onCreate(){
        super.onCreate();
        Face face = new Face("gateway/control");//temporary face name of gateway
        String registerName="wifi/location/ID";
        Name deviceName=new Name();
        incomingData incomD=new incomingData();
        try {
            face.expressInterest(deviceName, incomD, incomD);

            Log.i(TAG, "Express name " + deviceName.toUri());

            while (callbackCount< 1) {

                face.processEvents();

                // We need to sleep for a few milliseconds so we don't use
                // 100% of

                // the CPU.

                Thread.sleep(5);

            }
            //register device success
            if(registerSignal==1){
                Face faceListen=new Face("gateway/datagram");
                incomingInterest incomI= new incomingInterest();
                onregisterfailed regfailed=new onregisterfailed();
                Name incomInName=new Name("wifi/location/");
                faceListen.registerPrefix(incomInName,incomI,regfailed);
                faceListen.setInterestFilter(new Name("/wifi/location/"),incomI);
            }
            else{
                Log.i(TAG, "register device in gateway failed, try later...");
            }


        }
        catch (Exception e)
        {
            Log.i(TAG, "exception: " + e.getMessage());
            e.printStackTrace();
        }




    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


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
            Log.i(TAG, "get latlng argument"+deviceLatLng.toString());
            //send Interest of selected area here;
        }

    }





    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }
}
