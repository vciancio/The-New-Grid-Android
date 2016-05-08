package vincente.com.multidownloadbluetooth;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by vincente on 4/29/16
 */
public class DBUtils {


    /**
     * Gets the Users out of the Database
     * <p/>
     * Contacts.ADDRESS, Contact.NICKNAME, Seen_Device.Public_Key
     */
    public static Cursor getUsersCursor(Context context) {
        DbHelper helper = DbHelper.getInstance(context);

        String[] projection = {
                DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_ADDRESS,
                DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_NICKNAME,
                DbHelper.TABLE_SEEN_DEVICE + "." + DbHelper.KEY_PUBLIC_KEY
        };

        String selection = DbHelper.TABLE_SEEN_DEVICE
                + " outer left join " + DbHelper.TABLE_CONTACT + " on"
                + " " + DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_ADDRESS + " = "
                + DbHelper.TABLE_SEEN_DEVICE + "." + DbHelper.KEY_ADDRESS;
        System.out.println(selection);
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.query(
                selection,
                projection,
                null,
                null,
                null,
                null,
                DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_NICKNAME + " DESC, "
                        + DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_ADDRESS + " DESC"
        );
    }

    /**
     * Get Messages out of the Database for a specific user
     * <p/>
     * _id | body | other_address | time_stamp | encrypted | sent_from_me
     */
    public static Cursor getMessages(Context context, String otherAddress) {
        DbHelper helper = DbHelper.getInstance(context);
        /*
        select *
                from message as cur
        group by other_address
        having time_stamp >= all(
                select time_stamp
                from message as this
        where cur.other_address = this.other_address)
        */
        String from = "message as cur";
        String[] columns = {"*"};
        String selection = DbHelper.KEY_OTHER_ADDRESS + "=?";
        String[] selectionArgs = {otherAddress};
        String orderby = DbHelper.KEY_TIMESTAMP + " asc";
        return helper.getReadableDatabase().query(
                from,
                columns,
                selection,
                selectionArgs,
                null,
                null,
                orderby
        );
    }

    public static boolean upsert(Context context, String table, String where,
                                 String[] whereArgs, ContentValues values) {
        SQLiteDatabase db = DbHelper.getInstance(context).getWritableDatabase();
        db.beginTransaction();
        try {
            if(upsert(context, db, table, where, whereArgs, values)) {
                db.setTransactionSuccessful();
                return true;
            }
            else{
                return false;
            }
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public static boolean upsert(Context context, SQLiteDatabase db, String table, String where,
                                 String[] whereArgs, ContentValues values) {
        try {
            int rows = db.update(table, values, where, whereArgs);
            if (rows == 0) {
                long insert = db.insert(table, null, values);
                if (insert == -1) {
                    return false;
                }
            }
        } catch (SQLiteException e) {
            Log.d("DbUtils", "Failed to update/insert" + table + ", where: " + where + ", " +
                    Arrays.toString(whereArgs));
            e.printStackTrace();
        }
        return true;
    }
}
