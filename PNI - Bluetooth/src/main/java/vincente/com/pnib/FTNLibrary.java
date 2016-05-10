package vincente.com.pnib;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by vincente on 5/8/16
 */
public class FTNLibrary {

    public static class Message{
        public String body;
        public String uuid;
        public String address;
        public boolean isEncrypted;

        @Override
        public String toString() {
            JSONObject object = new JSONObject();
            try {
                object.put(Constants.JSON_KEY_BODY, body);
                object.put(Constants.JSON_KEY_UUID, uuid);
                object.put(Constants.JSON_KEY_ADDRESS, address);
                object.put(Constants.JSON_KEY_ENCRYPTED, isEncrypted);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return object.toString();
        }
    }

    /**
     * Creates a queue of packets to be sent for a given message
     * @param value message to be packed up and sent
     * @return queue of packages for a given message
     */
    public static ArrayList<byte[]> createPacketsForMessage(String value, boolean withMTU){
        ArrayList<byte[]> packets = new ArrayList<>();
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
        final int PACKET_SIZE = withMTU?256:20;
        final int INIT_SIZE = 1;
        final int SEQUENCE_SIZE = 2;
        final int MAX_DATA_SIZE = PACKET_SIZE-INIT_SIZE-SEQUENCE_SIZE;

        ByteBuffer packet = ByteBuffer.allocate(PACKET_SIZE);
        ByteBuffer dataBuffer = ByteBuffer.wrap(value.getBytes());
        int dataLength  = dataBuffer.array().length;
        short numOfPackets = (short) (Math.ceil(dataLength/MAX_DATA_SIZE)+1);

        Log.d(FTNLibrary.class.getSimpleName(), "\tCreating packets for " + dataLength + " bytes over " + numOfPackets + " packets");
        for(short i=0; i<numOfPackets; i++) {
/*
            if (i == -1) {
                packet.put((byte) 0x01);
                packet.put(ByteBuffer.allocate(SEQUENCE_SIZE).putShort(numOfPackets).array());
                packet.put(ByteBuffer.allocate(MAX_DATA_SIZE).putInt(dataLength).array());
            } else {
                int sendDataLength = (dataLength - MAX_DATA_SIZE * i > MAX_DATA_SIZE ? MAX_DATA_SIZE : dataLength);
                byte sendData[] = new byte[sendDataLength];
                dataBuffer.get(sendData);
                packet.put((byte) 0x00);
                packet.put(ByteBuffer.allocate(SEQUENCE_SIZE).putShort(i).array());
                packet.put(sendData);
            }
            packets.add(packet.array());
*/
            packets.add(value.getBytes());
            packet.clear();
        }
        return packets;
    }
}
