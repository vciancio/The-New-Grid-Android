package vincente.com.multidownloadbluetooth;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.DatabaseUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by vincente on 5/4/16
 */
public class ScanResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        JSONArray array = null;
        try {
            array = new JSONArray(intent.getExtras().getString("results"));

            for(int i=0; i<array.length(); i++){
                JSONObject object = (JSONObject) array.get(i);
                String address = DatabaseUtils.sqlEscapeString(object.getString("address"));
                ContentValues values = new ContentValues();
                values.put(DbHelper.KEY_ADDRESS, address);
                DBUtils.upsert(context.getApplicationContext(), DbHelper.TABLE_SEEN_DEVICE,
                        DbHelper.KEY_ADDRESS + "=" + address, null, values);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent i = new Intent(Constants.ACTION_SCAN_UPDATE);
        context.getApplicationContext().sendBroadcast(i);
    }
}
