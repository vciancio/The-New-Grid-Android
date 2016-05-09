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

import java.util.Arrays;

/**
 * Created by vincente on 4/20/16
 */
public class DbHelper extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

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
    public static final String KEY_BODY         = "body";
    public static final String KEY_TIMESTAMP    = "time_stamp";
    public static final String KEY_ENCRYPTED    = "encrypted";
    public static final String KEY_SENT_FROM_ME = "sent_from_me";
    public static final String KEY_PUBLIC_KEY   = "public_key";

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
        String CREATE_SEEN_TABLE =
                "CREATE TABLE SEENDEVICE\n" +
                "  ( \n" +
                "  address text not null primary key, \n" +
                "  public_key text default null \n" +
                "  );\n" +
                "\n";
        String CREATE_CONTACTS_TABLE =
                "CREATE TABLE CONTACT ( \n" +
                "  address text not null primary key, \n" +
                "  nickname text\n" +
                ");\n" +
                "\n";
        String CREATE_MESSAGE_TABLE =
                "CREATE TABLE MESSAGE (\n" +
                "  _id integer not null primary key autoincrement,\n" +
                "  body text not null, \n" +
                "  address text not null,\n" +
                "  time_stamp numeric not null,\n" +
                "  encrypted integer not null default 0,\n" +
                "  sent_from_me integer not null default 0\n" +
                ");";
        System.out.println(CREATE_CONTACTS_TABLE);
        try{
            db.execSQL(CREATE_SEEN_TABLE);
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
        db.execSQL("DROP Table IF Exists " + TABLE_SEEN_DEVICE);

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
            cursor = getReadableDatabase().query(TABLE_SEEN_DEVICE, null,
                    KEY_ADDRESS + "=" + DatabaseUtils.sqlEscapeString(address), null, null, null, null);
            return cursor.getCount() > 0;
        } finally {
            if(cursor != null)
                cursor.close();
        }
    }

    /**
     * Adds an address to the database when we see it
     * @param address
     * @return
     */
    public boolean addAddress(String address){
        if(addressExists(address)){
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        SQLiteStatement knownDevicesStatement =
                db.compileStatement(
                        "INSERT INTO " + DbHelper.TABLE_SEEN_DEVICE
                                + "(" + DbHelper.KEY_ADDRESS + ")"
                                + " VALUES (" + DatabaseUtils.sqlEscapeString(address) + ");");
        SQLiteStatement contactStatement =
                db.compileStatement(
                        "INSERT INTO " + DbHelper.TABLE_CONTACT
                                + "(" + DbHelper.KEY_ADDRESS + ")"
                                + " VALUES (" + DatabaseUtils.sqlEscapeString(address) + ");");
        db.beginTransaction();
        if (knownDevicesStatement.executeInsert()>0 && contactStatement.executeInsert()>0) {
            db.setTransactionSuccessful();
        }
        db.endTransaction();
        return true;
    }

    /**
     * Insert a message into our database
     * @param otherAddress
     * @param message
     * @param encrypted
     * @param fromMe
     * @return
     */
    public long addMessage(String otherAddress, String message, boolean encrypted, boolean fromMe){
        SQLiteDatabase db = getWritableDatabase();
        SQLiteStatement statement = db.compileStatement(
                "INSERT INTO " + TABLE_MESSAGE + " (" + KEY_BODY + ", " + KEY_ADDRESS + ", " +
                        KEY_TIMESTAMP + ", " + KEY_ENCRYPTED + ", " + KEY_SENT_FROM_ME + ") " +
                        "VALUES (" + DatabaseUtils.sqlEscapeString(message) + ", " + DatabaseUtils.sqlEscapeString(otherAddress) + ", " +
                        System.currentTimeMillis() + ", " + (encrypted?1:0) + ", " + (fromMe?1:0) + ");");
        return statement.executeInsert();
    }

    /**
     * Gets the Users out of the Database
     * <p/>
     * Contacts.ADDRESS, Contact.NICKNAME, Seen_Device.Public_Key
     */
    public Cursor getUsersCursor() {

        String[] projection = {
                DbHelper.TABLE_SEEN_DEVICE + "." + DbHelper.KEY_ADDRESS,
                DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_NICKNAME,
                DbHelper.TABLE_SEEN_DEVICE + "." + DbHelper.KEY_PUBLIC_KEY
        };

        String selection = DbHelper.TABLE_SEEN_DEVICE
                + " outer left join " + DbHelper.TABLE_CONTACT + " on"
                + " " + DbHelper.TABLE_CONTACT + "." + DbHelper.KEY_ADDRESS + " = "
                + DbHelper.TABLE_SEEN_DEVICE + "." + DbHelper.KEY_ADDRESS;
        System.out.println(selection);
        SQLiteDatabase db = this.getReadableDatabase();
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
    public Cursor getMessages(String otherAddress) {
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
        String selection = DbHelper.KEY_ADDRESS + "=?";
        String[] selectionArgs = {otherAddress};
        String orderby = DbHelper.KEY_TIMESTAMP + " asc";
        return this.getReadableDatabase().query(
                from,
                columns,
                selection,
                selectionArgs,
                null,
                null,
                orderby
        );
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