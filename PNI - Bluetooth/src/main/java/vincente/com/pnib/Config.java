package vincente.com.pnib;

/**
 * Created by vincente on 3/10/16
 */
public class Config {

    public String UUID_APPLICATION = "fa87c0d0-afac-11de-8a39-0800200c9a66"; // Default UUID of the Application
    public static final String UUID_SERVICE_PROFILE = "e8e11222-0276-11e6-b512-3e1d05defe78";
    public static final String UUID_CHARACTERISTIC_NAME = "0bc87b4f-0f27-4a53-93ae-52cf788a85aa";
    public static final String UUID_CHARACTERISTIC_WRITE = "e8e119e8-0276-11e6-b512-3e1d05defe78";

    public String UUID_NAME = null;
    private String name = "TheFlash"; // Default Name of the Application
    private long scanLength = 10000; //Default 10 Seconds
    private long updatePeriod = 60000; // Time between updates

    private boolean isDebugging = false;
    private static Config instance = null;

    private Config(){}

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
}
