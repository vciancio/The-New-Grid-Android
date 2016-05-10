package vincente.com.pnib;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created by vincente on 4/12/16
 * This is the service that will handle scanning for and updating our lower level service to send
 * and receive updates
 */
public class BluetoothLeService extends Service{

    /**
     * Whether or not we already did our init.
     */
    private boolean init = false;

    /**
     * My Current Bluetooth Manager
     */
    BluetoothManager mBluetoothManager;

    /**
     * My Current Bluetooth Adapter
     */
    private BluetoothAdapter mBluetoothAdapter;

    /**
     * A Handler which deals with periodically scanning for new devices
     */
    private BleServiceHandler bleServiceHandler;

    /**
     * The configuration for our library
     */
    private Config mConfig;

    /**
     * A Queue for sending items to devices
     */
    private final Queue<QueueItem> sendQueue;

    /**
     * A reference to our service so any other thread that binds to it can communicate with it
     */
    private IBinder mBinder = new LocalBinder();

    /**
     * A reference to open BluetoothGatts. These should be closed when removing from the map.
     */
    private Map<byte[], BluetoothGatt> bluetoothGattMap = new IdentityHashMap<>();

    private String mUUID;

    public BluetoothLeService() {
        sendQueue = new ConcurrentLinkedQueue<>();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //If we don't have our own uuid yet, we need to set one!
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(!preferences.contains(Constants.PREF_MY_UUID)){
            preferences.edit().putString(Constants.PREF_MY_UUID, Arrays.toString(Config.generateUUID())).commit();
        }
        mUUID = preferences.getString(Constants.PREF_MY_UUID, "0");

        bleServiceHandler = new BleServiceHandler(this);
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        if(!adapter.isEnabled()){
            Log.d(BluetoothLeService.class.getSimpleName(), "Bluetooth Adapter is not enabled, trying to enable...");
            if(!adapter.enable()){
                Toast.makeText(this, "Cannot turn on Bluetooth. Please do so.", Toast.LENGTH_SHORT).show();
            }
        }

        //Initiate our new Config and setup our adapters. Start Scanning Now!
        mConfig = Config.getInstance();
        mBluetoothAdapter = adapter;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //If we've already Init, then we don't have to start the constant scan again.
        if(init)
            return super.onStartCommand(intent, flags, startId);
        bleServiceHandler.sendEmptyMessage(BleServiceHandler.WHAT_START_SCANNING);
        init = true;
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Will go through the queue and and send items as they are requested
     * What:
     *      0: Start Scanning
     *      1: Stop Scanning
     *      2: Start next Scan
     *      3: Send Next In Queue
     */
    @SuppressLint("HandlerLeak")
    private class BleServiceHandler extends Handler {
        public static final int WHAT_START_SCANNING = 0;
        public static final int WHAT_STOP_SCANNING = 1;
        public static final int WHAT_START_NEXT_SCAN = 2;
        public static final int WHAT_SEND_NEXT_IN_QUEUE = 3;
        private final Context context;
        private boolean isScanning = false;
        private BleServiceHandler(Context context) {
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case WHAT_START_SCANNING:
                    isScanning = true;
                case WHAT_START_NEXT_SCAN:
                    if(isScanning)
                        new ScanNearbyDevicesAsync(context).execute();
                    break;
                case WHAT_STOP_SCANNING:
                    isScanning = false;
                    break;
                case WHAT_SEND_NEXT_IN_QUEUE:
                    new Thread(sendRunnable).start();
                    break;
                default:
            }
        }
    }

    /**
     * Scans for nearby devices so we can then query them and process them.
     */
    public class ScanNearbyDevicesAsync extends AsyncTask<Void, Void, Set<ScanResult>> {
        private Context context;

        public ScanNearbyDevicesAsync(Context context) {
            this.context = context;
        }

        @Override
        protected Set<ScanResult> doInBackground(Void... params) {
            final Set<ScanResult> results = new HashSet<>();

            final BluetoothLeScanner bleScanner = mBluetoothAdapter.getBluetoothLeScanner();
            ArrayList<ScanFilter> filters = new ArrayList<>();
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(UUID.fromString(mConfig.UUID_APPLICATION)))
                    .build();
            filters.add(filter);

            /* Setting up our Scan so we only have to find other people using our application */
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .build();

            final ScanCallback mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    boolean shouldAdd = true;
                    for(ScanResult old : results){
                        if(result.getDevice().getAddress().equals(old.getDevice().getAddress())){
                            shouldAdd = false;
                        }
                    }
                    if(shouldAdd) {
                        results.add(result);
                        Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "Added ScanResult: " + result);
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Log.e("ScanNearbyDevicesAsync", "The Scan Failed");
                    super.onScanFailed(errorCode);
                }
            };

            bleScanner.startScan(filters, settings, mScanCallback);

            bleServiceHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bleScanner.stopScan(mScanCallback);
                    Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "Stopped Scanning");
                    processResults(results);
                }
            }, mConfig.getScanLength());
            return results;
        }

        /**
         * This is where we will send the person our Credentials if they don't have them.
         * As well, we will read theirs.
         * @param results results of a successful scan.
         */
        private void processResults(Set<ScanResult> results) {
            JSONArray array = new JSONArray();
            for (ScanResult result : results) {
                JSONObject object = new JSONObject();
                try {
                    if(result.getScanRecord() != null) {
                        byte[] uuidBytes = result.getScanRecord().getManufacturerSpecificData(Constants.ID_MANUFACTURER);
                        if (uuidBytes == null) {
                            continue;
                        }
                        else{
                            object.put(Constants.JSON_KEY_UUID, Arrays.toString(uuidBytes));
                        }
                    }
                    else{
                        Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "Couldn't get UUID for " + result.getDevice().getAddress());
                        continue;
                    }
                    object.put(Constants.JSON_KEY_ADDRESS, result.getDevice().getAddress());
                    object.put(Constants.JSON_KEY_PUBLIC_KEY,null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                array.put(object);
            }

            Intent intent = new Intent(Constants.ACTION_SCAN_RESULTS);
            intent.putExtra(Constants.INTENT_EXTRA_RESULTS, array.toString());
            sendBroadcast(intent);

            bleServiceHandler.sendEmptyMessage(BleServiceHandler.WHAT_START_NEXT_SCAN);
            Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "Finished Processing Scan results");
        }
    }

    /**
     * Will start an attempt to read the Profile from the Connected Device.
     *
     * @param gatt
     */
    private void readProfileCharacteristic(BluetoothGatt gatt, BluetoothGattService service) {
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(Config.UUID_CHARACTERISTIC_FORWARD));
        if (gatt.readCharacteristic(characteristic)) {
            Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "\tSuccessfully initiated a read for " + Config.UUID_CHARACTERISTIC_FORWARD + " from " + gatt.getDevice().getAddress());
        } else {
            Log.e(ScanNearbyDevicesAsync.class.getSimpleName(), "\tFailed to initiate a read for " + Config.UUID_CHARACTERISTIC_FORWARD + " from " + gatt.getDevice().getAddress());
        }
    }


    /**
     * Sends the next packet in the queue
     * @return false if there are no packets in the queue
     */
    private Runnable sendRunnable = new Runnable() {
        private final Object lock = new Object();
        private final static String TAG = "SendRunnable";
        private GattServerService gattServerService;
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        @Override
        public void run() {
            if (sendQueue.isEmpty())
                return;
            BluetoothGatt mGatt = null;
            synchronized (lock) {
                try {
                    final QueueItem item = sendQueue.poll();
                    Log.d(TAG, "Sending a " + (item.isForward ? "forward" : "message") + " to " +
                            item.getMessage().address + ", text: " + item.getMessage().body);


                    //Get the Device Reference we want to talk to.
                    final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(item.getMessage().address);

                    //Packets we're going to send
                    final ArrayList<byte[]> packets = FTNLibrary.createPacketsForMessage(item.getMessage().toString(), true);

                    //A latch to tell us when we've connected. Once we're connected and have the services, we'll start
                    final CountDownLatch connectionLatch= new CountDownLatch(2);
                    //Allows us to continue when we have an MTU of the right size
                    final CountDownLatch mtuLatch       = new CountDownLatch(1);
                    //This barrier is what allows us to send the next packets after we hear a response from the server
                    final CountDownLatch sendPacketLatch= new CountDownLatch(packets.size());

                    final int[] mMtu = new int[1];
                    final byte[] uuid = Config.bytesFromString(item.getMessage().toUUID);

                    //Once we've connected, count down the connection latch to continue sending
                    final BluetoothGattCallback callback = new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            super.onConnectionStateChange(gatt, status, newState);
                            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                                Log.d(TAG, "\t\tConnected to: " + gatt.getDevice().getAddress());
                                connectionLatch.countDown();
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                gatt.discoverServices();
                            } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                                if(status == BluetoothGatt.GATT_SUCCESS){
                                    Log.d(TAG, "\t\tSuccessfully disconnected from the device");
                                }
                                else {
                                    Log.e(TAG, "\t\tRandomly disconnected... status: " + status);
                                    gatt.close();
                                    bluetoothGattMap.remove(uuid);
                                    connectionLatch.countDown();
                                    connectionLatch.countDown();
                                }
                            }
                            else{
                                Log.wtf(TAG, "I have no idea what happened... status=" + status + ", newState=" + newState);
                                item.setErrorSendFlag(true);
                                connectionLatch.countDown();
                                connectionLatch.countDown();
                            }
                        }

                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                            super.onServicesDiscovered(gatt, status);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.d(TAG, "\t\tObtained Services for " + gatt.getDevice().getAddress());
                                connectionLatch.countDown();
                            } else {
                                Log.e(TAG, "\t\tCouldn't obtain Services... " + status);
                                connectionLatch.countDown();
                            }
                        }

                        @Override
                        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                            super.onCharacteristicWrite(gatt, characteristic, status);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.d("SendRunnable", "\t\tSuccessfully Wrote Packet " + (packets.size() - sendPacketLatch.getCount()));
                                Log.d("SendRunnable", "\t\t\t" + Arrays.toString(packets.get(packets.size() - (int) sendPacketLatch.getCount())));
                                sendPacketLatch.countDown();
                            } else {
                                Log.e("SendRunnable", "\t\tWe couldn't send the message Successfully");
                                item.setErrorSendFlag(true);
                                sendPacketLatch.countDown();
                            }
                        }

                        @Override
                        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                            super.onMtuChanged(gatt, mtu, status);
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.d("SendRunnable", "\t\tSuccessfully got a new MTU size of " + mtu);
                                mMtu[0] = mtu;
                            } else {
                                Log.e("SendRunnable", "\t\tCouldn't get our MTU to the size we wanted... " + mtu);
                                mMtu[0] = mtu;
                            }
                            mtuLatch.countDown();
                        }
                    };

                    mBluetoothAdapter.cancelDiscovery();
                    if(!bluetoothGattMap.containsKey(uuid)){
                        mGatt = device.connectGatt(getApplicationContext(), false, callback, BluetoothDevice.TRANSPORT_LE);
                        bluetoothGattMap.put(uuid, mGatt);
                    }
                    else{
                        mGatt = bluetoothGattMap.get(uuid);
                        mGatt.connect();
                    }

                    //Wait for a state of Connected for 5000 ms. If we won't don't connect, end this;
                    try {
                        connectionLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(connectionLatch.getCount() != 0 || item.isErrorSendFlag()){
                        resetQueuedItem(item);
                        return;
                    }
                    if (mMtu[0] != 256) {
                        boolean usingMTU = mGatt.requestMtu(256);
                        if (usingMTU) {
                            Log.d("SendRunnable", "\t\tRequesting to use the MTU");
                        } else {
                            Log.d("SendRunnable", "\t\tCouldn't request to use the MTU");
                            packets.clear();
                            packets.addAll(FTNLibrary.createPacketsForMessage(item.getMessage().toString(), false));
                        }
                    }

                    try {
                        mtuLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                /* We failed... Requeue the Item */
                    if (mGatt == null || item.isErrorSendFlag()) {
                        resetQueuedItem(item);
                        return;
                    }

                    BluetoothGattService service = mGatt.getService(UUID.fromString(Config.UUID_SERVICE_PROFILE));

                /* We failed to get the services... requeue*/
                    if (service == null || item.isErrorSendFlag()) {
                        resetQueuedItem(item);
                        return;
                    }

                    //If we are going to forward the message, we'll send it to the specific forwarding characteristic
                    //  Else, we'll send it to the Messaging Service
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(
                            item.isForward() ? Config.UUID_CHARACTERISTIC_FORWARD : Config.UUID_CHARACTERISTIC_MESSAGE
                    ));

                    if (characteristic == null || item.isErrorSendFlag()) {
                        resetQueuedItem(item);
                        return;
                    }

                    //We're going to send packets. Wait until we get a response from the last one before sending again.
                    for (int i = 0; i < packets.size(); i++) {
                        characteristic.setValue(packets.get(i));
                        boolean ableToWrite = mGatt.writeCharacteristic(characteristic);
                        if (ableToWrite) {
                            Log.d(TAG, "\t\tWe were able to write packet " + (i + 1));
                        } else {
                            Log.d(TAG, "\t\tWe failed at writing packet " + (i + 1));
                            continue;
                        }
                        try {
                            sendPacketLatch.await();
                            if(sendPacketLatch.getCount() != packets.size() - i - 1){
                                Log.e(TAG, "\t\t Failed to send Packet... Requeing");
                                item.setErrorSendFlag(false);
                                sendQueue.add(item);
                                return;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    //We've Finished sending this Queue Item! Time to send the next one
                } finally{
                    if(mGatt != null) {
                        mGatt.disconnect();
                    }
                    bleServiceHandler.sendEmptyMessage(BleServiceHandler.WHAT_SEND_NEXT_IN_QUEUE);
                }
            }
        }
    };

    private void resetQueuedItem(QueueItem item){
        item.setErrorSendFlag(false);
        if(item.retryTimes < 3){
            item.retryTimes++;
            sendQueue.add(item);
            Log.d("SendRunnable", "Retry #" + item.retryTimes + " to send Message: " + item.getMessage().toString());
        }
        else{
            Log.e("BluetoothLeService", "Failed to send Message: " + item.getMessage().toString());
        }
    }

    /**
     * Will start scanning for device and pulling their basic info at a constant interval
     */
    public void startPeriodicScan(){
        bleServiceHandler.sendEmptyMessage(BleServiceHandler.WHAT_START_SCANNING);
    }

    public void stopPeriodicScan(){
        bleServiceHandler.sendEmptyMessage(BleServiceHandler.WHAT_STOP_SCANNING);
    }

    public String getMyAddress(){
        if(mBluetoothAdapter != null) {
            return mBluetoothAdapter.getAddress();
        }
        else{
            return null;
        }
    }

    /**
     * A Class which allows us to save our connection so we can map the callbacks to the device.
     */
    private class RememberingBluetoothGattCallback extends BluetoothGattCallback{
        private static final String TAG = "mBluetoothGattCallback";
        private BluetoothDevice device;

        private RememberingBluetoothGattCallback(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "\tFound Services!");

            for (BluetoothGattService service : gatt.getServices()) {
                Log.d(TAG, "\t\tFound Service: " + service.getUuid());
            }
        }

        /**
         * Will perform a get profile from the connected device in STATE_CONNECTED switch case
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Successfully connected to " + gatt.getDevice().getAddress());
            } else {
                Log.d(TAG, "Could not connect to " + gatt.getDevice().getAddress());
            }
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d(TAG, "\tState Change: Connected to " + gatt.getDevice().getAddress());
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d(TAG, "\tState Change: Disconnected from " + gatt.getDevice().getAddress());
                    break;
                default:
                    Log.d(TAG, "\tWhatNewState");
            }
        }

        /**
         * This is where we are handling getting other people's profile
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "\tSuccessfully read from gatt");
                switch (characteristic.getUuid().toString()) {
                    case Config.UUID_CHARACTERISTIC_FORWARD:
                        Log.d(TAG, "\t\tRead {'value':'" + characteristic.getStringValue(0) + "', 'uuid':'" + characteristic.getUuid() + "'}");
                        break;
                    default:
                        Log.d(TAG, "\t\tRead an unknown UUID: {'value':'" + characteristic.getStringValue(0) + "', 'uuid':'" + characteristic.getUuid() + "'}");
                }
            } else {
                Log.e(TAG, "\tCouldn't read from " + gatt.getDevice().getAddress() + " correctly");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "\tin onCharacteristicWrite");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "\tWrote Characteristic! {'value':'" + Arrays.toString(characteristic.getValue()) + "', 'uuid':'" + characteristic.getUuid() + "'}");
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.d(TAG, "In onReliableWriteCompleted");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "\tCompleted Reliable Write successfully!");
            }
        }
    }

    /**
     * An Item that is placed into the queue to send to a device.
     */
    private static class QueueItem{
        private String serviceUuid;
        private String characteristicUuid;
        private FTNLibrary.Message message;
        private boolean isForward;
        private boolean errorSendFlag = false;
        private int retryTimes = 0;

        /**
         * This is a Forward Request
         * @param message
         */
        public QueueItem(FTNLibrary.Message message) {
            this.message = message;
            this.serviceUuid = Config.UUID_SERVICE_PROFILE;
            if (isForward) {
                this.characteristicUuid = Config.UUID_CHARACTERISTIC_FORWARD;
            } else {
                this.characteristicUuid = Config.UUID_CHARACTERISTIC_MESSAGE;

            }
        }

        public String getCharacteristicUuid() {
            return characteristicUuid;
        }

        public String getServiceUuid() {
            return serviceUuid;
        }

        public FTNLibrary.Message getMessage() {
            return message;
        }

        public boolean isForward() {
            return isForward;
        }

        public void setIsForward(boolean isForward) {
            this.isForward = isForward;
        }

        public boolean isErrorSendFlag() {
            return errorSendFlag;
        }

        public void setErrorSendFlag(boolean errorSendFlag) {
            this.errorSendFlag = errorSendFlag;
        }
    }

    public class LocalBinder extends Binder{
        public BluetoothLeService getSendingServiceInstance(){
            return BluetoothLeService.this;
        }
    }

    /**
     * Send a Message to a certain device with a given message
     * @param message The Message Object that we will send to a device
     */
    public void sendMessage(FTNLibrary.Message message){
        QueueItem item = new QueueItem(message);
        item.getMessage().fromUUID = mUUID;
        sendQueue.add(item);
        bleServiceHandler.sendEmptyMessage(BleServiceHandler.WHAT_SEND_NEXT_IN_QUEUE);
    }
}