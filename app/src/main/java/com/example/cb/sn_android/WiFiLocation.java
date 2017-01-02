package com.example.cb.sn_android;

import com.baidu.mapapi.model.LatLng;

/**
 * Created by cb on 16-11-11.
 */
public class WiFiLocation {
    private LatLng point;
    private LatLng leftDown;
    private LatLng rightUp;
    private String dataType;
    private String deviceName;


    public WiFiLocation(LatLng point, String deviceName) {
        this.point = point;
        this.deviceName = deviceName;
    }

    public WiFiLocation(LatLng leftDown, LatLng rightUp, String deviceName) {
        this.leftDown = leftDown;
        this.rightUp = rightUp;
        this.deviceName = deviceName;
    }

    public WiFiLocation(LatLng leftDown, LatLng rightUp, String deviceName, String dataType) {
        this.leftDown = leftDown;
        this.rightUp = rightUp;
        this.deviceName = deviceName;
        this.dataType = dataType;
    }



    public LatLng getRightUp() {
        return rightUp;
    }

    public void setRightUp(LatLng rightUp) {
        this.rightUp = rightUp;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public LatLng getLeftDown() {
        return leftDown;
    }

    public String getDataType() {
        return dataType;
    }

    public void setLeftDown(LatLng leftDown) {
        this.leftDown = leftDown;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }


    public LatLng getPoint() {
        return point;
    }

    public void setPoint(LatLng point) {
        this.point = point;
    }
}
