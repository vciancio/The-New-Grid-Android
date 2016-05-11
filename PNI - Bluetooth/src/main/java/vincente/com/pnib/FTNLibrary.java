package vincente.com.pnib;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by vincente on 5/8/16
 */
public class FTNLibrary {

    public static byte[] generateUUID() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static String stringFromBytes(byte[] bytes) {
        return Arrays.toString(bytes);
    }

    public static byte[] bytesFromString(String string) {
        String[] strings = string.replace("[", "").replace("]", "").split(", ");
        byte result[] = new byte[strings.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Byte.valueOf(strings[i]);
        }
        return result;
    }


    public static class Message {
        /**
         * The body that we are going to send
         */
        private byte[] body = null;
        private byte[] toUUID = null;
        private byte[] fromUUID = null;
        private String address = null;
        private byte isEncrypted = 0x0;

        /**
         * Total number of packets that we are sending
         */
        private short totalPacketNum = -1;

        /**
         * The number of packets we've recieved (This is for the GattServerService on the Recieving End)
         */
        private short packetsReceived = -1;

        /**
         * The maximum packet size
         */
        private short maxPacketSize = -1;

        public void setBody(byte[] body) {
            this.body = body;
            calculateTotalNumberOfPackets();
        }

        public void setBody(String body) {
            this.body = body.getBytes();
            calculateTotalNumberOfPackets();
        }

        public void calculateTotalNumberOfPackets() {
            if (maxPacketSize== -1 || body == null)
                return;
            totalPacketNum = (short) (
                    //Number of packets for just the mesage itself
                    Math.ceil(this.body.length / maxPacketSize)
                            //Account for the possibility of 2 init packets
                            + (maxPacketSize > 20 ? 2 : 1)
            );
            Log.d("Message", "Reset the message size to " + totalPacketNum);
        }

        public byte[] getBodyBytes() {
            return body;
        }

        public String getBody() {
            return new String(body);
        }

        public byte[] getToUUIDBytes() {
            return toUUID;
        }

        public String getToUUID() {
            return stringFromBytes(toUUID);
        }

        public void setToUUID(byte[] uuid) {
            this.toUUID = uuid;
        }

        public void setToUUID(String uuid) {
            this.toUUID = bytesFromString(uuid);
        }

        public byte[] getFromUUIDUUIDBytes() {
            return fromUUID;
        }

        public String getFromUUID() {
            return stringFromBytes(fromUUID);
        }

        public void setFromUUID(byte[] uuid) {
            this.fromUUID = uuid;
        }

        public void setFromUUID(String uuid) {
            this.fromUUID = bytesFromString(uuid);
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public byte getIsEncrypted() {
            return isEncrypted;
        }

        public void setIsEncrypted(byte isEncrypted) {
            this.isEncrypted = isEncrypted;
        }

        public void setIsEncrypted(boolean isEncrypted) {
            this.isEncrypted = (byte) (isEncrypted ? 0x1 : 0x0);
        }

        public short getTotalPacketNum() {
            return totalPacketNum;
        }

        public void setTotalPacketNum(short totalPacketNum) {
            this.totalPacketNum = totalPacketNum;
        }

        public short getPacketsReceived() {
            return packetsReceived;
        }

        public void setPacketsReceived(short packetsReceived) {
            this.packetsReceived = packetsReceived;
        }

        public short getMaxPacketSize() {
            return maxPacketSize;
        }

        public void setMaxPacketSize(short maxPacketSize) {
            this.maxPacketSize = maxPacketSize;
            calculateTotalNumberOfPackets();
        }

        @Override
        public String toString() {
            JSONObject object = new JSONObject();
            try {
                object.put(Constants.JSON_KEY_BODY, body);
                object.put(Constants.JSON_KEY_TO_UUID, toUUID);
                object.put(Constants.JSON_KEY_FROM_UUID, fromUUID);
                object.put(Constants.JSON_KEY_ADDRESS, address);
                object.put(Constants.JSON_KEY_ENCRYPTED, isEncrypted);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return object.toString();
        }

        /**
         * Gets all the packets needed for sending the message. Includes Init
         * @return The packets to send to the other Bluetooth Device.
         */
        public ArrayList<MessagePacket> getPackets() {
            ArrayList<MessagePacket> packets = new ArrayList<>();

            MessagePacket packet = getInitLargePacket();
            packets.add(packet);

            //Max data size - header size
            int dataInPacketSize = maxPacketSize - 3;
            ByteBuffer dataBuffer = ByteBuffer.wrap(body);
            for (int i = 1; i < totalPacketNum - 1; i++) {
                MessagePacket dataPacket = new MessagePacket();
                dataPacket.init = 0x0;
                dataPacket.currentPacketNum = (short) i;

                //Figure out how many bytes we can hold with this data
                int currentDataLength = (body.length - i * dataInPacketSize > dataInPacketSize) ?
                        dataInPacketSize : body.length;

                //Allocate the amount of data we'll carry through
                ByteBuffer buffer = ByteBuffer.allocate(currentDataLength);

                //Getting the Amount of bytes we want from the Buffer
                byte[] bytes = new byte[currentDataLength - 3];
                dataBuffer.get(currentDataLength);
                buffer.put(bytes);
                dataPacket.data = buffer.array();
                packets.add(dataPacket);
            }
            return packets;
        }

        /**
         * Get the Init packet for a large MTU
         * @return
         */
        private MessagePacket getInitLargePacket() {
            MessagePacket packet = new MessagePacket();
            packet.init = 1;
            packet.currentPacketNum = 0;
            packet.totalPacketNum = this.totalPacketNum;
            packet.dataSize = this.body.length;
            packet.fromUUID = this.fromUUID;
            packet.toUUID = this.toUUID;
            packet.maxSize = maxPacketSize;
            return packet;
        }

        /*
         Packet Init - If max < 20, will send 2 init packets. Else, it will contain both to and from and Size
            | initial | sequence# |  #of packets | dataSize | fromUUID |   toUUID   |
            |---------|-----------|--------------|----------|----------|------------|
            | 1 byte  |  2 byte   |    2 byte    |  4 bytes | 16 bytes |  16 bytes  |

         If there's 2 init packets
            | initial | sequence# |  #of packets | fromUUID |
            |---------|-----------|--------------|----------|
            | 1 byte  |  2 byte   |    2 byte    | 16 bytes |


            | initial | sequence# |   toUUID   |
            |---------|-----------|------------|
            | 1 byte  |  2 byte   |  16 bytes  |


         Packet Not-Init
            | initial | sequence# |  data  |
            |---------|-----------|--------|
            | 1 byte  |  2 bytes  | x bytes|
         */


        public class MessagePacket {
            public byte init;
            public Short currentPacketNum;
            public Short totalPacketNum;
            public byte fromUUID[];
            public byte toUUID[];
            public int offset;
            public byte[] data;
            public Integer dataSize;
            public short maxSize;

            public MessagePacket() {
            }

            public MessagePacket(byte[] bytes) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                boolean multiInit = bytes.length <= 20;
                init = buffer.get();
                currentPacketNum = buffer.getShort();

                //TODO: HACK - Don't handle Multi-Init
                if (init == 0x1) {
                    totalPacketNum = buffer.getShort();

                    this.dataSize = buffer.getInt();

                    this.fromUUID = new byte[16];
                    buffer.get(this.fromUUID);

                    this.toUUID = new byte[16];
                    buffer.get(toUUID);
                } else if (init == 0x0) {
                    //The rest of the bytes that will be put into data
                    data = new byte[(bytes.length - 1) - buffer.arrayOffset()];
                }
            }

            public byte[] getBytes() {
                int packetSize = 0;
                ByteBuffer buffer = ByteBuffer.wrap(new byte[maxSize]);

                buffer.put(init);
                packetSize += 1;
                buffer.putShort(currentPacketNum.byteValue());
                packetSize += 2;

                if (totalPacketNum != null) {
                    buffer.putShort(totalPacketNum);
                    packetSize += 2;
                }
                if (dataSize != null) {
                    buffer.putInt(dataSize);
                    packetSize += 4;
                }
                if (fromUUID != null) {
                    buffer.put(fromUUID);
                    packetSize += 16;
                }
                if (toUUID != null) {
                    buffer.put(toUUID);
                    packetSize += 16;
                }
                if (data != null) {
                    buffer.put(data);
                    this.offset = packetSize;
                    packetSize += data.length;
                }

                //If its less than the max size, we need to shrink the buffer.
                if (packetSize < maxSize) {
                    ByteBuffer temp = ByteBuffer.wrap(new byte[packetSize]);
                    for (int i = 0; i < packetSize; i++) {
                        temp.put(buffer.get(i));
                    }
                    return temp.array();
                }
                return buffer.array();
            }

            public int getOffset() {
                return offset;
            }
        }
    }


    /**
     * Creates a queue of packets to be sent for a given message
     *
     * @param value message to be packed up and sent
     * @return queue of packages for a given message
     */
    public static ArrayList<byte[]> createPacketsForMessage(String value, boolean withMTU) {
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
        final int PACKET_SIZE = withMTU ? 256 : 20;
        final int INIT_SIZE = 1;
        final int SEQUENCE_SIZE = 2;
        final int MAX_DATA_SIZE = PACKET_SIZE - INIT_SIZE - SEQUENCE_SIZE;

        ByteBuffer packet = ByteBuffer.allocate(PACKET_SIZE);
        ByteBuffer dataBuffer = ByteBuffer.wrap(value.getBytes());
        int dataLength = dataBuffer.array().length;
        short numOfPackets = (short) (Math.ceil(dataLength / MAX_DATA_SIZE) + 1);

        Log.d(FTNLibrary.class.getSimpleName(), "\tCreating packets for " + dataLength + " bytes over " + numOfPackets + " packets");
        for (short i = 0; i < numOfPackets; i++) {
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
