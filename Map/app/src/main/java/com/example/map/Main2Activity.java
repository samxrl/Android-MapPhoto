package com.example.map;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.model.LatLng;

import java.io.File;
import java.io.FileInputStream;

public class Main2Activity extends AppCompatActivity {

    private MyDatabaseHelper dbHelper;
    private String id;
    private ImageView photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("test","photo stert");

        setContentView(R.layout.activity_main2);
        Intent intent = getIntent();
        id = "" + intent.getIntExtra("id",-1);

        photo = (ImageView) findViewById(R.id.image);

        dbHelper = new MyDatabaseHelper(this,"PhotoDB.db",null,1);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query("Photo",null,"id = ?",new String[]{id},null,null,null);
        cursor.moveToFirst();
        int ID = cursor.getInt(cursor.getColumnIndex("id"));
        final String path= Environment.getExternalStorageDirectory()+"/"+cursor.getString(cursor.getColumnIndex("path"));
        String PHAT = cursor.getString(cursor.getColumnIndex("path"));
        cursor.close();
        Bitmap bitmap=BitmapFactory.decodeFile(path);
        Log.d("test",path);
        photo.setImageBitmap(bitmap);

        ImageView back = (ImageView) findViewById(R.id.back);
        ImageView del = (ImageView) findViewById(R.id.del);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("test","back");
                Main2Activity.this.finish();
            }
        });

        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("test","del");
                File file = new File(path);
                file.delete();
                db.delete("Photo","id = ?",new String[]{id});
                Main2Activity.this.finish();
            }
        });

    }
}
