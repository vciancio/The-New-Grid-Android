package vincente.com.multidownloadbluetooth.receivers;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

import vincente.com.multidownloadbluetooth.DbHelper;
import vincente.com.pnib.BluetoothLeService;
import vincente.com.pnib.Config;
import vincente.com.pnib.Constants;
import vincente.com.pnib.FTNLibrary;

/**
 * Created by vincente on 5/10/16
 */
public class ForwardMessageReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

    }

    public class MyIntentService extends IntentService {
        private BluetoothLeService sendService = null;

        public MyIntentService() {
            super(MyIntentService.class.getSimpleName());
        }

        @Override
        public void onHandleIntent(Intent broadcastedIntent) {
            // parse intent parameters...

            // bind to whatever service
            Intent bindIntent = new Intent(this,BluetoothLeService.class);
            String rawJSON = broadcastedIntent.getStringExtra(Constants.INTENT_EXTRA_RESULTS);

            final FTNLibrary.Message message = new FTNLibrary.Message(rawJSON);
            if(sendService == null){
                Toast.makeText(this, "Unable to forward Message to " + message.toUUID+ ". " +
                                "SendService Unavailable...",
                        Toast.LENGTH_SHORT).show();
                Log.e("ForwardMessageReceiver", "Unable to forward message to " + message.toUUID + ". " +
                        "SendService Unavailable...");
                return;
            }
            //Figure out who we need to get the message to.
            final String toUUID = message.toUUID;

            //Bind to the service and send the things we need
            bindService(bindIntent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    BluetoothLeService sendService = ((BluetoothLeService.LocalBinder) service).getSendingServiceInstance();
                    //Go through our db and see if we can reach them right now.
                    if(DbHelper.getInstance(MyIntentService.this).isInRange(toUUID)){
                        message.address = DbHelper.getInstance(MyIntentService.this).getAddress(toUUID);
                        sendService.sendMessage(message, false);
                        Log.d("ForwardServiceCon", "Forwarded a message for " +
                                UUID.nameUUIDFromBytes(Config.bytesFromString(message.toUUID)) +
                                " from " +
                                UUID.nameUUIDFromBytes(Config.bytesFromString(message.fromUUID)) +
                                " through " + message.address);
                    }
                    else{
                        String addresses[] = DbHelper.getInstance(MyIntentService.this).inRangeDevices();
                        for(String address : addresses){
                            message.address = address;
                            sendService.sendMessage(message, true);
                            Log.d("ForwardServiceCon", "Forwarded a message for " +
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
                    Log.d("ForwardServiceCon", "We have disconnected from the service");
                }

            }, Context.BIND_AUTO_CREATE);
        }
    }
}
