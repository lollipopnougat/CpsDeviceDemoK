package top.lollipopnougat.cpsdevicedemok

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.UUID

class DBHelper(context: Context, version: Int) : SQLiteOpenHelper(context, "device.db", null, version) {
    override fun onCreate(db: SQLiteDatabase?) {
        val sql = "create table device(id integer primary key autoincrement, uid text)"
        db?.execSQL(sql)
        val uid = UUID.randomUUID().toString();
        val id = ContentValues().apply {
            put("uid", uid)
        }
        db?.insert("device", null, id)

    }



    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldV: Int, newV: Int) {

    }

    val uniqueId: String
        get() {
            val db = readableDatabase
            var res = ""
            val cursor = db.query("device",null,null,null,null,null,null)
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
                res = cursor.getString(1)
            }
            cursor.close()
            Log.i("minfo", "uid = $res")
            return res
        }


}