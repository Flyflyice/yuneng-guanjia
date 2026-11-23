package com.yuneng.guanjia.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "yunengguanjia.db";
    private static final int VERSION = 2;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE vehicles ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "type TEXT NOT NULL,"
                + "battery REAL NOT NULL DEFAULT 0,"
                + "tank REAL NOT NULL DEFAULT 0,"
                + "thermal REAL NOT NULL DEFAULT 0.4606,"
                + "fuel_grade TEXT NOT NULL DEFAULT '92#',"
                + "is_default INTEGER NOT NULL DEFAULT 0,"
                + "created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "vehicle_id INTEGER NOT NULL,"
                + "type TEXT NOT NULL,"
                + "ts INTEGER NOT NULL,"
                + "odometer REAL NOT NULL DEFAULT 0,"
                + "amount REAL NOT NULL DEFAULT 0,"
                + "quantity REAL NOT NULL DEFAULT 0,"
                + "unit_price REAL NOT NULL DEFAULT 0,"
                + "list_amount REAL NOT NULL DEFAULT 0,"
                + "discount REAL NOT NULL DEFAULT 0,"
                + "soc_start REAL NOT NULL DEFAULT 0,"
                + "soc_end REAL NOT NULL DEFAULT 0,"
                + "oil_before REAL NOT NULL DEFAULT 0,"
                + "oil_after REAL NOT NULL DEFAULT 0,"
                + "remaining_oil REAL NOT NULL DEFAULT 0,"
                + "remaining_soc REAL NOT NULL DEFAULT 0,"
                + "station TEXT NOT NULL DEFAULT '',"
                + "fuel_type TEXT NOT NULL DEFAULT '',"
                + "fuel_grade TEXT NOT NULL DEFAULT '',"
                + "category TEXT NOT NULL DEFAULT '',"
                + "note TEXT NOT NULL DEFAULT '',"
                + "photo_path TEXT NOT NULL DEFAULT '',"
                + "previous_recorded INTEGER NOT NULL DEFAULT 1,"
                + "created_at INTEGER NOT NULL,"
                + "FOREIGN KEY(vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE)");

        db.execSQL("CREATE INDEX idx_records_vehicle_ts ON records(vehicle_id, ts DESC)");
        db.execSQL("CREATE INDEX idx_records_vehicle_odo ON records(vehicle_id, odometer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE records ADD COLUMN fuel_type TEXT NOT NULL DEFAULT ''");
        }
    }
}
