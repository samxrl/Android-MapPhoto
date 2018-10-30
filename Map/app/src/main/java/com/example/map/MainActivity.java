package com.example.map;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.inner.*;
import com.baidu.mapapi.map.BaiduMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

public class MainActivity extends AppCompatActivity {

    private MyDatabaseHelper dbHelper;

    public LocationClient mLocationClient;
    private TextView positionText;
    private MapView mapView;
    private BaiduMap baiduMap;
    private boolean isFirstLocate = true;

    public static final int TAKE_PHOTO = 1;
    private Uri imageUri;


    private static int PhotoId = 1;
    private double lat;// 纬度
    private double lng;// 经度

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        dbHelper = new MyDatabaseHelper(this,"PhotoDB.db",null,1);
        dbHelper.getWritableDatabase();

        mapView = (MapView) findViewById(R.id.bmapView);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        positionText = (TextView) findViewById(R.id.jwd);
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        } else {
            requestLocation();
        }

        //悬浮按钮
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                Cursor cursor = db.query("Photo",null,null,null,null,null,null,null);
                if(cursor.moveToFirst()){
                    do{
                        PhotoId = cursor.getInt(cursor.getColumnIndex("id")) + 1;
                    }while (cursor.moveToNext());
                }
                cursor.close();

                File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
                try {
                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT < 24) {
                    imageUri = Uri.fromFile(outputImage);
                } else {
                    imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.map.fileprovider", outputImage);
                }
                // 启动相机程序
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                Log.d("test",imageUri.toString());
                startActivityForResult(intent, TAKE_PHOTO);
            }
        });

        //覆盖物点击事件
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker result) {
                Log.d("test","2");
                int ID = result.getZIndex();
                Intent intent = new Intent(MainActivity.this,Main2Activity.class);
                intent.putExtra("id",ID);
                startActivity(intent);

                return true;
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        // 将拍摄的照片保存到本地,并把信息存进数据库
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        saveImage(bitmap);

                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        ContentValues values = new ContentValues();
                        values.put("path","Android/data/com.example.map/photos/"+PhotoId+".jpg");
                        values.put("lng",lng);
                        values.put("lat",lat);
                        db.insert("Photo",null,values);

                        MarkerOptions markerOptions = new MarkerOptions();
                        BitmapDescriptor bitmap1 = BitmapDescriptorFactory.fromResource(R.mipmap.photo); // 描述图片
                        markerOptions.position(new LatLng(lat,lng)) // 设置位置
                                .icon(bitmap1) // 加载图片
                                .draggable(true) // 支持拖拽
                                .zIndex(PhotoId); //
                        //把绘制的覆盖物加到百度地图上去
                        baiduMap.addOverlay(markerOptions);
                        Log.d("test","take");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }

    //保存图片到本地
    public static void saveImage(Bitmap bmp) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "Android/data/com.example.map/photos");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName =PhotoId + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(BDLocation location) {

        //消除位置偏移
        Point point = new Point(location.getLongitude(),(location.getLatitude()));
        point = point.google_bd_encrypt();
        lng = point.getLng();
        lat = point.getLat();

        if (isFirstLocate) {
            Toast.makeText(this, "nav to " + location.getAddrStr(), Toast.LENGTH_SHORT).show();
            LatLng ll = new LatLng(lat, lng);
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(update);
            update = MapStatusUpdateFactory.zoomTo(16f);
            baiduMap.animateMapStatus(update);
            isFirstLocate = false;
        }
        MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
        locationBuilder.latitude(lat);
        locationBuilder.longitude(lng);
        MyLocationData locationData = locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }

    private void requestLocation() {
        initLocation();
        mLocationClient.start();
    }

    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(5000);
        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query("Photo",null,null,null,null,null,null,null);
        if(cursor.moveToFirst()){
            do{
                PhotoId = cursor.getInt(cursor.getColumnIndex("id")) + 1;
            }while (cursor.moveToNext());
        }
        cursor.close();

        //设置地图覆盖物
        baiduMap.clear();

        cursor = db.query("Photo",null,null,null,null,null,null,null);
        if(cursor.moveToFirst()){
            do{
                int ID = cursor.getInt(cursor.getColumnIndex("id"));
                double LNG = cursor.getDouble(cursor.getColumnIndex("lng"));
                double LAT = cursor.getDouble(cursor.getColumnIndex("lat"));
                String PHAT = cursor.getString(cursor.getColumnIndex("path"));
                MarkerOptions markerOptions = new MarkerOptions();
                BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.mipmap.photo); // 描述图片
                markerOptions.position(new LatLng(LAT,LNG)) // 设置位置
                        .icon(bitmap) // 加载图片
                        .draggable(true) // 支持拖拽
                        .zIndex(ID); //
                //把绘制的覆盖物加到百度地图上去
                baiduMap.addOverlay(markerOptions);
                Log.d("test","1");
            }while (cursor.moveToNext());
        }
        cursor.close();


    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意所有权限才能使用本程序", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
//
            if (location.getLocType() == BDLocation.TypeGpsLocation
                    || location.getLocType() == BDLocation.TypeNetWorkLocation) {
                navigateTo(location);
            }
        }
    }

}
