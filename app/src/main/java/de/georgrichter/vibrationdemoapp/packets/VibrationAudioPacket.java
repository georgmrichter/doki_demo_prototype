package de.georgrichter.vibrationdemoapp.packets;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class VibrationAudioPacket extends BluetoothPacket {
    private Context context;
    private byte[] data;
    private int soundId;
    private int chunkId;

    public VibrationAudioPacket() {
        super(PACKET_TYPE_VIBRATION_AUDIO);
    }

    public VibrationAudioPacket(Context context, int resourceId, int soundId, int chunkId) {
        super(PACKET_TYPE_VIBRATION_AUDIO);
        this.context = context;
        this.soundId = soundId;
        this.chunkId = chunkId;
        readData(resourceId);
    }

    public VibrationAudioPacket(Context context, byte[] data, int soundId, int chunkId) {
        super(PACKET_TYPE_VIBRATION_AUDIO);
        this.context = context;
        this.data = data;
        this.soundId = soundId;
        this.chunkId = chunkId;
    }

    public static ArrayList<VibrationAudioPacket> getSplits(VibrationAudioPacket packet){
        final int maxSize = 255;
        byte[] allData = packet.data;
        ArrayList<byte[]> splitData = new ArrayList<>();
        int read = 0;
        while (read < allData.length){
            int offset = Math.min(allData.length - read, maxSize);
            splitData.add(Arrays.copyOfRange(allData, read, read + offset));
            read += offset;
        }
        ArrayList<VibrationAudioPacket> packets = new ArrayList<>();
        for (byte[] d : splitData) {
            packets.add(new VibrationAudioPacket(packet.context, d, packet.soundId, packet.chunkId));
        }
        return packets;
    }

    @Override
    protected void setFromData(ByteBuffer buffer) {
    }

    @Override
    protected int getDataSize() {
        return Integer.BYTES * 3 + data.length;
    }

    @Override
    protected void addDataToBuffer(ByteBuffer buffer) {
        buffer.put(intToByteArray(soundId));
        buffer.put(intToByteArray(chunkId));
        buffer.put(intToByteArray(data.length));
        buffer.put(data);
    }

    private void readData(int resourceId){
        try {
            InputStream inStream = context.getResources().openRawResource(resourceId);
            data = convertStreamToByteArray(inStream);
            inStream.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static byte[] convertStreamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[10240];
        int i = Integer.MAX_VALUE;
        while ((i = is.read(buff, 0, buff.length)) > 0) {
            baos.write(buff, 0, i);
        }

        return baos.toByteArray(); // be sure to close InputStream in calling function
    }
}
