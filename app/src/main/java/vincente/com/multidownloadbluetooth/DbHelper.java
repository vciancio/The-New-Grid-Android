package vincente.com.multidownloadbluetooth;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by vincente on 4/20/16
 */
public class DbHelper extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 2;

    // Database Name
    private static final String DATABASE_NAME = "FTNDatabase.db";

    // Contacts table name
    public static final String TABLE_CONTACT = "CONTACT";
    public static final String TABLE_SEEN_DEVICE = "SEENDEVICE";
    public static final String TABLE_MESSAGE = "MESSAGE";

    // Contacts Table Columns names
    public static final String KEY_ADDRESS      = "address";
    public static final String KEY_NICKNAME     = "nickname";
    public static final String KEY_ID           = "_id";
    public static final String KEY_UUID        = "uuid";
    public static final String KEY_BODY         = "body";
    public static final String KEY_TIMESTAMP    = "time_stamp";
    public static final String KEY_ENCRYPTED    = "encrypted";
    public static final String KEY_SENT_FROM_ME = "sent_from_me";
    public static final String KEY_PUBLIC_KEY   = "public_key";
    public static final String KEY_IN_RANGE     = "inRange";

    public static DbHelper instance;

    public static DbHelper getInstance(Context context){
        if(instance == null)
            instance = new DbHelper(context.getApplicationContext());
        return instance;
    }

    private DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_CONTACTS_TABLE =
                "CREATE TABLE CONTACT ( \n" +
                        " uuid text not null primary key, \n" +
                        " address text not null, \n" +
                        " public_key text default null, \n" +
                        " nickname text, \n" +
                        " inRange integer default 0\n" +
                        ");\n";

        String CREATE_MESSAGE_TABLE =
                "CREATE TABLE MESSAGE (\n" +
                        "  _id integer not null primary key autoincrement,\n" +
                        "  body text not null, \n" +
                        "  uuid text not null,\n" +
                        "  time_stamp numeric not null,\n" +
                        "  encrypted integer not null default 0,\n" +
                        "  sent_from_me integer not null default 0\n" +
                        ");";
        System.out.println(CREATE_CONTACTS_TABLE);
        try{
//            db.execSQL(CREATE_SEEN_TABLE);
            db.execSQL(CREATE_CONTACTS_TABLE);
            db.execSQL(CREATE_MESSAGE_TABLE);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACT);
        db.execSQL("DROP Table IF Exists " + TABLE_MESSAGE);
//        db.execSQL("DROP Table IF Exists " + TABLE_SEEN_DEVICE);

        // Create tables again
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Returns whether or not the address exits in the Seen Devices Table
     * @param address Address to check for
     */
    public boolean addressExists(String address){
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(TABLE_CONTACT, null,
                    KEY_ADDRESS + "=" + DatabaseUtils.sqlEscapeString(address), null, null, null, null);
            return cursor.getCount() > 0;
        } finally {
            if(cursor != null)
                cursor.close();
        }
    }

    /**
     * Adds an address to the database when we see it
     * @param uuid
     * @param address
     * @return
     */
    public boolean addAddress(String uuid, String address, boolean inRange){
        SQLiteDatabase db = getWritableDatabase();
        SQLiteStatement contactStatement =
                db.compileStatement(
                        "INSERT INTO " + DbHelper.TABLE_CONTACT
                                + "(" + DbHelper.KEY_ADDRESS + ", " + KEY_UUID + ", " + KEY_IN_RANGE + ") "
                                + "VALUES (" + DatabaseUtils.sqlEscapeString(address) + ", "
                                + DatabaseUtils.sqlEscapeString(uuid) + ", " + (inRange?1:0) + ");");
        SQLiteStatement updateStatement = db.compileStatement(
                "UPDATE " + DbHelper.TABLE_CONTACT + "\n"
                        + "SET " + DbHelper.KEY_ADDRESS + "=" + DatabaseUtils.sqlEscapeString(address) + "\n"
                        + "WHERE " + KEY_UUID + "=" + DatabaseUtils.sqlEscapeString(uuid) + ";");

        db.beginTransaction();
        //Essentially an upsert
        try {
            if (updateStatement.executeUpdateDelete() > 0) {
                db.setTransactionSuccessful();
            } else if (contactStatement.executeInsert() != -1) {
                db.setTransactionSuccessful();
            } else {
                return false;
            }
        } finally {
            db.endTransaction();
        }
        return true;
    }

    /**
     * Insert a message into our database
     * @param otherUUID
     * @param message
     * @param encrypted
     * @param fromMe
     * @return
     */
    public long addMessage(String otherUUID, String message, boolean encrypted, boolean fromMe){
        SQLiteDatabase db = getWritableDatabase();
        SQLiteStatement statement = db.compileStatement(
                "INSERT INTO " + TABLE_MESSAGE + " (" + KEY_BODY + ", " + KEY_UUID + ", " +
                        KEY_TIMESTAMP + ", " + KEY_ENCRYPTED + ", " + KEY_SENT_FROM_ME + ") " +
                        "VALUES (" + DatabaseUtils.sqlEscapeString(message) + ", " + DatabaseUtils.sqlEscapeString(otherUUID) + ", " +
                        System.currentTimeMillis() + ", " + (encrypted ? 1 : 0) + ", " + (fromMe ? 1 : 0) + ");");
        return statement.executeInsert();
    }

    /**
     * Gets the Users out of the Database
     * <p/>
     * Contacts.ADDRESS, Contact.NICKNAME, Seen_Device.Public_Key
     */
    public Cursor getUsersCursor() {

        String[] projection = {
                DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_UUID,
                DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_NICKNAME,
                DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_PUBLIC_KEY
        };

        String selection = DbHelper.TABLE_CONTACT;

        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                selection,
                projection,
                null,
                null,
                null,
                null,
                DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_NICKNAME + " DESC, "
                        + DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_UUID + " DESC"
        );
    }

    /**
     * Get Messages out of the Database for a specific user
     * <p/>
     * _id | body | other_address | time_stamp | encrypted | sent_from_me
     */
    public Cursor getMessages(String otherUUID) {
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
        String selection = DbHelper.KEY_UUID + "=" + DatabaseUtils.sqlEscapeString(otherUUID);
        String orderby = DbHelper.KEY_TIMESTAMP + " asc";
        return this.getReadableDatabase().query(
                from,
                columns,
                selection,
                null,
                null,
                null,
                orderby
        );
    }

    /**
     * Get the address based off of a device's uuid
     * @param uuid
     * @return
     */
    public String getAddress(String uuid){
        SQLiteDatabase db = getReadableDatabase();

        String[] columns = {KEY_ADDRESS};
        Cursor cursor = null;
        try {
            cursor = db.query(
                    TABLE_CONTACT,
                    columns,
                    KEY_UUID + "=" + DatabaseUtils.sqlEscapeString(uuid),
                    null,
                    null,
                    null,
                    null
            );
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(KEY_ADDRESS));
            } else {
                return null;
            }
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
    }

    public String[] inRangeDevices(){
        ArrayList<String> addresses = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        String[] columns = {KEY_ADDRESS};
        try{
            cursor = db.query(
                    TABLE_CONTACT,
                    columns,
                    KEY_IN_RANGE + "=" + 1,
                    null,
                    null,
                    null,
                    null
            );
            cursor.moveToPosition(-1);
            while(cursor.moveToNext()){
                addresses.add(cursor.getString(cursor.getColumnIndex(KEY_ADDRESS)));
            }
            return addresses.toArray(new String[addresses.size()]);
        } finally {
            if(cursor != null){
                cursor.close();
            }
        }
    }

    /**
     * Returns whether or not the item was in range.
     * @return whether or not we can connect to this device directly
     */
    public boolean isInRange(String uuid){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        String[] columns = {KEY_IN_RANGE};
        try {
            cursor = db.query(
                    TABLE_CONTACT,
                    columns,
                    KEY_UUID + "=" + DatabaseUtils.sqlEscapeString(uuid),
                    null,
                    null,
                    null,
                    null
            );
            if(cursor.moveToFirst()){
                if(cursor.getInt(cursor.getColumnIndex(KEY_IN_RANGE)) == 1){
                    return true;
                }
            }
            return false;
        } finally{
            if(cursor != null){
                cursor.close();
            }
        }
    }

    /**
     * Marks all the items in contact table as not in range.
     * This is to be followed up by marking the ones that we find in the scan as in range.
     */
    public void clearInRangeFlag(){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_IN_RANGE, 0);
        db.beginTransaction();
        db.update(
                TABLE_CONTACT,
                values,
                null,
                null
        );
        db.endTransaction();
    }

    public boolean upsert(String table, String where,
                          String[] whereArgs, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            if(upsert(db, table, where, whereArgs, values)) {
                db.setTransactionSuccessful();
                return true;
            }
            else{
                return false;
            }
        } finally {
            db.endTransaction();
        }
    }

    private boolean upsert(SQLiteDatabase db, String table, String where,
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