package vincente.com.multidownloadbluetooth.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import vincente.com.multidownloadbluetooth.MyIntentService;

/**
 * Created by vincente on 5/10/16
 */
public class ForwardMessageReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, MyIntentService.class);
        context.startService(intent);
    }
}
