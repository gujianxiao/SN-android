package com.example.cb.sn_android;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by cb on 17-6-15.
 */
public class WiFiDetect extends Activity {
    private String TAG="WiFiManagement";
    private WifiManager wifiManager;
    List<ScanResult> list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_wifi_list);
        init();
    }

    private void init() {
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        openWifi();
        list = wifiManager.getScanResults();
//        ListView listView = (ListView) findViewById(R.id.listView);
        if (list == null) {
            Log.i(TAG, "init: wifi未打开");
//            Toast.makeText(this, "wifi未打开！", Toast.LENGTH_LONG).show();
        }else {
            Log.i(TAG, "init: 多wifi覆盖！ 共"+list.size()+"个。");
            Log.i(TAG, "Name of NDN-IoT gateway:");
            Iterator iterator=list.iterator();
            while (iterator.hasNext()){
                Map.Entry entry = (Map.Entry) iterator.next();
                Log.i(TAG, entry.getKey().toString()+entry.getValue().toString());
            }
//            Toast.makeText(this, "多wifi覆盖！ 共"+list.size()+"个。", Toast.LENGTH_LONG).show();
//            listView.setAdapter(new MyAdapter(this,list));
        }

    }

    private void openWifi() {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

    }

//    public class MyAdapter extends BaseAdapter {
//
//        LayoutInflater inflater;
//        List<ScanResult> list;
//        public MyAdapter(Context context, List<ScanResult> list) {
//            // TODO Auto-generated constructor stub
//            this.inflater = LayoutInflater.from(context);
//            this.list = list;
//        }
//
//        @Override
//        public int getCount() {
//            // TODO Auto-generated method stub
//            return list.size();
//        }
//        @Override
//        public Object getItem(int position) {
//            // TODO Auto-generated method stub
//            return position;
//        }
//
//        @Override
//        public long getItemId(int position) {
//            // TODO Auto-generated method stub
//            return position;
//        }
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            // TODO Auto-generated method stub
//            View view = null;
//            view = inflater.inflate(R.layout.item_wifi_list, null);
//            ScanResult scanResult = list.get(position);
//            TextView textView = (TextView) view.findViewById(R.id.textView);
//            textView.setText(scanResult.SSID);
//            TextView signalStrenth = (TextView) view.findViewById(R.id.signal_strenth);
//            signalStrenth.setText(String.valueOf(Math.abs(scanResult.level)));
//            ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
//            //判断信号强度，显示对应的指示图标
//            if (Math.abs(scanResult.level) > 100) {
//                imageView.setImageDrawable(getResources().getDrawable(R.drawable.stat_sys_wifi_signal_0));
//            } else if (Math.abs(scanResult.level) > 80) {
//                imageView.setImageDrawable(getResources().getDrawable(R.drawable.stat_sys_wifi_signal_1));
//            } else if (Math.abs(scanResult.level) > 70) {
//                imageView.setImageDrawable(getResources().getDrawable(R.drawable.stat_sys_wifi_signal_1));
//            } else if (Math.abs(scanResult.level) > 60) {
//                imageView.setImageDrawable(getResources().getDrawable(R.drawable.stat_sys_wifi_signal_2));
//            } else if (Math.abs(scanResult.level) > 50) {
//                imageView.setImageDrawable(getResources().getDrawable(R.drawable.stat_sys_wifi_signal_3));
//            } else {
//                imageView.setImageDrawable(getResources().getDrawable(R.drawable.stat_sys_wifi_signal_4));
//            }
//            return view;
//        }
//
//    }

}