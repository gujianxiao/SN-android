package com.example.cb.sn_android;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;

/**
 * Created by cb on 16-11-9.
 */
public class WSNArea {
    //location class use to locate a point

    private int leftDownLat;
    private int leftDownLng;
    private int rightUpLat;
    private int rightUpLng;
    private String dataType;

    public WSNArea(int leftDownLat, int leftDownLng, int rightUpLat, int rightUpLng, String dataType) {
        this.leftDownLat = leftDownLat;
        this.leftDownLng = leftDownLng;
        this.rightUpLat = rightUpLat;
        this.rightUpLng = rightUpLng;
        this.dataType=dataType;
    }

    public int getLeftDownLat() {
        return leftDownLat;
    }

    public void setLeftDownLat(int leftDownLat) {
        this.leftDownLat = leftDownLat;
    }

    public int getLeftDownLng() {
        return leftDownLng;
    }

    public void setLeftDownLng(int leftDownLng) {
        this.leftDownLng = leftDownLng;
    }

    public int getRightUpLat() {
        return rightUpLat;
    }


    public void setRightUpLat(int rightUpLat) {
        this.rightUpLat = rightUpLat;
    }

    public int getRightUpLng() {
        return rightUpLng;
    }

    public void setRightUpLng(int rightUpLng) {
        this.rightUpLng = rightUpLng;
    }


    public String getDataType(){
        return dataType;
    }

    public  void setDataType(String dataType){
        this.dataType=dataType;
    }





}
