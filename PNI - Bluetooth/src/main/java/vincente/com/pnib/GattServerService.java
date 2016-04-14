package vincente.com.pnib;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.Log;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Created by vincente on 4/14/16
 */
public class GattServerService extends Service {
    private boolean init = false;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer server;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(init)
            return super.onStartCommand(intent, flags, startId);
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isEnabled() && !mBluetoothAdapter.enable()){
            Log.e(GattServerService.class.getSimpleName(), "We can't start the bluetooth service");
            return super.onStartCommand(intent, flags, startId);
        }

        init = true;
        startGattServer();
        startGattAdvertising();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Advertises our device to the outside world with our application uuid
     */
    private void startGattAdvertising(){
        //Setup Advertisement
        BluetoothLeAdvertiser advertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build();

        ParcelUuid mApplicationParcelUUID = new ParcelUuid(UUID.fromString(Config.getInstance().UUID_APPLICATION));
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(mApplicationParcelUUID)
                .addServiceData(mApplicationParcelUUID, "FTN".getBytes(Charset.forName("UTF-8")))
                .build();
        AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d(GattServerService.class.getSimpleName(), "Started Advertising!");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e(GattServerService.class.getSimpleName(), "Failed to start advertising: error code " + errorCode);
            }
        };

        advertiser.startAdvertising(settings, advertiseData, advertiseCallback);
    }

    /**
     * This is where we handle sending other devices our information
     */
    private void startGattServer(){
        server = mBluetoothManager.openGattServer(this, serverCallback);

        //Create the profile service
        BluetoothGattService profileService = new BluetoothGattService(
                UUID.fromString(Config.UUID_SERVICE_PROFILE),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic nameCharacteristic = new BluetoothGattCharacteristic(
                UUID.fromString(Config.UUID_CHARACTERISTIC_NAME),
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        nameCharacteristic.setValue("TNG");


        BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(
                UUID.fromString(Config.UUID_CHARACTERISTIC_WRITE),
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        profileService.addCharacteristic(nameCharacteristic);
        profileService.addCharacteristic(writeCharacteristic);
        server.addService(profileService);
    }

    BluetoothGattServerCallback serverCallback = new BluetoothGattServerCallback() {
        private static final String TAG = "BluetoothGattServer";
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d(TAG, "Connected to: " + device.getAddress());
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d(TAG, "Disconnected from: " + device.getAddress());
                    break;
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d(TAG, "Service was " + (status== BluetoothGatt.GATT_SUCCESS?"":"not")+ " added: " + service.getUuid());
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "Characteristic was read! " + characteristic.getUuid() + ", returning " + characteristic.getStringValue(0));
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d(TAG, "Someone tried to write a characteristic: " + "{'address':'" + device.getAddress()+"', 'value':'" + new String(value, StandardCharsets.UTF_8));
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }
    };
}
