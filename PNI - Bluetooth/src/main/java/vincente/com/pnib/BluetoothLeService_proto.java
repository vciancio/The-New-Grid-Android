package vincente.com.pnib;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by vincente on 3/10/16
 */
public class BluetoothLeService_proto extends Service{
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

    private GatherDevicesForUpdateThread gatherDevicesForUpdateThread;
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
//        mConfig.UUID_NAME = "2b382cd6-eb3a-11e5-9ce9-5e5517507c66";
        String uuid = UUID.randomUUID().toString();
        uuid = uuid.substring(uuid.lastIndexOf("-"));
        mConfig.UUID_NAME = "";
        for(int i=0; i<6; i++){
            mConfig.UUID_NAME += uuid.substring(i*2, (i*2)+1);
            mConfig.UUID_NAME += ":";
        }
        mConfig.UUID_NAME = mConfig.UUID_NAME.replace(mConfig.UUID_NAME.substring(mConfig.UUID_NAME.length()-1), "");

        mConfig.setName("FTN");


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

        //Setup Advertisement
        BluetoothLeAdvertiser advertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .setConnectable(true)
                .build();

        ParcelUuid mApplicationParcelUUID = new ParcelUuid(UUID.fromString(mConfig.UUID_APPLICATION));
        ParcelUuid mNameParcelUUID= new ParcelUuid(UUID.fromString(Config.UUID_CHARACTERISTIC_NAME));
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(mApplicationParcelUUID)
                .addServiceData(mApplicationParcelUUID, "FTN".getBytes(Charset.forName("UTF-8")))
                .build();
        AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d(BluetoothLeService_proto.class.getSimpleName(), "Started Advertising!");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e(BluetoothLeService_proto.class.getSimpleName(), "Failed to start advertising: error code " + errorCode);
            }
        };

        advertiser.startAdvertising(settings, advertiseData, advertiseCallback);
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
     * This is where we handle sending other devices our information
     */
    private void startGattServer(){

        mBluetoothManager.openGattServer(this, new BluetoothGattServerCallback() {

            private BluetoothDevice connectedDevice = null;

            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
                switch(newState){
                    case BluetoothProfile.STATE_CONNECTED:
                        connectedDevice = device;
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        connectedDevice = null;
                        break;
                }
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                super.onServiceAdded(status, service);

            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }
        });
    }

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
        GatherDevicesForUpdateThread gatherDevicesForUpdateThread = new GatherDevicesForUpdateThread();
        gatherDevicesForUpdateThread.start();
        this.gatherDevicesForUpdateThread = gatherDevicesForUpdateThread;
    }

    /**
     * Basic Debug Log for convenience
     */
    private void log(String message){
        if(mConfig.isDebugging())
            Log.d(TAG, message);
    }

    /**
     * Formats a Bluetooth Device's Properties into a string so we can see it visibly.
     */
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
    private class GatherDevicesForUpdateThread extends Thread{
        private BluetoothLeScanner mScanner;
        private Handler mHandler;
        GetNeighborUpdateThread neighborUpdateThread;

        public GatherDevicesForUpdateThread(){
            mScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mHandler = new Handler();
        }

        @Override
        public void run() {
            //Build a List of Filters so we only look for our own app
            ArrayList<ScanFilter> filters = new ArrayList<>();
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(UUID.fromString(mConfig.UUID_APPLICATION)))
                    .build();
            filters.add( filter );

            //Scann Settings
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build();

            log("Starting to scan");
            mAdapterState = STATE_CONNECTING;
            neighborUpdateThread = new GetNeighborUpdateThread();
            mScanner.startScan(filters, settings, mScanCallback);
//            mScanner.startScan(mScanCallback);
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
                String log = "Found Device: " + describeBluetoothDevice(result.getDevice());
                if(result.getScanRecord() != null && result.getScanRecord().getServiceData() != null){
                    for(int i=0; i<result.getScanRecord().getServiceData().size(); i++){
                        ParcelUuid uuid = result.getScanRecord().getServiceUuids().get(i);
                        byte[] data = result.getScanRecord().getServiceData(uuid);
                        log += ", {'uuid':'"+uuid+"', 'advertisement':'" + (data==null?"null":new String(data))+"'}";
                    }

                }
                log(log);
                neighborUpdateThread.addResultToQueue(result);

            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                log("Batch Results:");
                for(ScanResult result : results){
                    log("\t\t" + describeBluetoothDevice(result.getDevice()));
                    neighborUpdateThread.addResultToQueue(result);
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
        private final ArrayList<ScanResult> resultQueue;
        private final ArrayList<ScanResult> devicesToUpdate;
        private CountDownLatch characteristicLatch;
        private JSONObject currentBuildingObject;

        public GetNeighborUpdateThread(){
            resultQueue = new ArrayList<>();
            devicesToUpdate = new ArrayList<>();
            currentBuildingObject = new JSONObject();
        }

        @Override
        public void run() {
            Log.d(TAG, "Started GetNeighborUpdateThread");
            while(!resultQueue.isEmpty()){
                final ScanResult result = resultQueue.get(0);
                resultQueue.remove(0);
                devicesToUpdate.add(result);

                //We got our Device! Time to try to connect to it
                Log.d(TAG, "Trying to connect to: " + describeBluetoothDevice(result.getDevice()));

                //Work around to try and connect to a device. Some people have achieved it by running the conncet on the UI thread
                final BluetoothGatt[] gattConnection = new BluetoothGatt[1];
                new Handler(getApplicationContext().getMainLooper()).post(
                        new Runnable() {
                            @Override
                            public void run() {
                                gattConnection[0] =result.getDevice().connectGatt(getBaseContext(), true, gattCallback, BluetoothDevice.TRANSPORT_LE);
                            }
                        }
                );
                mAdapterState = STATE_CONNECTING;
                characteristicLatch = new CountDownLatch(1);

                //Wait for us to finish getting all of our characteristics needed for the node or timeout
                try {
                    Log.d(TAG, "Waiting for the device to finish getting characteristics or to timeout. Then Disconnect");
                    characteristicLatch.await(10000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(gattConnection[0] != null) {
                    gattConnection[0].disconnect();
                    gattConnection[0].close();
                }
                mAdapterState = STATE_NONE;
            }
            Log.d(TAG, "Finished Getting Updates from Neighbors");
        }

        /**
         * <p>Add Devices to the queue so we can get their information, store it, and update them with
         * our information</p>
         */
        public synchronized void addResultToQueue(ScanResult mNewResult){
            synchronized(resultQueue){
                boolean shouldAdd = true;
                for(ScanResult mQueuedResult : resultQueue){
                    if(mQueuedResult.getDevice().getAddress().equals(mNewResult.getDevice().getAddress())){
                        shouldAdd = false;
                        break;
                    }
                }
                if(shouldAdd) {
                    resultQueue.add(mNewResult);
                    Log.d(TAG, "Added: " + describeBluetoothDevice(mNewResult.getDevice()));
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
                Log.d(TAG, gatt.getDevice().getAddress() + ": In OnServicesDiscovered");
                if(status == BluetoothGatt.GATT_SUCCESS){
                    Log.d(TAG, gatt.getDevice().getAddress() + ": Connected Successfully to service");

                    //We now know that we can get info from the device. Create a new JSONObject here.
                    //TODO: Ask for a Characteristic Read here
                    Log.d(TAG, "Going to try and read characteristics from the device");
                    if(gatt.readCharacteristic(
                            new BluetoothGattCharacteristic(UUID.fromString(Config.UUID_CHARACTERISTIC_NAME),
                                    BluetoothGattCharacteristic.PROPERTY_READ,
                                    BluetoothGattCharacteristic.PERMISSION_READ)
                    )){
                        Log.d(TAG, gatt.getDevice().getAddress() + ": We started to read from the Device");
                    }
                    else{
                        Log.e(TAG, gatt.getDevice().getAddress() + ": We failed at initiating a read from the device");
                    }
                }
                else if(status == BluetoothGatt.GATT_FAILURE){
                    Log.d(TAG, gatt.getDevice().getAddress() + ": Failed to connect to the Gatt Service");
                }
                else{
                    Log.d(TAG, "onServicesDiscovered received: " + status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);

                //Complete the read into a format and log to the file
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
