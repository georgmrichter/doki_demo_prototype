package de.georgrichter.vibrationdemoapp.packets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import de.georgrichter.vibrationdemoapp.ContextProvider;
import de.georgrichter.vibrationdemoapp.util.IOUtils;

public class VibrationPatternPacket extends BluetoothPacket {
    public static final int STATUS_PLAY = 1;
    public static final int STATUS_STOP = 2;
    public static final int STATUS_RESUME = 3;

    public int status;
    public byte[] data;

    public VibrationPatternPacket(){
        super(PACKET_TYPE_VIBRATION_PATTERN);
    }

    private VibrationPatternPacket(ContextProvider contextProvider, int status, int resourceID) {
        super(PACKET_TYPE_VIBRATION_PATTERN);
        this.status = status;
        if(status == STATUS_PLAY) readData(contextProvider, resourceID);
    }

    public static VibrationPatternPacket getPlayPacket(ContextProvider contextProvider, int resourceID){
        return new VibrationPatternPacket(contextProvider, STATUS_PLAY, resourceID);
    }

    public static VibrationPatternPacket getResumePacket(){
        return new VibrationPatternPacket(null, STATUS_RESUME, 0);
    }

    public static VibrationPatternPacket getStopPacket(){
        return new VibrationPatternPacket(null, STATUS_STOP, 0);
    }

    private void readData(ContextProvider contextProvider, int resourceID){
        try {
            InputStream stream = contextProvider.getContext().getResources().openRawResource(resourceID);
            data = IOUtils.readAllBytes(stream);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void setFromData(ByteBuffer buffer) {
        byte[] tmp = new byte[Integer.BYTES];
        buffer.get(tmp);
        int status = byteArrayToInt(tmp);
        if(status != STATUS_PLAY) return;
        buffer.get(tmp);
        int length = byteArrayToInt(tmp);
        data = new byte[length];
        buffer.get(data);
    }

    @Override
    protected int getDataSize() {
        int size = Integer.BYTES;
        if(status == STATUS_PLAY) size += Integer.BYTES + data.length;
        return size;
    }

    @Override
    protected void addDataToBuffer(ByteBuffer buffer) {
        buffer.put(intToByteArray(status));
        if(status == STATUS_PLAY) {
            buffer.put(intToByteArray(data.length));
            buffer.put(data);
        }
    }
}
