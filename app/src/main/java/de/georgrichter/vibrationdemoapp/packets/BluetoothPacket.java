package de.georgrichter.vibrationdemoapp.packets;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public abstract class BluetoothPacket {
    private interface BTPacketConstructor {
        BluetoothPacket ctor();
    }

    public static final int PACKET_TYPE_TEST = 1;
    public static final int PACKET_TYPE_STATUS = 2;
    public static final int PACKET_TYPE_VIBRATION_EFFECT = 3;
    public static final int PACKET_TYPE_VIBRATION_PATTERN = 4;
    public static final int PACKET_TYPE_VIBRATION_AUDIO = 5;
    public static final int PACKET_TYPE_VIBRATION_AUDIO_REQUEST = 6;
    public static final int PACKET_TYPE_VIBRATION_REALTIME = 7;

    protected static final Charset CHARSET = StandardCharsets.ISO_8859_1;

    private static final HashMap<Integer, BTPacketConstructor> CONSTRUCTORS = new HashMap<>();

    static {
        CONSTRUCTORS.put(PACKET_TYPE_TEST, TestPacket::new);
        CONSTRUCTORS.put(PACKET_TYPE_STATUS, StatusPacket::new);
        CONSTRUCTORS.put(PACKET_TYPE_VIBRATION_EFFECT, VibrationEffectPacket::new);
        CONSTRUCTORS.put(PACKET_TYPE_VIBRATION_PATTERN, VibrationPatternPacket::new);
        CONSTRUCTORS.put(PACKET_TYPE_VIBRATION_AUDIO, VibrationAudioPacket::new);
        CONSTRUCTORS.put(PACKET_TYPE_VIBRATION_AUDIO_REQUEST, VibrationAudioRequestPacket::new);
        CONSTRUCTORS.put(PACKET_TYPE_VIBRATION_REALTIME, VibrationRealtimePacket::new);
    }

    public int packetType;
    public int dataLength;

    public BluetoothPacket(int packetType) {
        this.packetType = packetType;
    }

    public byte[] getData() {
        dataLength = getDataSize();
        ByteBuffer buffer = ByteBuffer.allocate(2 * Integer.BYTES + dataLength);
        buffer.put(intToByteArray(packetType));
        buffer.put(intToByteArray(dataLength));
        addDataToBuffer(buffer);
        return buffer.array();
    }

    public static ArrayList<BluetoothPacket> fromData(byte[] data) throws Exception{
        ByteBuffer buffer = ByteBuffer.wrap(data);
        ArrayList<BluetoothPacket> packets = new ArrayList<>();
        while (buffer.hasRemaining()){
            byte[] tmp = new byte[Integer.BYTES];
            buffer.get(tmp);
            int packetType = byteArrayToInt(tmp);
            if (!CONSTRUCTORS.containsKey(packetType)) {
                throw new Exception("Received unknown packet type (" + packetType + ").");
            }
            BTPacketConstructor constructor = CONSTRUCTORS.get(packetType);
            BluetoothPacket packet = Objects.requireNonNull(constructor).ctor();
            buffer.get(tmp);
            packet.dataLength = byteArrayToInt(tmp);
            packet.setFromData(buffer);
            packets.add(packet);
        }
        return packets;
    }

    protected static void reverseByteArray(byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    protected static byte[] intToByteArray(int i) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES).putInt(i);
        byte[] array = buffer.array();
        reverseByteArray(array);
        return array;
    }

    protected static int byteArrayToInt(byte[] src) {
        reverseByteArray(src);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES).put(src);
        buffer.rewind();
        return buffer.getInt();
    }

    protected abstract void setFromData(ByteBuffer buffer);

    protected abstract int getDataSize();

    protected abstract void addDataToBuffer(ByteBuffer buffer);

}
