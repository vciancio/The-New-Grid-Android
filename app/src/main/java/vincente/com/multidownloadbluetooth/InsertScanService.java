package vincente.com.multidownloadbluetooth;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by vincente on 5/7/16
 */
public class InsertScanService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        JSONArray array = null;
        try {
            array = new JSONArray(intent.getExtras().getString("results"));

            for(int i=0; i<array.length(); i++){
                JSONObject object = (JSONObject) array.get(i);
                String address = DatabaseUtils.sqlEscapeString(object.getString("address"));
                ContentValues values = new ContentValues();
                values.put(DbHelper.KEY_ADDRESS, address);
                DBUtils.upsert(getApplicationContext(), DbHelper.TABLE_SEEN_DEVICE,
                        DbHelper.KEY_ADDRESS + "=" + address, null, values);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        finally {
        }

        Intent i = new Intent(Constants.ACTION_SCAN_UPDATE);
        getApplicationContext().sendBroadcast(i);
        return 0;
    }
}
