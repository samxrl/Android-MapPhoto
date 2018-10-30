package com.example.map;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by iamxrl on 18/10/29.
 */

public class MyDatabaseHelper extends SQLiteOpenHelper {

    public static final String CREATE_PHOTOS = "create table Photo (" +
            "id integer primary key autoincrement," +
            "path text," +
            "lng real," +
            "lat real)";

    private Context mContext;

    public  MyDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context,name,factory,version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(CREATE_PHOTOS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
    }

}

