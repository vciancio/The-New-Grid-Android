package vincente.com.multidownloadbluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

import vincente.com.pnib.Constants;

/**
 * Created by vincente on 5/8/16
 */
public class NewMessageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            JSONObject object = new JSONObject(intent.getStringExtra("results"));
            String address = object.getString(Constants.JSON_KEY_ADDRESS);
            String message = object.getString(Constants.JSON_KEY_BODY);
            boolean encrypted = object.getBoolean(Constants.JSON_KEY_ENCRYPTED);
            DbHelper.getInstance(context).addAddress(address);
            Intent i = new Intent(vincente.com.multidownloadbluetooth.Constants.ACTION_SCAN_UPDATE);
            context.sendBroadcast(i);
            DbHelper.getInstance(context).addMessage(address, message, encrypted, false);
            i = new Intent(vincente.com.multidownloadbluetooth.Constants.ACTION_MESSAGE_UPDATE);
            i.putExtra("address", address);
            context.sendBroadcast(i);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}