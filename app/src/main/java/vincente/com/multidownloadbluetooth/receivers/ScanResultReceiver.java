package vincente.com.multidownloadbluetooth.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import vincente.com.multidownloadbluetooth.Constants;
import vincente.com.multidownloadbluetooth.DbHelper;

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
            //Clear everything as being in range, and then update the flags with the ones that are
            DbHelper.getInstance(context).clearInRangeFlag();
            addToDatabase(context, array);
        }catch(JSONException e){
            e.printStackTrace();
        }
        Intent i = new Intent(Constants.ACTION_SCAN_UPDATE);
        context.getApplicationContext().sendBroadcast(i);
    }

    private static synchronized void addToDatabase(Context context, JSONArray results){
        //Have to do it like this because ContentValues doesn't like the format "89:4D:32"
        try {
            for (int i = 0; i < results.length(); i++) {
                JSONObject object = (JSONObject) results.get(i);
                String address = object.getString(vincente.com.pnib.Constants.JSON_KEY_ADDRESS);
                String uuid = object.getString(vincente.com.pnib.Constants.JSON_KEY_UUID);
                DbHelper.getInstance(context).addAddress(uuid, address, true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}