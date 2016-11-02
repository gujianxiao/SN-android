package com.example.cb.sn_android;

/**
 * Created by cb on 16-10-28.
 */
public class WSNLocation {
    //location class use to locate a point

        private int latitude;
        private int longitude;
        private String dataType;

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

}
