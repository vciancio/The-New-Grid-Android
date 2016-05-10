package vincente.com.multidownloadbluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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

    private static synchronized void addToDatabase(Context context, JSONArray results){
        //Have to do it like this because ContentValues doesn't like the format "89:4D:32"
        try {
            for (int i = 0; i < results.length(); i++) {
                JSONObject object = (JSONObject) results.get(i);
                String address = object.getString("address");
                String uuid = object.getString("uuid");
                DbHelper.getInstance(context).addAddress(uuid, address);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}