package de.georgrichter.vibrationdemoapp.packets;

import java.nio.ByteBuffer;

public class TestPacket extends BluetoothPacket {
    public String message;

    public TestPacket() {
        this("");
    }

    public TestPacket(String message) {
        super(PACKET_TYPE_TEST);
        this.message = message;
    }

    @Override
    protected void setFromData(ByteBuffer buffer) {
        int length = buffer.getInt();
        byte[] bytes = new byte[length - 1]; // remove null termination
        buffer.get(bytes, 0, length - 1);
        message = new String(bytes, CHARSET);
    }

    @Override
    protected int getDataSize() {
        int dataSize = Integer.BYTES * 2;
        if (message != null) dataSize += message.length();
        return dataSize;
    }

    @Override
    protected void addDataToBuffer(ByteBuffer buffer) {
        int strLen = message == null ? 1 : message.length() + 1; //null terminated
        buffer.put(BluetoothPacket.intToByteArray(strLen));
        if (message != null) {
            buffer.put(message.getBytes(CHARSET));
        }
        buffer.put((byte) 0); // null terminate the string
    }
}
