package com.example.cb.sn_android;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by cb on 16-10-13.
 */
public class Sensory_service extends Service {
    private SensorManager sensorManager;
    private Sensor light;
    private Sensor temperature;
    private Sensor accelerometer;
    private static String TAG="sensory-service";



    private AlarmManager alarmManager;



    private SensorEventListener listener=new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float value=sensorEvent.values[0];
            Log.i(TAG, "onSensorChanged: light changed value is:"+value);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: create sensory service!");
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        light =sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        temperature=sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(listener,light,SensorManager.SENSOR_DELAY_NORMAL);


    }


    public class ServiceBinder extends Binder{
        public void startBind(){
            Log.i(TAG, "Start binding sensory service...success!");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
