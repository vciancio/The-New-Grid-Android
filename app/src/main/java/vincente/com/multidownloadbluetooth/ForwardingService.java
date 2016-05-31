package vincente.com.multidownloadbluetooth;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;

import vincente.com.pnib.BluetoothLeService;
import vincente.com.pnib.Config;
import vincente.com.pnib.FTNLibrary;

/**
 * Created by vincente on 5/11/16
 */
public class ForwardingService extends IntentService {
    private static final String TAG = "ForwardingService";
    public ForwardingService() {
        super(ForwardingService.class.getSimpleName());
    }

    @Override
    public void onHandleIntent(Intent broadcastedIntent) {
        // parse intent parameters...

        // bind to whatever service
        Intent bindIntent = new Intent(this,BluetoothLeService.class);
        String rawJSON = broadcastedIntent.getStringExtra(vincente.com.pnib.Constants.INTENT_EXTRA_RESULTS);

        final FTNLibrary.Message message = new FTNLibrary.Message(rawJSON);
        final String toUUID = message.toUUID;

        //Bind to the service and send the things we need
        Log.d(TAG, "Binding to SendingService..");
        bindService(bindIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                BluetoothLeService sendService = ((BluetoothLeService.LocalBinder) service).getSendingServiceInstance();
                //Go through our db and see if we can reach them right now.
                if(DbHelper.getInstance(ForwardingService.this).isInRange(toUUID)){
                    message.address = DbHelper.getInstance(ForwardingService.this).getAddress(toUUID);
                    sendService.sendMessage(message, false);
                    Log.d(TAG, "Forwarded a message for " +
                            UUID.nameUUIDFromBytes(Config.bytesFromString(message.toUUID)) +
                            " from " +
                            UUID.nameUUIDFromBytes(Config.bytesFromString(message.fromUUID)) +
                            " through " + message.address);
                }
                else{
                    String addresses[] = DbHelper.getInstance(ForwardingService.this).inRangeDevices();
                    for(String address : addresses){
                        message.address = address;
                        sendService.sendMessage(message, true);
                        Log.d(TAG, "Forwarded a message for " +
                                UUID.nameUUIDFromBytes(Config.bytesFromString(message.toUUID)) +
                                " from " +
                                UUID.nameUUIDFromBytes(Config.bytesFromString(message.fromUUID)) +
                                " through " + message.address);
                    }
                }
                unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "We have disconnected from the service");
            }
        }, Context.BIND_AUTO_CREATE);
    }
}
