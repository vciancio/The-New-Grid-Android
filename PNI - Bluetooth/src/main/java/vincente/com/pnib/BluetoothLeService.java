package vincente.com.pnib;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by vincente on 3/10/16
 */
public class BluetoothLeService extends Service{
    private static final String TAG = "BluetoothLeService";

    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private Config mConfig;

    //Current State of our BLE Adapter
    private int mAdapterState = 0;

    private BroadcastUpdateThread broadcastUpdateThread;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mHandler = new Handler();

        //TODO: Replace this with the actual config from the library
        mConfig = new Config();
        mConfig.setIsDebugging(true);
        mConfig.UUID_NAME = "2b382cd6-eb3a-11e5-9ce9-5e5517507c66";

        //If we don't have the bluetooth adapter, then wait for someone to turn it on.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Could not access Bluetooth Adapter, asking application to turn it on for us");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            sendBroadcast(enableBtIntent);
            mBluetoothAdapter = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("Started Service!");
        mHandler.post(updateRunnable);
        return 0;
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            broadcastUpdate();
            mHandler.postDelayed(updateRunnable, mConfig.getUpdatePeriod());
        }
    };

    /**
     * <p>Notifies the Library that the Bluetooth Adapter has been turned on (prevents us from having to
     * query the bluetooth and waste time)</p>
     * <p>Should only be used when we couldn't get bluetooth access and asked for the app to turn on Bluetooth</p>
     * @param isBluetoothOn whether or not the bluetooth adapter is turned on
     */
    public void setIsBluetoothOn(boolean isBluetoothOn){
        if(!isBluetoothOn){
            mBluetoothAdapter = null;
            return;
        }

        BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Log.e(TAG, "Could not access Bluetooth Adapter");
            return;
        }

        mBluetoothAdapter = adapter;
    }

    /**
     * <p>Starts up the Broadcast Thread. This will update our information about our neighbors,
     * then broadcast an update telling out neighbors our current status</p>
     */
    public synchronized void broadcastUpdate(){
        BroadcastUpdateThread broadcastUpdateThread = new BroadcastUpdateThread();
        broadcastUpdateThread.start();
        this.broadcastUpdateThread = broadcastUpdateThread;
    }

    /**
     * Basic Debug Log for convenience
     */
    private void log(String message){
        if(mConfig.isDebugging())
            Log.d(TAG, message);
    }

    private String describeBluetoothDevice(BluetoothDevice device){
        JSONObject object = new JSONObject();
        try {
            object.put("name", device.getName());
            object.put("address", device.getAddress());
        } catch (JSONException e) {
            log("Failed to Describe Device because of JSON Exception");
            e.printStackTrace();
        }
        return object.toString();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * <p>This thread runs while listening for incoming connections. It behaves
     * like a server-side client.</p>
     * <p>
     *     <li>Get all Neighboring Devices</li>
     *     <li>Ask them for their Routing Tables</li>
     *     <li>Update your routing table</li>
     *     <li>Send them your routing table</li>
     * </p>
     */
    private class BroadcastUpdateThread extends Thread{
        private BluetoothLeScanner mScanner;
        private Handler mHandler;
        GetNeighborUpdateThread neighborUpdateThread;

        public BroadcastUpdateThread(){
            mScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mHandler = new Handler();
        }

        @Override
        public void run() {
            log("Starting to scan");
            mAdapterState = STATE_CONNECTING;
            neighborUpdateThread = new GetNeighborUpdateThread();
            mScanner.startScan(mScanCallback);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    log("Stopping Scan");
                    mScanner.stopScan(mScanCallback);
                    neighborUpdateThread.start();
                    mAdapterState = STATE_NONE;
                }
            }, mConfig.getScanLength());
        }

        private ScanCallback mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                log("Found Device: " + describeBluetoothDevice(result.getDevice()));
                neighborUpdateThread.addDeviceToQueue(result.getDevice());
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                log("Batch Results:");
                for(ScanResult result : results){
                    log("\t\t" + describeBluetoothDevice(result.getDevice()));
                    neighborUpdateThread.addDeviceToQueue(result.getDevice());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "Scan Failed: " + errorCode);
            }
        };
    }

    private class GetNeighborUpdateThread extends Thread{
        private final static String TAG = "GetNeighborUpdateThread";
        private final ArrayList<BluetoothDevice> deviceQueue;
        private final ArrayList<BluetoothDevice> devicesToUpdate;
        private CountDownLatch characteristicLatch;
        private JSONObject currentBuildingObject;

        public GetNeighborUpdateThread(){
            deviceQueue = new ArrayList<>();
            devicesToUpdate = new ArrayList<>();
        }

        @Override
        public void run() {
            Log.d(TAG, "Started GetNeighborUpdateThread");
            while(!deviceQueue.isEmpty()){
                BluetoothDevice device = deviceQueue.get(0);
                deviceQueue.remove(0);
                devicesToUpdate.add(device);

                //We got our Device! Time to try to connect to it
                Log.d(TAG, "Trying to connect to: " + describeBluetoothDevice(device));
                BluetoothGatt gattConnection = device.connectGatt(getBaseContext(), false, gattCallback);
                mAdapterState = STATE_CONNECTING;
                characteristicLatch = new CountDownLatch(1);

                //Wait for us to finish getting all of our characteristics needed for the node or timeout
                try {
                    Log.d(TAG, "Waiting for the device to finish getting characteristics or to timeout. Then Disconnect");
                    characteristicLatch.await(5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(gattConnection != null) {
                    gattConnection.disconnect();
                    gattConnection.close();
                }
                mAdapterState = STATE_NONE;
            }
            Log.d(TAG, "Finished Getting Updates from Neighbors");
        }

        /**
         * <p>Add Devices to the queue so we can get their information, store it, and update them with
         * our information</p>
         */
        public synchronized void addDeviceToQueue(BluetoothDevice device){
            synchronized(deviceQueue){
                boolean shouldAdd = true;
                for(BluetoothDevice updateDevice : deviceQueue){
                    if(updateDevice.getAddress().equals(device.getAddress())){
                        shouldAdd = false;
                        break;
                    }
                }
                if(shouldAdd) {
                    deviceQueue.add(device);
                    Log.d(TAG, "Added: " + describeBluetoothDevice(device));
                }
            }
        }

        private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    mAdapterState = STATE_CONNECTED;
                    Log.d(TAG, "Connected to Gatt Server: " + describeBluetoothDevice(gatt.getDevice()));
                    Log.d(TAG, "Attempting to start service discovery:" +
                            gatt.discoverServices());
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from " + describeBluetoothDevice(gatt.getDevice()));
                    //TODO: Save the new JSON Node to the Library
                    mAdapterState = STATE_NONE;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                //We found a service that matched ours! Lets poll it for info
                if(status == BluetoothGatt.GATT_SUCCESS){
                    Log.d(TAG, gatt.getDevice().getAddress() + " has our application on it! Lets log it!");

                    //We now know that we can get info from the device. Create a new JSONObject here.
                    currentBuildingObject = new JSONObject();

                    //TODO: Ask for a Characteristic Read here
                    gatt.readCharacteristic(
                            new BluetoothGattCharacteristic(UUID.fromString(mConfig.UUID_NAME),
                                    BluetoothGattCharacteristic.PROPERTY_READ,
                                    BluetoothGattCharacteristic.PERMISSION_READ));
                }
                else{
                    Log.d(TAG, "onServicesDiscovered received: " + status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);

                //Complete the read into a fomrat and log to the file
                //If we have read all of the characteristics we need, disconnect from the phone.
                if(status == BluetoothGatt.GATT_SUCCESS){
                    Log.d(TAG, "Got the characteristic: " + characteristic.getUuid().toString());
                    try {
                        currentBuildingObject.put(characteristic.getUuid().toString(),
                                characteristic.getStringValue(0));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        characteristicLatch.countDown();
                    }
                }
                else {
                    Log.e(TAG, "There was a problem when getting a characteristic: "
                            + (characteristic!=null?characteristic.getUuid():"null"));
                }
            }
        };
    }
}
