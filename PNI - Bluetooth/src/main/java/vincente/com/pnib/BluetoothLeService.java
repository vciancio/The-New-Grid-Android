package vincente.com.pnib;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Created by vincente on 4/12/16
 * This is the service that will handle scanning for and updating our lower level service to send
 * and receive updates
 */
public class BluetoothLeService extends Service{
    private boolean init = false;
    private BluetoothAdapter mBluetoothAdapter;
    private PeriodicBleScanHandler periodicBleScanHandler;
    private static int counter = 0;
    private Config mConfig;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //If we've already Init, then we don't have to start the constant scan again.
        if(init)
            return super.onStartCommand(intent, flags, startId);

        periodicBleScanHandler = new PeriodicBleScanHandler(this);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if(!adapter.isEnabled()){
            Log.d(BluetoothLeService.class.getSimpleName(), "Bluetooth Adapter is not enabled, trying to enable...");
            if(!adapter.enable()){
                Toast.makeText(this, "Cannot turn on Bluetooth. Please do so.", Toast.LENGTH_SHORT).show();
                return super.onStartCommand(intent, flags, startId);
            }
        }

        //Initiate our new Config and setup our adapters. Start Scanning Now!
        mConfig = Config.getInstance();
        mBluetoothAdapter = adapter;
        periodicBleScanHandler.sendEmptyMessage(0);

        init = true;
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("HandlerLeak")
    private class PeriodicBleScanHandler extends Handler {
        private boolean isSendingMessages = false;
        private final Context context;

        private PeriodicBleScanHandler(Context context) {
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 0:
                    isSendingMessages = true;
                    new ScanNearbyDevicesAsync(context).execute();
                    break;
                default:
                    isSendingMessages = false;
            }
        }
    }

    /**
     * Scans for nearby devices so we can then query them and process them.
     */
    public class ScanNearbyDevicesAsync extends AsyncTask<Void, Void, Set<ScanResult>> {

        private Context context;
        private CountDownLatch connectionLatch;
        private CountDownLatch writeLatch;

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
            filters.add( filter );

            /* Setting up our Scan so we only have to find other people using our application */
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                    .build();

            final ScanCallback mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "Added ScanResult: " + result);
                    results.add(result);
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

            periodicBleScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bleScanner.stopScan(mScanCallback);
                    Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "Stopped Scanning");
                    processResults(results);
                    periodicBleScanHandler.sendEmptyMessage(0);
                }
            }, mConfig.getScanLength());
            return results;
        }

        /**
         * <p>
         *      Try to get the service data for newly connected devices.
         *          <li>Public Key</li>
         *          <li>UUIDs of other people he knows</li>
         * </p>
         * @param results results of a successful scan.
         */
        private void processResults(Set<ScanResult> results){
            for(ScanResult result : results){
                    connectionLatch = new CountDownLatch(1);
                BluetoothGatt gatt = result.getDevice().connectGatt(
                        context, false, gattConnectCallback, BluetoothDevice.TRANSPORT_LE);
                try {
                    connectionLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally{
                    gatt.disconnect();
                }
            }
            Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "Finished Processing Scan results");
        }

        private BluetoothGattCallback gattConnectCallback = new BluetoothGattCallback() {
            private static final String TAG = "mBluetoothGattCallback";

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.d(TAG, "\tFound Services!");

                for(BluetoothGattService service : gatt.getServices()){
                    Log.d(TAG, "\t\tFound Service: " + service.getUuid());
                    if(service.getUuid().toString().equals(Config.UUID_SERVICE_PROFILE)){
                        writeCharacteristic(gatt, service, "Hello World!");
                    }
                }
            }

            /**
             * Will perform a get profile from the connected device in STATE_CONNECTED switch case
             */
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if(status == BluetoothGatt.GATT_SUCCESS){
                    Log.d(TAG, "Successfully connected to " + gatt.getDevice().getAddress());
                }
                else{
                    Log.d(TAG, "Could not connect to " + gatt.getDevice().getAddress());
                }
                switch(newState){
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
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "\tSuccessfully read from gatt");
                    switch (characteristic.getUuid().toString()) {
                        case Config.UUID_CHARACTERISTIC_NAME:
                            Log.d(TAG, "\t\tRead {'value':'" + characteristic.getStringValue(0) + "', 'uuid':'" + characteristic.getUuid() + "'}");
                            break;
                        default:
                            Log.d(TAG, "\t\tRead an unknown UUID: {'value':'" + characteristic.getStringValue(0) + "', 'uuid':'" + characteristic.getUuid() + "'}");
                    }
                }
                else{
                    Log.e(TAG, "\tCouldn't read from " + gatt.getDevice().getAddress() + " correctly");
                }
                connectionLatch.countDown();
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "\tin onCharacteristicWrite");
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "\tWrote Characteristic! {'value':'" + Arrays.toString(characteristic.getValue()) + "', 'uuid':'" + characteristic.getUuid() + "'}");
                }
                writeLatch.countDown();
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                Log.d(TAG, "In onReliableWriteCompleted");
                if(status == BluetoothGatt.GATT_SUCCESS){
                    Log.d(TAG, "\tCompleted Reliable Write successfully!");
                }
            }
        };

        /**
         * Will start an attempt to read the Profile from the Connected Device.
         * @param gatt
         */
        private void readProfileCharacteristic(BluetoothGatt gatt, BluetoothGattService service){
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(Config.UUID_CHARACTERISTIC_NAME));
            if(gatt.readCharacteristic(characteristic)){
                Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "\tSuccessfully initiated a read for " + Config.UUID_CHARACTERISTIC_NAME + " from " + gatt.getDevice().getAddress());
            }
            else{
                Log.e(ScanNearbyDevicesAsync.class.getSimpleName(), "\tFailed to initiate a read for " + Config.UUID_CHARACTERISTIC_NAME + " from " + gatt.getDevice().getAddress());
                connectionLatch.countDown();
            }
        }

        /**
         * Writing to a characteristic.
         * @param gatt
         * @param service
         * @param value
         */
        private void writeCharacteristic(BluetoothGatt gatt, BluetoothGattService service, String value){
            BluetoothGattCharacteristic characteristic = null;
            for(BluetoothGattCharacteristic tempChar : service.getCharacteristics()){
                if(tempChar.getUuid().equals(UUID.fromString(Config.UUID_CHARACTERISTIC_WRITE))){
                    characteristic = tempChar;
                }
            }
            if(characteristic == null){
                Log.e(ScanNearbyDevicesAsync.class.getSimpleName(), "\tCouldn't find Write Characteristic...");
                connectionLatch.countDown();
                return;
            }

            /*
                Packet Init - 20 bytes
                |initial  |sequence#|      data|
                |---------|---------|----------|
                | 1 byte  | 2 bytes | 17 bytes |

                * Sequence # will be total amount of sequence numbers for the transaction.
                * Will be the size of the the message we are sending.
                * Data will be the size of the data we are sending
                * Initial will be set to '0x01'
             */

            /*
                Packet - 20 bytes total
                |initial  |sequence#|      data|
                |---------|---------|----------|
                | 1 byte  | 2 bytes | 17 bytes |

                * Initial will be set to '0x00'
                * Sequence number will align with which packet we're sending
                * Data is the data we will be sending
             */

            //Max length for sending packets
            final int PACKET_SIZE = 20;
            final int INIT_SIZE = 1;
            final int SEQUENCE_SIZE = 2;
            final int MAX_DATA_SIZE = PACKET_SIZE-INIT_SIZE-SEQUENCE_SIZE;

            ByteBuffer packet = ByteBuffer.allocate(20);
            ByteBuffer dataBuffer = ByteBuffer.wrap(value.getBytes());
            int dataLength  = dataBuffer.array().length;
            short numOfPackets = (short) (Math.ceil(dataLength/17)+1);

            Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "\tSending " + dataLength + " bytes over " + numOfPackets + " packets");
            for(short i=-1; i<numOfPackets; i++){
                Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "\tCan start reliably writing: " + gatt.beginReliableWrite());
                if(i==-1){
                    packet.put((byte)0x01);
                    packet.put(ByteBuffer.allocate(SEQUENCE_SIZE).putShort(numOfPackets).array());
                    packet.put(ByteBuffer.allocate(MAX_DATA_SIZE).putInt(dataLength).array());
                }
                else{
                    int sendDataLength = (dataLength-MAX_DATA_SIZE*i>MAX_DATA_SIZE ?MAX_DATA_SIZE:dataLength);
                    byte sendData[] = new byte[sendDataLength];
                    dataBuffer.get(sendData);
                    packet.put((byte) 0x00);
                    packet.put(ByteBuffer.allocate(SEQUENCE_SIZE).putShort(i).array());
                    packet.put(sendData);
                }

                byte[] sendingPacket = packet.array();
                packet.clear();

                characteristic.setValue(sendingPacket);

                Log.i(ScanNearbyDevicesAsync.class.getSimpleName(), "\tTrying to write data " + Arrays.toString(sendingPacket));

//                if(gatt.writeCharacteristic(characteristic)){
//                    Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "\t\tSuccessfully wrote sequence " + i);
//                }
//                else{
//                    Log.e(ScanNearbyDevicesAsync.class.getSimpleName(), "\t\tFailed to complete writeCharacteristic(characteristic)");
//                }
                Log.d(ScanNearbyDevicesAsync.class.getSimpleName(), "\tTrying to execute Reliable Write " + i + ": " + gatt.executeReliableWrite());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            connectionLatch.countDown();
        }
    }

    /**
     * Creates a queue of packets to be sent for a given message
     * @param message message to be packed up and sent
     * @return queue of packages for a given message
     */
    private static ArrayList<byte[]> createQueueForMessage(String message){
        ArrayList<byte[]> packets = null;
        return packets;
    }

    /**
     * Sends the next packet in the queue
     * @return false if we are sending the last packet, true if there's more packets to be sent
     */
    private boolean sendNextPacketInQueue(){
        return false;
    }

    /**
     * Will start scanning for device and pulling their basic info at a constant interval
     */
    private void startPeriodicScan(){
        periodicBleScanHandler.sendEmptyMessage(0);
    }
}