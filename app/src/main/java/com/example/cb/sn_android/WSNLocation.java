package com.example.cb.sn_android;

import com.baidu.mapapi.model.LatLng;

/**
 * Created by cb on 16-10-28.
 */
public class WSNLocation {
    //location class use to locate a point

        private int latitude;
        private int longitude;
        private String dataType;
        private int leftDownLat;
        private int leftDownLng;
        private int rightUpLat;
        private int rightUpLng;
        private LatLng leftDown;
        private LatLng rightUp;

    public WSNLocation(LatLng leftDown, LatLng rightUp, String dataType) {
        this.leftDown = leftDown;
        this.rightUp = rightUp;
        this.dataType = dataType;
    }

    public LatLng getLeftDown() {
        return leftDown;
    }

    public void setLeftDown(LatLng leftDown) {
        this.leftDown = leftDown;
    }

    public LatLng getRightUp() {
        return rightUp;
    }

    public void setRightUp(LatLng rightUp) {
        this.rightUp = rightUp;
    }

    public WSNLocation(int leftDownLat, int leftDownLng, int rightUpLat, int rightUpLng) {
        this.rightUpLat = rightUpLat;
        this.leftDownLat = leftDownLat;
        this.leftDownLng = leftDownLng;
        this.rightUpLng = rightUpLng;
    }

    public WSNLocation(int leftDownLat, int leftDownLng, int rightUpLat, int rightUpLng, String dataType) {
        this.leftDownLat = leftDownLat;
        this.leftDownLng = leftDownLng;
        this.rightUpLat = rightUpLat;
        this.rightUpLng = rightUpLng;
        this.dataType = dataType;
    }

    public int getLeftDownLat() {
        return leftDownLat;
    }

    public int getLeftDownLng() {
        return leftDownLng;
    }

    public int getRightUpLat() {
        return rightUpLat;
    }

    public int getRightUpLng() {
        return rightUpLng;
    }

    public void setLeftDownLat(int leftDownLat) {
        this.leftDownLat = leftDownLat;
    }

    public void setLeftDownLng(int leftDownLng) {
        this.leftDownLng = leftDownLng;
    }

    public void setRightUpLat(int rightUpLat) {
        this.rightUpLat = rightUpLat;
    }

    public void setRightUpLng(int rightUpLng) {
        this.rightUpLng = rightUpLng;
    }

    public WSNLocation(int lat, int lng){
            latitude=lat;
            longitude=lng;
            dataType=null;
        }

        public WSNLocation(int lat, int lng, String dt){
            latitude=lat;
            longitude=lng;
            dataType=dt;
        }


        public int getLatitude(){
            return latitude;
        }

        public int getLongitude(){
            return longitude;
        }

        public String getDataType(){ return dataType; }

        public boolean setLatitude(int la){
            latitude=la;
            return true;
        }

        public boolean setLongitude(int lng){
            longitude=lng;
            return true;
        }
        public boolean setDataType(String dt){
            dataType=dt;
            return true;
        }



}
