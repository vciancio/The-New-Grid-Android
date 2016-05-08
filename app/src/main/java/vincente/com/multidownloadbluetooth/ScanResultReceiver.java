package vincente.com.multidownloadbluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by vincente on 5/4/16
 */
public class ScanResultReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        //Synchronization
        JSONArray array = null;
        try {
            array = new JSONArray(intent.getExtras().getString("results"));
            addToDatabase(context, array);
        }catch(JSONException e){
            e.printStackTrace();
        }
        Intent i = new Intent(Constants.ACTION_SCAN_UPDATE);
        context.getApplicationContext().sendBroadcast(i);
    }

    private synchronized void addToDatabase(Context context, JSONArray results){
        //Have to do it like this because ContentValues doesn't like the format "89:4D:32"
        SQLiteDatabase db = null;
        try {
            db = DbHelper.getInstance(context.getApplicationContext()).getWritableDatabase();
            for (int i = 0; i < results.length(); i++) {
                JSONObject object = (JSONObject) results.get(i);
                String address = object.getString("address");
                SQLiteStatement knownDevicesStatement =
                        db.compileStatement(
                                "INSERT INTO " + DbHelper.TABLE_CONTACT
                                        + "(" + DbHelper.KEY_ADDRESS + ")"
                                        + " VALUES (" + "'" + address + "'" + ");");
                SQLiteStatement contactStatement =
                        db.compileStatement(
                                "INSERT INTO " + DbHelper.TABLE_CONTACT
                                        + "(" + DbHelper.KEY_ADDRESS + ")"
                                        + " VALUES (" + "'" + address + "'" + ");");
                db.beginTransaction();
                if (knownDevicesStatement.executeInsert()>0 && contactStatement.executeInsert()>0) {
                    db.setTransactionSuccessful();
                }
                db.endTransaction();
            }
        } catch (SQLiteConstraintException e) {
            //If there is already the item in the database, its perfectly fine.
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if(db != null){
                db.close();
            }
        }
    }
}