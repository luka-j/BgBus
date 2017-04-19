package rs.luka.android.bgbus.io;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

/*
 * See StatsReporting
 */

/**
 * WIP
 * Created by luka on 10.11.15..
 */
public class StatsBase extends SQLiteOpenHelper {
    private static final String TABLE_DATA = "stats (_id INTEGER PRIMARY KEY, data TEXT)";
    private static final String DB_NAME = "stats.db";
    private static final int DB_VERSION = 1;
    private static StatsBase database;

    public StatsBase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @NonNull
    public static StatsBase getInstance(Context c) {
        if(database == null) database=new StatsBase(c);
        return database;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS stats");
        onCreate(db);
    }

    public void insertRecord(String data) {
        ContentValues cv = new ContentValues(1);
        cv.put("data", data);
        SQLiteDatabase db   = getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DATA);
        long           code = db.insert("stats", null, cv);
    }

    public List<String> queryRecords() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DATA);
        Cursor c = db.query("stats",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null);
        c.moveToFirst();
        List<String> records = new LinkedList<>();
        while(!c.isAfterLast()) {
            records.add(c.getString(c.getColumnIndex("data")));
            c.moveToNext();
        }
        c.close();
        return records;
    }

    public void clearRecords() {
        getWritableDatabase().execSQL("DROP TABLE IF EXISTS stats");
    }
}
