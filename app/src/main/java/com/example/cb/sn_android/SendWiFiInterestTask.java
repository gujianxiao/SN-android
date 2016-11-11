package com.example.cb.sn_android;

import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by cb on 16-11-11.
 */
public class SendWiFiInterestTask extends AsyncTask <HashMap<Integer,WiFiLocation>,Integer,Boolean>{


    private final String TAG="SendWiFiInterestTask";
    private HashMap<Integer,Data> comeBackData = new HashMap<>();
    //   private Data comeBackData[]=null;
    private Callback callback;
    private int callbackCount=0;
    private int dataCount=0;
    interface Callback{
        public void updateUI(HashMap<Integer,Data> updataBase);
    }
    public SendWiFiInterestTask(){}
    public SendWiFiInterestTask(Callback callback){
        this.callback = callback;
    }
    @Override
    protected Boolean doInBackground(HashMap<Integer, WiFiLocation>... base) {
        Log.i(TAG,"doInBackground............");
        Face face=new Face();
        if(base[0].size()>1&&base[0].get(1000)==null){
            WiFiLocation nodeWiFiLocation;
            Iterator iterator=base[0].entrySet().iterator();
            try {
                while(iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    int key = (int) entry.getKey();
                    nodeWiFiLocation = (WiFiLocation) entry.getValue();
                    Name interestOfNode = new Name("/wifi/" + nodeWiFiLocation.getLeftDown().latitude +","+nodeWiFiLocation.getLeftDown().longitude+"/"+nodeWiFiLocation.getRightUp().latitude+","+nodeWiFiLocation.getRightUp().longitude+"/0/10000/"+nodeWiFiLocation.getDataType());
                    incomingData incomD = new incomingData();
                    face.expressInterest(interestOfNode, incomD, incomD);
                }



                while (callbackCount < base[0].size()) {

                    face.processEvents();

                    // We need to sleep for a few milliseconds so we don't use
                    // 100% of

                    // the CPU.

                    Thread.sleep(50);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        else if (base[0].size()==1&&base[0].get(1000)!=null){
            WiFiLocation nodeWiFiLocation;
            Iterator iterator=base[0].entrySet().iterator();
            Map.Entry entry=(Map.Entry)iterator.next();
            int key=(int)entry.getKey();
            nodeWiFiLocation =(WiFiLocation)entry.getValue();
            Name interestOfNode=new Name("/wifi/"+ nodeWiFiLocation.getLeftDown().latitude+ ","+nodeWiFiLocation.getLeftDown().longitude+"/"+ nodeWiFiLocation.getRightUp().latitude+","+ nodeWiFiLocation.getRightUp().longitude+"/0/10000/"+nodeWiFiLocation.getDataType() );
            Log.i(TAG, "send interest "+interestOfNode.toString());
            incomingData incomD=new incomingData();
            try {
                face.expressInterest(interestOfNode,incomD,incomD);
                while (callbackCount<1) {

                    face.processEvents();

                    // We need to sleep for a few milliseconds so we don't use
                    // 100% of

                    // the CPU.

                    Thread.sleep(50);

                }
            }
            catch (Exception e){
                e.printStackTrace();
            }



        }

        else if (base[0].size()==1&&base[0].get(1000)==null){
            WiFiLocation nodeWiFiLocation;
            Iterator iterator=base[0].entrySet().iterator();
            Map.Entry entry=(Map.Entry)iterator.next();
            int key=(int)entry.getKey();
            nodeWiFiLocation =(WiFiLocation)entry.getValue();
            long currentTimeStart=System.currentTimeMillis();
            long currentTimeEnd=currentTimeStart-10;
            Name interestOfNode=new Name("/wifi/"+ nodeWiFiLocation.getLeftDown().latitude+ ","+nodeWiFiLocation.getLeftDown().longitude+"/"+ nodeWiFiLocation.getRightUp().latitude+","+ nodeWiFiLocation.getRightUp().longitude+"/"+currentTimeStart+"/"+currentTimeEnd+"/"+nodeWiFiLocation.getDataType() );
            Log.i(TAG, "send interest "+interestOfNode.toString());
            incomingData incomD=new incomingData();
            try {
                face.expressInterest(interestOfNode,incomD,incomD);
                while (callbackCount<1) {

                    face.processEvents();

                    // We need to sleep for a few milliseconds so we don't use
                    // 100% of

                    // the CPU.

                    Thread.sleep(50);

                }
            }
            catch (Exception e){
                e.printStackTrace();
            }



        }

        return null;
    }

    @Override
    protected void onPreExecute() {
        Log.i(TAG,"onPreExecute.................");
        super.onPreExecute();
    }



    @Override
    protected void onPostExecute(Boolean o) {
        Log.i(TAG, "onPostExecute: execute async task of send interest success!");
        if(this.callback!=null){
            this.callback.updateUI(comeBackData);
        }
        //   Toast.makeText(MainActivityV2.,"Send Interest task success!",Toast.LENGTH_LONG).show();
        super.onPostExecute(o);
    }









//    public class NetThread extends Thread{
//        public NetThread(){
//
//        }
//
//        @Override
//        public void run() {
//            super.run();
//            Face face=new Face();
//
//        }
//    }

    private class incomingData implements OnData,OnTimeout {
        @Override
        public void onTimeout(Interest interest) {
            callbackCount++;
            Log.i(TAG, "Time out for interest:" + interest.getName().toUri());
        }

        @Override
        public void onData(Interest interest, Data data) {
            callbackCount++;
            Log.i(TAG, "Got data packet with name:" + data.getName().toUri());
            String msg = data.getContent().toString();
            Log.i(TAG, "onData: " + msg);
            if (msg.length()==0) {
                Log.i(TAG, "Data is null");
            } else if (msg.length()>0) {
                comeBackData.put(dataCount, data);
                dataCount++;
            }
        }

    }
}
