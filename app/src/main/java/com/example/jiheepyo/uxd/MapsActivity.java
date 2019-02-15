package com.example.jiheepyo.uxd;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;

class ToIndexSort implements Comparable<ToIndexSort>{
    int m_index;
    double m_distant;

    ToIndexSort(int index, double distant) {
        m_index = index;
        m_distant = distant;
    }
    public double getDistance(){return m_distant;}
    public int getIndex(){return m_index;}

    @Override
    public int compareTo(ToIndexSort another) {
        return new Double(m_distant).compareTo(another.m_distant)*10000;
    }
}

public class MapsActivity extends Activity implements OnMapReadyCallback {

    GoogleMap map;
    MapFragment mapFr;
    LocationManager locationManager;
    // 현재 GPS 사용유무
    boolean isGPSEnabled = false;
    // 네트워크 사용유무
    boolean isNetworkEnabled = false;
    // GPS 상태값
    boolean isGetLocation = false;
    //현재위치
    Location nowLocation;
    // GPS 정보 업데이트 거리 10미터
    private static final long MIN_DISTANCE_UPDATES = 10;
    // GPS 정보 업데이트 시간 1/1000
    private static final long MIN_TIME_UPDATES = 1000 * 60 * 1;

    //Spinner spinner;
    //ArrayList<String> m_data;
    //ArrayAdapter<String> m_adapter; // 지도 종류 선택을 위한 것

    ArrayList<Place> m_smokingPlace;//흡연구역 저장.-소팅후.
    ListAdapter m_placeAdapter;//흡연구역 리스트 어뎁터.
    ListView listView;
    LinearLayout listLayout;
    ArrayList<Place> tempStore;//흡연구역 소팅전
    ToggleButton listToggle;//리스트 볼지 말지 토글버튼.
    ToggleButton nonSmokeToggle;//금연구역 보기 버튼.
    Button checkboxBtn;//체크박스 창여는 버튼.

    double camLati=0,camLongi=0;//현재 카메라 위치.
    boolean checkList[]={true,true,true,true};//체크박스 뭐뭐 되있나.
    Context context;

    boolean doubleBackToExitPressedOnce = false;

    int strokewidth =  1; // polygon과 circle의 굵기
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        startActivity(new Intent(this, SplashActivity.class)); // 스플래시 받아주는 구문
        context = this;
        initData();
        init();
    }

    void init() {
        // 맵의 형태를 선택하는 예제제
        mapFr = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFr.getMapAsync(this);//비동기식 맵서비스 제공
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        m_smokingPlace = new ArrayList<Place>();
        m_smokingPlace.addAll(tempStore);

        listLayout = (LinearLayout) findViewById(R.id.listLayout);
        listLayout.setVisibility(View.GONE);
        listView = (ListView) findViewById(R.id.spotList);
        m_placeAdapter = new ListAdapter(this, R.layout.list_row, m_smokingPlace);
        m_placeAdapter.setNotifyOnChange(true);
        listView.setAdapter(m_placeAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //마커 찍는거 넣기
                //resetMap();
                //맵 이동.
                updateAnimateMap(m_smokingPlace.get(position).m_latitude, m_smokingPlace.get(position).m_longitude);
            }
        });

        listToggle = (ToggleButton) findViewById(R.id.toggleList);
        listToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listToggle.isChecked()) {
                    ViewGroup.LayoutParams params = mapFr.getView().getLayoutParams();
                    params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350, getResources().getDisplayMetrics());//dp로 쓰기위해.
                    mapFr.getView().setLayoutParams(params);
                    listLayout.setVisibility(View.VISIBLE);
                    setList(tempStore,camLati,camLongi);
                } else {
                    ViewGroup.LayoutParams params = mapFr.getView().getLayoutParams();
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    mapFr.getView().setLayoutParams(params);
                    listLayout.setVisibility(View.GONE);
                }
            }
        });
        checkboxBtn = (Button)findViewById(R.id.btnCheck);
        checkboxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context,PopupActivity.class);
                intent.putExtra("checkedList",checkList);
                startActivityForResult(intent,1);
            }
        });
        nonSmokeToggle = (ToggleButton)findViewById(R.id.toggleNonSmoke);
        nonSmokeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nonSmokeToggle.isChecked()){
                    showNonSmokingArea();//금연구역 표시해줌.
                }else{
                    map.clear();//지도 클리어
                    //금연구역 말고 나머지는 화면에 다시 띄워줘야된다.
                    //resetMap();
                    showSmokingArea();
                }
            }
        });
//        
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1){
            if(resultCode==RESULT_OK){
                boolean changeCheckList[] = data.getBooleanArrayExtra("result");
                for(int i =0; i<checkList.length; i++){
                    //팝업에 적용버튼이 눌렸을때 적용시킴.
                    checkList[i] = changeCheckList[i];
                    setList(tempStore,camLati,camLongi);
                }
                resetMap();
            }else if(resultCode == RESULT_CANCELED){
                    //취소버튼이면 아무것도 안함.
            }
        }
    }
    //데이터 초기화. 흡연구역 데이터를 받아서 배열에 저장한다.
    void initData() {
        tempStore = new ArrayList<Place>();
        String file = "smoke.xml";
        String result = "";
        try{
            InputStream is = getAssets().open(file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            result = new String(buffer, "utf-8");
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp= factory.newPullParser();
            xpp.setInput(new StringReader(result));

            int eventType= xpp.getEventType();
            boolean done = false;
            int type = 0;
            String name = "";
            double lati = 0.0;
            double longi = 0.0;
            while(eventType!= XmlPullParser.END_DOCUMENT && !done)
            {
                if(eventType==XmlPullParser.START_DOCUMENT)
                {
                    ;
                }
                else if(eventType== XmlPullParser.START_TAG)
                {
                    String tag_name= xpp.getName();
                    if(tag_name.equals("type")) {
                        String data= xpp.nextText();
                        type = Integer.parseInt(data);
                    }
                    else if ( tag_name.equals("name")) {
                        String data = xpp.nextText();
                        name = data;
                    }
                    else if (tag_name.equals("latitude")) {
                        String data = xpp.nextText();
                        lati=Double.parseDouble(data);
                    }
                    else if (tag_name.equals("longitude")) {
                        String data = xpp.nextText();
                        longi = Double.parseDouble(data);
                    }
                }
                else if(eventType==XmlPullParser.END_TAG)
                {
                    String tag = xpp.getName();
                    if(tag.equals("smokearea")) {
                        Place p = new Place(type,name,lati,longi);
                        tempStore.add(p);
                    }
                    else if(tag.equals("smoke")){
                        done = true;
                    }
                }
                eventType= xpp.next();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error on xml", Toast.LENGTH_SHORT).show();
        }

    }

    // 위도 경도를 이용해서 거리구하기. (미터로 바꿔줌)
    public double calDistance(double lat1, double lon1, double lat2, double lon2){
        double theta, dist;
        theta = lon1 - lon2;
        dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);

        dist = dist * 60 * 1.1515;
        dist = dist * 1.609344;    // 단위 mile 에서 km 변환.
        dist = dist * 1000.0;      // 단위  km 에서 m 로 변환

        return dist;
    }

    // 주어진 도(degree) 값을 라디언으로 변환
    private double deg2rad(double deg){
        return (double)(deg * Math.PI / (double)180d);
    }

    // 주어진 라디언(radian) 값을 도(degree) 값으로 변환
    private double rad2deg(double rad){
        return (double)(rad * (double)180d / Math.PI);
    }

    public void setList(ArrayList<Place> list,double camLati,double camLongi){
        ArrayList<ToIndexSort> tempStoreData = new ArrayList<ToIndexSort>();
        m_smokingPlace.clear();

        for (int i = 0; i < list.size(); i++) {
            //double dist = Math.sqrt(Math.pow(list.get(i).m_latitude-camLati,2)+Math.pow(list.get(i).m_longitude-camLongi,2));
            double dist = calDistance(list.get(i).m_latitude,list.get(i).m_longitude,camLati,camLongi);
            ToIndexSort item = new ToIndexSort(i,dist);

            tempStoreData.add(item);
        }
        Collections.sort(tempStoreData);
        for(int i = 0 ; i<list.size(); i++){
            if(checkList[list.get(tempStoreData.get(i).m_index).m_type])
                m_smokingPlace.add(list.get(tempStoreData.get(i).m_index));
        }
        m_placeAdapter.notifyDataSetChanged();
    }
    //카메라 이동이 있을때 스르륵이동.
    public void updateAnimateMap(double latitude, double longitude) {
        //double latitude = location.getLatitude();
        //double longitude = location.getLongitude();
        final LatLng Loc = new LatLng(latitude, longitude);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(Loc, 16));
    }
    //카메라 이동이 있을때 바로이동.
    public void updateMap(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        final LatLng Loc = new LatLng(latitude, longitude);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(Loc, 16));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMinZoomPreference(15.0f);
        // 현재 위치 버튼 코드
        map.setMapType(googleMap.MAP_TYPE_NORMAL);
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            map.setMyLocationEnabled(true);
        }
        //카메라가 움직이고 나면 호출.
        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                camLati = map.getCameraPosition().target.latitude;
                camLongi = map.getCameraPosition().target.longitude;
                if(listToggle.isChecked())
                    setList(tempStore,camLati,camLongi);
                //resetMap();
            }
        });
        // 새천년관에서 카메라 줌으로 시작
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        double latitude = 37.543564;
        double longitude = 127.077431;

        final LatLng Loc = new LatLng(latitude, longitude);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(Loc, 16));

        //camLati = map.getCameraPosition().target.latitude;
        //camLongi = map.getCameraPosition().target.longitude;
        resetMap();
    }
    public void resetMap(){
        map.clear();
        camLati = map.getCameraPosition().target.latitude;
        camLongi = map.getCameraPosition().target.longitude;
        showSmokingArea();
        if(nonSmokeToggle.isChecked())
            showNonSmokingArea();
    }

    //흡연구역 보여주기.
    public void showSmokingArea(){
        for(int i = 0; i<m_smokingPlace.size(); i++){
            if(m_smokingPlace.get(i).m_type==0){
                if(checkList[0]){
                    LatLng location = new LatLng(m_smokingPlace.get(i).m_latitude,m_smokingPlace.get(i).m_longitude);
                    map.addMarker(new MarkerOptions().position(location).title(m_smokingPlace.get(i).getName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_booth)));
                }
            }else if(m_smokingPlace.get(i).m_type==1){
                if(checkList[1]){
                    LatLng location = new LatLng(m_smokingPlace.get(i).m_latitude,m_smokingPlace.get(i).m_longitude);
                    map.addMarker(new MarkerOptions().position(location).title(m_smokingPlace.get(i).getName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_pc)));
                }
            }else if(m_smokingPlace.get(i).m_type==2){
                if(checkList[2]){
                    LatLng location = new LatLng(m_smokingPlace.get(i).m_latitude,m_smokingPlace.get(i).m_longitude);
                    map.addMarker(new MarkerOptions().position(location).title(m_smokingPlace.get(i).getName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe)));
                }
            }else if(m_smokingPlace.get(i).m_type==3){
                if(checkList[3]){
                    LatLng location = new LatLng(m_smokingPlace.get(i).m_latitude,m_smokingPlace.get(i).m_longitude);
                    map.addMarker(new MarkerOptions().position(location).title(m_smokingPlace.get(i).getName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_outside)));
                }
            }
        }
    }

    //거리 비교식.
    //double dist = calDistance(Sub_X[i], Sub_Y[i],camLati,camLongi);
    //if(dist<2600) {}

    //금연구역 표시.
    public void showNonSmokingArea() {
        // <-- 금연구역 polygon 표시 -->
        // [1. 공원]
        // 어린이대공원
        map.addPolygon(new PolygonOptions()
                .add(new LatLng(37.547465, 127.074475),
                        new LatLng(37.551834, 127.076825),
                        new LatLng(37.551679, 127.077376),
                        new LatLng(37.552951, 127.080488),

                        new LatLng(37.550341, 127.083650),
                        new LatLng(37.548266, 127.085285),
                        new LatLng(37.545926, 127.083933),

                        new LatLng(37.545632, 127.079234),

                        new LatLng(37.547465, 127.074475)
                )
                .fillColor(Color.argb(80, 100, 0, 0))
                .strokeWidth(strokewidth)
                .strokeColor(Color.RED));


        // <-- 금연구역 Circle 표시 -->
        // [2. 지하철 출구]
        Double[] Sub_X = new Double[]{37.540754, 37.540120, 37.540854, 37.540011, 37.539846, 37.540533, 37.536985, 37.537453, 37.537292, 37.536773, 37.535233, 37.534821, 37.534595, 37.545457, 37.545347, 37.544645, 37.544709, 37.566092, 37.566145, 37.532094, 37.530910, 37.530992, 37.531841, 37.548987, 37.547205, 37.547078, 37.546980, 37.547680, 37.548841};
        Double[] Sub_Y = new Double[]{127.068694, 127.070470, 127.071283, 127.071132, 127.070328, 127.068300, 127.085109, 127.087110, 127.087201, 127.085157, 127.094844, 127.095127, 127.094876, 127.103593, 127.104344, 127.104118, 127.103668, 127.084722, 127.084295, 127.067147, 127.066578, 127.066273, 127.066349, 127.075430, 127.074716, 127.074460, 127.073830, 127.074254, 127.075022};

        for (int i = 0; i < 29; i++) {

            map.addCircle(new CircleOptions()
                    .center(new LatLng(Sub_X[i], Sub_Y[i]))
                    .fillColor(Color.argb(80, 100, 0, 0))
                    .strokeColor(Color.RED)
                    .strokeWidth(strokewidth)
                    .radius(10));

        }


        // <-- 금연구역 실내 구역 표시 -->
        // [3. 공공기관]

        String[] Public_Name = new String[]{"광진우체국", "광진소방서", "광진경찰서(임시청사)", "서울지방검찰청 동부지청", "서울지방법원 동부지원", "광진구 시설관리공단", "광장동 주민자치센터", "구의1동 주민자치센터", "구의3동 주민자치센터", "구의2동 주민자치센터", "군자동 주민자치센터", "능동 주민자치센터", "광진구청 ", "자양3동 주민자치센터", "자양1동 주민자치센터", "자양4동 주민자치센터", "자양2동 주민자치센터", "광진구의회", "중곡1동 주민자치센터", "중곡2동 주민자치센터", "중곡4동 주민자치센터", "중곡3동 주민자치센터", "화양동 주민자치센터"};
        Double[] Public_X = new Double[]{37.5377348, 37.5449282, 37.5464996, 37.5369293, 37.5367626, 37.5376641, 37.546845, 37.542456, 37.538066, 37.5472058, 37.5551213, 37.5537327, 37.5383734, 37.5337905, 37.5383727, 37.5340959, 37.5287913, 37.5372067, 37.5605114, 37.5601783, 37.5590531, 37.568753, 37.5465112};
        Double[] Public_Y = new Double[]{127.0882707
                , 127.0806046
                , 127.0700511
                , 127.0857987
                , 127.0853543
                , 127.0683809
                , 127.1008247
                , 127.0834653
                , 127.0896917
                , 127.0877149
                , 127.073716
                , 127.0783267
                , 127.0734529
                , 127.0707169
                , 127.0734529
                , 127.0641065
                , 127.0821882
                , 127.0680782
                , 127.077864
                , 127.0792841
                , 127.0872231
                , 127.0779427
                , 127.0691056};
        for (int i = 0; i < 23; i++) {
            Public_Y[i] = Public_Y[i] + 0.01;
            final LatLng Loc1 = new LatLng(Double.valueOf(Public_X[i]).doubleValue(), Double.valueOf(Public_Y[i]).doubleValue());
            map.addMarker(new MarkerOptions().position(Loc1).title(Public_Name[i]).icon(BitmapDescriptorFactory.fromResource(R.drawable.smokex)));


        }


        // <-- 금연구역 실내 구역 표시 -->
        // [4. 유치원, 초등학교, 고등학교, 대학교]


        String[] Sch_Name = new String[]{
                "보성유치원"
                , "광장유치원"
                , "광남유치원"
                , "청구이화유치원"
                , "경원유치원"
                , "영화유치원"
                , "샬롬유치원"
                , "현정유치원"
                , "동인유치원"
                , "슬기유치원"
                , "프라임유치원"
                , "선양유치원"
                , "선미유치원"
                , "동심유치원"
                , "어린이회관유치원"
                , "선화유치원"
                , "서울신자초등학교병설유치원"
                , "서울양진초등학교병설유치원"
                , "서울자양초등학교병설유치원"
                , "자양하나유치원"
                , "한일유치원"
                , "성수아름유치원"
                , "인애유치원"
                , "대원유치원"
                , "성산유치원"
                , "자양유치원"
                , "서울양남초등학교병설유치원"
                , "선경유치원"
                , "정신유치원"
                , "예일유치원"
                , "경하유치원"
                , "아이조아유치원"
                , "금빛유치원"
                , "미파유치원"
                , "서울구의초등학교병설유치원"
                , "윤진유치원"
                , "광장초등학교"
                , "광남초등학교"
                , "양진초등학교"
                , "동의초등학교"
                , "광진초등학교"
                , "구남초등학교"
                , "장안초등학교"
                , "세종초등학교"
                , "경복초등학교"
                , "자양초등학교"
                , "신자초등학교"
                , "성자초등학교"
                , "양남초등학교"
                , "성동초등학교"
                , "신양초등학교"
                , "동자초등학교"
                , "용마초등학교"
                , "중마초등학교"
                , "중광초등학교"
                , "용곡초등학교"
                , "화양초등학교"
                , "구의초등학교"
                , "재한몽골학교"
                , "광진학교"
                , "한국켄트외국인학교"
                , "광장중학교"
                , "광남중학교"
                , "양진중학교"
                , "동대사대부속여자중학교"
                , "선화예술중학교"
                , "건대부속중학교"
                , "광양중학교"
                , "광진중학교"
                , "자양중학교"
                , "신양중학교"
                , "용곡중학교"
                , "대원중학교"
                , "구의중학교"
                , "광남고등학교"
                , "동국대학사범대부속고등학교"
                , "선화예술고등학교"
                , "건대부속고등학교"
                , "광양고등학교"
                , "자양고등학교"
                , "대원고등학교"
                , "대원여자고등학교"
                , "대원외국어고등학교"
                , "장로회신학대학교"
                , "세종대학교"
                , "건국대학교"
        };
        Double[] Sch_X = new Double[]{
                37.5418039
                , 37.5484282
                , 37.541429
                , 37.5431231
                , 37.5548717
                , 37.5541218
                , 37.5424009
                , 37.5444284
                , 37.5436817
                , 37.683966
                , 37.5387626
                , 37.554288
                , 37.5476966
                , 37.3624353
                , 37.5475945
                , 37.5495909
                , 37.5300132
                , 37.5442619
                , 37.5367903
                , 37.538762
                , 37.5372292
                , 37.5364011
                , 37.5337627
                , 35.8997794
                , 37.4455717
                , 37.5311521
                , 37.5310134
                , 37.532902
                , 37.5372344
                , 37.5311469
                , 37.5660644
                , 37.5661478
                , 37.4491022
                , 37.5704529
                , 37.5428173
                , 37.5427615
                , 37.5482972
                , 37.4674323
                , 37.5442975
                , 37.5536219
                , 37.547678
                , 37.5371516
                , 37.5503163
                , 37.2796372
                , 37.5494685
                , 37.5385105
                , 37.5299855
                , 37.531097
                , 37.5307356
                , 37.5328187
                , 35.3336465
                , 37.5344849
                , 37.5579267
                , 37.5660922
                , 37.5651202
                , 37.5667312
                , 37.5489092
                , 37.5429916
                , 37.5470394
                , 37.5373182
                , 37.5470947
                , 37.547095
                , 37.5402625
                , 37.5447896
                , 37.5422897
                , 37.5504276
                , 37.5407636
                , 37.5302912
                , 37.531819
                , 37.5350518
                , 37.5334015
                , 37.5660646
                , 37.5637594
                , 37.5428173
                , 37.5413179
                , 37.5425104
                , 37.5498641
                , 37.5419708
                , 37.5294579
                , 37.5354321
                , 37.5631761
                , 37.5623706
                , 37.5636716
                , 37.5505112
                , 37.549035
                , 37.5407625
        };
        Double[] Sch_Y = new Double[]{
                127.0994639
                , 127.1029356
                , 127.0983807
                , 127.0934367
                , 127.0939085
                , 127.0952973
                , 127.0896593
                , 127.0867151
                , 127.0829708
                , 127.0460203
                , 127.0961588
                , 127.0689943
                , 126.8882795
                , 126.3900504
                , 127.0749384
                , 127.0853889
                , 127.0733001
                , 127.0962419
                , 127.0773273
                , 127.0763833
                , 127.0296092
                , 127.0601901
                , 127.0661896
                , 128.6037422
                , 126.718025
                , 127.0815493
                , 127.0875209
                , 127.0873265
                , 127.0624399
                , 127.0487186
                , 127.0759933
                , 127.0782708
                , 127.2473449
                , 127.0836591
                , 127.0773549
                , 127.0627175
                , 127.0988806
                , 127.0960393
                , 127.0962347
                , 127.0980623
                , 127.0892147
                , 127.0900761
                , 126.9320796
                , 127.6315586
                , 127.0847876
                , 127.0740405
                , 127.0733279
                , 127.0809383
                , 127.0875209
                , 127.0873265
                , 129.0223145
                , 127.0703836
                , 127.0819096
                , 127.077882
                , 127.0801874
                , 127.0863811
                , 126.5117433
                , 127.0781603
                , 127.0988805
                , 127.0896872
                , 127.0841319
                , 127.0995193
                , 127.099214
                , 127.0960197
                , 127.0844376
                , 127.0851873
                , 127.0800164
                , 127.0836324
                , 127.0874373
                , 127.0723022
                , 127.0610235
                , 127.0866867
                , 127.08727
                , 127.0770772
                , 127.0996861
                , 127.0780236
                , 127.0851799
                , 127.0679737
                , 127.0830214
                , 127.0640817
                , 127.0872978
                , 127.0872145
                , 127.0869531
                , 127.101269
                , 127.0729033
                , 127.0771541
        };


        for (int i = 0; i < 86; i++) {
            Sch_Y[i] = Sch_Y[i]+0.002188;
            if(i == 77){
                Sch_Y[i] = 127.082144;
            }
            final LatLng Loc2 = new LatLng(Double.valueOf(Sch_X[i]).doubleValue(), Double.valueOf(Sch_Y[i]).doubleValue());
            map.addMarker(new MarkerOptions().position(Loc2).title(Sch_Name[i]).icon(BitmapDescriptorFactory.fromResource(R.drawable.smokex)));
        }


        // <-- 금연구역 실외 구역 표시 -->
        // [4. 놀이터 및 공원]

        String[] Park_Name = new String[]{
                "온달공원"
                , "평강공원"
                , "명성공원"
                , "산마루공원"
                , "구둘공원"
                , "무궁화공원"
                , "개나리공원"
                , "목련공원"
                , "푸른동산공원"
                , "구의공원"
                , "장미공원"
                , "구의가로휴식공원"
                , "군자공원"
                , "남일공원"
                , "능동정자마당공원"
                , "정말공원"
                , "자마장공원"
                , "장독골공원"
                , "금모래공원"
                , "언덕배기공원"
                , "양마장공원"
                , "동자공원"
                , "자양공원"
                , "성화공원"
                , "대림공원"
                , "약초원공원"
                , "노유산공원"
                , "약초원공원"
                , "중곡공원"
                , "한마음공원"
                , "배나무터공원"
                , "해오름공원"
                , "긴고랑공원"
                , "화양동정자마당공원"
                , "한아름공원"
                , "가중나무공원"
                , "해님공원"
                , "화양공원"
                , "워커힐아파트 놀이터"
                , "워커힐푸르지오아파트 놀이터"
                , "광장 극동2차아파트 놀이터"
                , "광장 극동1차아파트 놀이터"
                , "신동아파밀리에아파트 놀이터"
                , "광장 삼성2차아파트 놀이터"
                , "광장 현대3단지아파트 놀이터"
                , "광장 현대5단지아파트 놀이터"
                , "광장 삼성1차아파트 놀이터"
                , "광장 현대8단지아파트 놀이터"
                , "광장 청구아파트 놀이터"
                , "광나루현대아파트 놀이터"
                , "상록타워 아파트 놀이터"
                , "광장 현대9단지아파트 놀이터"
                , "광장 현대파크빌 놀이터"
                , "광장 금호베스트빌아파트 놀이터"
                , "광장11차현대홈타운 놀이터"
                , "광장 현대12차아파트 놀이터"
                , "광장 자이아파트 놀이터"
                , "광장 현대힐스테이트 놀이터"
                , "강변우성아파트 놀이터"
                , "구의 현대7단지아파트 놀이터"
                , "구의 현대6단지아파트 놀이터"
                , "구의 현대2단지아파트 놀이터"
                , "구의현대프라임아파트 놀이터"
                , "새한아파트 놀이터"
                , "구의동 세양아파트 놀이터"
                , "세림리오빌아파트 놀이터"
                , "구의 현진에버빌아파트 놀이터"
                , "아차산한라녹턴아파트 놀이터"
                , "아차산휴먼시아아파트 놀이터"
                , "일성파크아파트 놀이터"
                , "능동소공원"
                , "더샵스타시티아파트 놀이터"
                , "자양우성6차아파트 놀이터"
                , "자양 우성3차아파트 놀이터"
                , "자양우성2차아파트 놀이터"
                , "자양 우성5차아파트 놀이터"
                , "자양동 한강극동아파트 놀이터"
                , "자양 우성4차아파트 놀이터"
                , "자양1차우성아파트 놀이터"
                , "자양경남아파트 놀이터"
                , "자양 현대5차아파트 놀이터"
                , "로얄동아아파트 놀이터"
                , "자양 현대아파트 놀이터"
                , "경남아너스빌 아파트 놀이터"
                , "자양 현대3차아파트 놀이터"
                , "한솔리베르아파트 놀이터"
                , "현대강변아파트 놀이터"
                , "자양한양아파트 놀이터"
                , "자양현대2차아파트 놀이터"
                , "자양한라아파트 놀이터"
                , "자양 우성7차아파트 놀이터"
                , "자양 대동아파트 놀이터"
                , "자양 현대6차아파트 놀이터"
                , "자양동 삼성아파트 놀이터"
                , "자양7차 현대홈타운아파트 놀이터"
                , "자양8차 현대홈타운아파트 놀이터"
                , "자양9차현대홈타운 놀이터"
                , "금강KCC아파트 놀이터"
                , "자양동 우방아파트 놀이터"
                , "강변아이파크아파트 놀이터"
                , "광진하우스스토리 놀이터"
                , "한강현대아파트 놀이터"
                , "한화 꿈에그린아파트 놀이터"
                , "이튼타워리버5차아파트 놀이터"
                , "한강우성아파트 놀이터"
                , "중곡1차아파트 놀이터"
                , "중곡동 SK 아파트 놀이터"
                , "중곡동 성원아파트 놀이터"
                , "광덕아파트 놀이터"
                , "화양동 현대아파트 놀이터"
        };
        Double[] Park_X = new Double[]{
                37.5458634
                , 37.5479855
                , 37.541424
                , 37.5550475
                , 37.5481208
                , 37.5412468
                , 37.538013
                , 37.5405783
                , 37.5428532
                , 37.5369387
                , 37.5362728
                , 37.5364305
                , 37.5544681
                , 37.5559474
                , 37.5533954
                , 37.5332296
                , 37.5343065
                , 37.5356422
                , 37.530164
                , 37.5320183
                , 37.5308707
                , 37.5318659
                , 37.5335548
                , 37.5361767
                , 37.5333713
                , 37.5333713
                , 37.5391102
                , 37.532914
                , 37.5645362
                , 37.5595627
                , 37.5684529
                , 37.5568146
                , 37.5590279
                , 37.5465041
                , 37.5443368
                , 37.5463514
                , 37.5426933
                , 37.5415252
                , 37.5503259
                , 37.5496537
                , 37.5423647
                , 37.5418039
                , 37.546875
                , 37.542457
                , 37.541429
                , 37.5396708
                , 37.541423
                , 37.5418966
                , 37.5431231
                , 37.5471252
                , 37.5424177
                , 37.5424287
                , 37.5438971
                , 37.5438193
                , 37.5432843
                , 37.5448428
                , 37.5424862
                , 37.5435738
                , 37.5352343
                , 37.5366742
                , 37.5384522
                , 37.5380998
                , 37.5373365
                , 37.541024
                , 37.5375454
                , 37.5391282
                , 37.5425872
                , 37.5539324
                , 37.547882
                , 37.5512513
                , 37.5525498
                , 37.5376118
                , 37.5364805
                , 37.5359338
                , 37.5332767
                , 37.5348078
                , 37.5337093
                , 37.5351003
                , 37.534211
                , 37.5288924
                , 37.52829
                , 37.5281274
                , 37.5280105
                , 37.5295129
                , 37.5283621
                , 37.5280575
                , 37.5282457
                , 37.5323313
                , 37.5341526
                , 37.5353649
                , 37.5363249
                , 37.5363352
                , 37.5330093
                , 37.531978
                , 37.5329861
                , 37.5333999
                , 37.5338221
                , 37.5375702
                , 37.5311559
                , 37.5311684
                , 37.5299478
                , 37.5330165
                , 37.5335292
                , 37.5323276
                , 37.5323924
                , 37.5663087
                , 37.568895
                , 37.5711016
                , 37.5684244
                , 37.5443487
        };
        Double[] Park_Y = new Double[]{
                127.0952451
                , 127.0919548
                , 127.0779533
                , 127.08634
                , 127.0833569
                , 127.0856117
                , 127.083613
                , 127.0817408
                , 127.082314
                , 127.0858245
                , 127.0813722
                , 127.0845147
                , 127.0640498
                , 127.0649347
                , 127.0705493
                , 127.069573
                , 127.0734282
                , 127.0690601
                , 127.082429
                , 127.0790614
                , 127.079924
                , 127.0655041
                , 127.0610649
                , 127.0530768
                , 127.0544015
                , 127.0544015
                , 127.0597422
                , 127.057631
                , 127.0703703
                , 127.0752611
                , 127.0714959
                , 127.0833114
                , 127.0822014
                , 127.0630463
                , 127.0616575
                , 127.0591168
                , 127.0581133
                , 127.0609127
                , 127.0981565
                , 127.100179
                , 127.0961796
                , 127.0994639
                , 127.0960374
                , 127.0931503
                , 127.0983807
                , 127.0914739
                , 127.0941528
                , 127.0909176
                , 127.0934367
                , 127.0998051
                , 127.09237
                , 127.0889649
                , 127.0927064
                , 127.0851357
                , 127.0886705
                , 127.0943122
                , 127.0864656
                , 127.0900272
                , 127.0841024
                , 127.0823368
                , 127.0858988
                , 127.0878651
                , 127.0889176
                , 127.0792412
                , 127.0850113
                , 127.0833197
                , 127.0840099
                , 127.088395
                , 127.0844064
                , 127.0639163
                , 127.0740599
                , 127.0638635
                , 127.0617254
                , 127.0627992
                , 127.0650939
                , 127.0624873
                , 127.0558614
                , 127.0670503
                , 127.0657884
                , 127.0703118
                , 127.0701427
                , 127.072731
                , 127.0736941
                , 127.075142
                , 127.0747805
                , 127.0756882
                , 127.0769485
                , 127.0821202
                , 127.0618957
                , 127.0615027
                , 127.0657849
                , 127.0595275
                , 127.0625967
                , 127.0631197
                , 127.0610288
                , 127.0632368
                , 127.0604289
                , 127.0655658
                , 127.0617043
                , 127.0608816
                , 127.08127
                , 127.0581409
                , 127.0531471
                , 127.0602449
                , 127.0570388
                , 127.070132
                , 127.0777151
                , 127.0708541
                , 127.0705961
                , 127.0569744
        };

        // 범위 표시 원

        for (int i = 0; i < 110; i++) {
            Park_Y[i] = Park_Y[i]+0.008757;
            map.addCircle(new CircleOptions()
                    .center(new LatLng(Park_X[i], Park_Y[i]))
                    .fillColor(Color.argb(80, 100, 0, 0))
                    .strokeColor(Color.RED)
                    .strokeWidth(strokewidth)
                    .radius(20));

            // 마커로 내용 표시하기

            final LatLng Loc3 = new LatLng(Double.valueOf(Park_X[i]).doubleValue(), Double.valueOf(Park_Y[i]).doubleValue());
            map.addMarker(new MarkerOptions().position(Loc3).title(Park_Name[i]).icon(BitmapDescriptorFactory.fromResource(R.drawable.smokex)));

        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "종료하시려면 한번더 눌러주세요.", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }
}
