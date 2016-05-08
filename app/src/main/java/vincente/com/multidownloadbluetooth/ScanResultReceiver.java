package vincente.com.multidownloadbluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by vincente on 5/4/16
 */
public class ScanResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, InsertScanService.class);
        i.putExtras(intent.getExtras());
        context.startService(i);
    }
}
