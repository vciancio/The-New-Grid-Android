package vincente.com.multidownloadbluetooth;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by vincente on 4/29/16
 */
public class DBUtils {

    /**
     * Gets the Users out of the Database
     *
     * Contacts.ADDRESS, Contact.NICKNAME, Seen_Device.Public_Key
     */
    public static Cursor getUsers(Context context){
        DbHelper helper = DbHelper.getInstance(context);

        String[] projection = {
                DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_ADDRESS,
                DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_NICKNAME,
                DbHelper.TABLE_SEEN_DEVICE + "." + DbHelper.KEY_PUBLIC_KEY
        };

        Cursor c = DbHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        if (c.moveToFirst()) {
            while ( !c.isAfterLast() ) {
                System.out.println("Table Name=> "+c.getString(0));
                c.moveToNext();
            }
        }
        else{
            System.out.println("No tables");
        }


        String selection = DbHelper.TABLE_CONTACT
                + " outer left join " + DbHelper.TABLE_SEEN_DEVICE + " on"
                + " " + DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_ADDRESS + " = "
                + DbHelper.TABLE_SEEN_DEVICE + "." + DbHelper.KEY_ADDRESS;
        System.out.println(selection);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                selection,
                projection,
                null,
                null,
                null,
                null,
                DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_NICKNAME + " DESC, "
                    + DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_ADDRESS + " DESC"
        );
        return cursor;
    }

    /**
     * Get Messages out of the Database for a specific user
     *
     * _id | body | other_address | time_stamp | encrypted | sent_from_me
     *
     */
    public static Cursor getMessages(Context context, String otherAddress){
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
}
