package de.georgrichter.vibrationdemoapp.packets;

import java.nio.ByteBuffer;

public class VibrationRealtimePacket extends BluetoothPacket{
    public short realtimeValue;

    public VibrationRealtimePacket() {
        super(PACKET_TYPE_VIBRATION_REALTIME);
    }

    public VibrationRealtimePacket(short realtimeValue) {
        super(PACKET_TYPE_VIBRATION_REALTIME);
        this.realtimeValue = realtimeValue;
    }

    @Override
    protected void setFromData(ByteBuffer buffer) {
        byte[] tmp = new byte[Short.BYTES];
        buffer.get(tmp);
        reverseByteArray(tmp);
        ByteBuffer tmpBuffer = ByteBuffer.allocate(Short.BYTES).put(tmp);
        realtimeValue = tmpBuffer.getShort();
    }

    @Override
    protected int getDataSize() {
        return Short.BYTES;
    }

    @Override
    protected void addDataToBuffer(ByteBuffer buffer) {
        ByteBuffer b = ByteBuffer.allocate(Short.BYTES).putShort(realtimeValue);
        byte[] array = b.array();
        reverseByteArray(array);
        buffer.put(array);
    }
}
