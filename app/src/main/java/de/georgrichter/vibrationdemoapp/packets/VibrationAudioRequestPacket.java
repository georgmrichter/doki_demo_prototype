package de.georgrichter.vibrationdemoapp.packets;

import java.nio.ByteBuffer;

public class VibrationAudioRequestPacket extends BluetoothPacket {
    public static final int REQUEST_NEXT_CHUNK = 1;
    public static final int REQUEST_STOP_PLAYBACK = 2;
    public int requestType;

    public VibrationAudioRequestPacket() {
        this(REQUEST_NEXT_CHUNK);
    }

    public VibrationAudioRequestPacket(int requestType){
        super(PACKET_TYPE_VIBRATION_AUDIO_REQUEST);
        this.requestType = requestType;
    }

    @Override
    protected void setFromData(ByteBuffer buffer) {
        byte[] bytes = new byte[Integer.BYTES];
        buffer.get(bytes);
        requestType = byteArrayToInt(bytes);
    }

    @Override
    protected int getDataSize() {
        return Integer.BYTES;
    }

    @Override
    protected void addDataToBuffer(ByteBuffer buffer) {
        buffer.put(intToByteArray(requestType));
    }
}
