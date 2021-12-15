package de.georgrichter.vibrationdemoapp.packets;

import java.nio.ByteBuffer;

public class VibrationEffectPacket extends BluetoothPacket {
    public byte[] effectIds;

    public VibrationEffectPacket() {
        this(1);
    }

    public VibrationEffectPacket(int... effectIds) {
        super(PACKET_TYPE_VIBRATION_EFFECT);
        byte[] bytes = new byte[effectIds.length];
        for (int i = 0; i < effectIds.length; i++) {
            if (effectIds[i] > Byte.MAX_VALUE)
                throw new IllegalArgumentException("Effect id (" + effectIds[i] + ") can not be greater than " + Byte.MAX_VALUE + ".");
            else {
                bytes[i] = (byte) effectIds[i];
            }
        }
        this.effectIds = bytes;
    }

    public VibrationEffectPacket(byte... effectIds) {
        super(PACKET_TYPE_VIBRATION_EFFECT);
        this.effectIds = effectIds;
    }

    @Override
    protected void setFromData(ByteBuffer buffer) {
        byte[] array = new byte[Integer.BYTES];
        buffer.get(array);
        int length = BluetoothPacket.byteArrayToInt(array);
        effectIds = new byte[length];
        buffer.get(effectIds);
    }

    @Override
    protected int getDataSize() {
        return Integer.BYTES + Byte.BYTES * effectIds.length;
    }

    @Override
    protected void addDataToBuffer(ByteBuffer buffer) {
        buffer.put(BluetoothPacket.intToByteArray(effectIds.length));
        buffer.put(effectIds);
    }
}
