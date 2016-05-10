package vincente.com.pnib;

/**
 * Created by vincente on 5/8/16
 */
public class Constants {



    private Constants(){}

    public static final String INTENT_EXTRA_RESULTS = "results";

    public static final String JSON_KEY_BODY = "body";
    public static final String JSON_KEY_UUID = "uuid";
    public static final String JSON_KEY_TO_UUID = "toUUID";
    public static final String JSON_KEY_FROM_UUID = "fromUUID";
    public static final String JSON_KEY_ADDRESS = "address";
    public static final String JSON_KEY_FORWARD = "isForward";
    public static final String JSON_KEY_ENCRYPTED = "encrypted";
    public static final String JSON_KEY_PUBLIC_KEY = "public_key";

    public static final String PREF_MY_UUID = "mUUID";

    public static final int ID_MANUFACTURER = 3309;

    /**
     * Broadcasts the Results of the Scan. It is up to the App dev to determine what to do with them
     * Extra as String "results":
     *  {
     *      'address': 'Address stored as a string',
     *      'public_key': 'Public Key (If one) stored as a string'
     *  }
     */
    public static final String ACTION_SCAN_RESULTS = "com.ftn.action.SCAN_RESULTS";

    /**
     * Broadcasts the Message that was received by the server.
     * Extra as String "results":
     * {
     *     'address': 'Address stored as a string',
     *     'encrypted': boolean,
     *     'body': 'message contents stored as a string'
     * }
     */
    public static final String ACTION_RECEIVED_MESSAGE = "com.ftn.action.RECEIVED_MESSAGE";
}
