package vincente.com.pnib;

import android.bluetooth.BluetoothDevice;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by vincente on 3/10/16
 */
public class Config {

    public String UUID_APPLICATION = "fa87c0d0-afac-11de-8a39-0800200c9a66"; // Default UUID of the Application
    public static final String UUID_SERVICE_PROFILE = "e8e11222-0276-11e6-b512-3e1d05defe78";
    public static final String UUID_CHARACTERISTIC_FORWARD = "0bc87b4f-0f27-4a53-93ae-52cf788a85aa";
    public static final String UUID_CHARACTERISTIC_MESSAGE = "e8e119e8-0276-11e6-b512-3e1d05defe78";

    public String UUID_NAME = null;
    private String name = "TheFlash"; // Default Name of the Application
    private long scanLength = 10000; //Default 10 Seconds
    private long updatePeriod = 60000; // Time between updates

    private boolean isDebugging = false;
    private static Config instance = null;
    private ArrayList<BluetoothDevice> knownDevices;
    private String personal_uuid;
    private Config(){
        knownDevices = new ArrayList<>();
    }

    public static Config getInstance(){
        if(instance == null)
            instance = new Config();
        return instance;
    }

    public String getName() {
        return name;
    }

    public Config setName(String name) {
        this.name = name;
        return this;
    }

    public long getScanLength() {
        return scanLength;
    }

    public Config setScanLength(long scanLength) {
        this.scanLength = scanLength;
        return this;
    }

    public boolean isDebugging() {
        return isDebugging;
    }

    public Config setIsDebugging(boolean isDebugging){
        this.isDebugging = isDebugging;
        return this;
    }

    public long getUpdatePeriod() {
        return updatePeriod;
    }

    public void setUpdatePeriod(long updatePeriod) {
        this.updatePeriod = updatePeriod;
    }

    /**
     * Adds the device to the known devices list.
     * @param device Device we want to add
     * @return <p> Whether or not we added the device (if false, we already know the device and don't
     * need to write our key onto their device</p>
     */
    public boolean isInKnownDevices(BluetoothDevice device){
        for(BluetoothDevice knownDevice : knownDevices){
            if(knownDevice.getAddress().equals(device.getAddress()))
                return true;
        }
        return false;
    }

    /**
     * Add a device to our known list of devices
     * @param device Device to add to our known list.
     * @return Whether or not we added successfully.
     */
    public boolean addToKnownDevices(BluetoothDevice device){
        if(isInKnownDevices(device))
            return false;
        synchronized (knownDevices){
            knownDevices.add(device);
        }
        return true;
    }

    public String getPersonal_uuid() {
        return personal_uuid;
    }

    public void setPersonal_uuid(String personal_uuid) {
        this.personal_uuid = personal_uuid;
    }

    public static byte[] generateUUID(){
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static byte[] bytesFromString(String string) {
        String[] strings = string.replace("[", "").replace("]", "").split(", ");
        byte result[] = new byte[strings.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Byte.valueOf(strings[i]);
        }
        return result;
    }

}
