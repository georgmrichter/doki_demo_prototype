package de.georgrichter.vibrationdemoapp.packets;

import java.nio.ByteBuffer;

public class StatusPacket extends BluetoothPacket {
    public static final int STATUS_READY = 1;
    public int statusCode;

    public StatusPacket() {
        super(PACKET_TYPE_STATUS);
        statusCode = STATUS_READY;
    }

    @Override
    protected void setFromData(ByteBuffer buffer) {

    }

    @Override
    protected int getDataSize() {
        return Integer.BYTES;
    }

    @Override
    protected void addDataToBuffer(ByteBuffer buffer) {
        buffer.putInt(statusCode);
    }
}
