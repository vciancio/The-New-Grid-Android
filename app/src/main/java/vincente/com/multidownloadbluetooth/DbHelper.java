package vincente.com.multidownloadbluetooth;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by vincente on 4/20/16
 */
public class DbHelper extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 2;

    // Database Name
    private static final String DATABASE_NAME = "FTNDatabase";

    // Contacts table name
    public static final String TABLE_CONTACT = "CONTACT";
    public static final String TABLE_SEEN_DEVICE = "SEENDEVICE";
    public static final String TABLE_MESSAGE = "MESSAGE";

    // Contacts Table Columns names
    public static final String KEY_ADDRESS      = "address";
    public static final String KEY_NICKNAME     = "nickname";
    public static final String KEY_ID           = "_id";
    public static final String KEY_BODY         = "body";
    public static final String KEY_OTHER_ADDRESS= "other_address";
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
                "  other_address text not null,\n" +
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
}