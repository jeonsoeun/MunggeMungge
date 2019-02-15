package com.example.jiheepyo.uxd;

/**
 * Created by JeonSoEun on 2016-11-30.
 */

public class Place{
    int m_type; //0:흡연부스, 1:카페, 2:pc방, 3:인기 실외구역
    String m_name;
    double m_latitude;
    double m_longitude;
    Place(int type, String name, double lati, double longi){
        m_type = type;
        m_name = name;
        m_latitude = lati;
        m_longitude = longi;
    }
    public String getName(){return m_name;}
    public double getLatitude(){return m_latitude;}
    public double getLongitude(){return m_longitude;}
    public int getType(){return  m_type;}
    public void setName(String name){m_name = name;}
    public void setType(int type){m_type = type;}
    public void setLati(double lati){m_latitude = lati;}
    public void setLongi(double longi){m_longitude = longi;}
}
