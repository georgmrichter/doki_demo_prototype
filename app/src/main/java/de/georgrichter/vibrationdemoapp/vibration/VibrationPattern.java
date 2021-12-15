package de.georgrichter.vibrationdemoapp.vibration;

import android.os.Build;
import android.os.VibrationEffect;

import androidx.annotation.RequiresApi;

import java.io.InputStream;
import java.nio.ByteBuffer;

import de.georgrichter.vibrationdemoapp.ContextProvider;

public class VibrationPattern {
    public int[] amplitudes;
    public long[] timings;
    public int repeat;

    public VibrationPattern(){
        this(new long[0], new int[0], -1);
    }

    public VibrationPattern(long[] timings, int[] amplitudes){
        this(timings, amplitudes, -1);
    }

    public VibrationPattern(long[] timings, int[] amplitudes, int repeat){
        this.timings = timings;
        this.amplitudes = amplitudes;
        this.repeat = repeat;
    }

    public static VibrationPattern fromVibFile(ContextProvider contextProvider, int resourceID){
        try {
            InputStream stream = contextProvider.getContext().getResources().openRawResource(resourceID);
            // read first 4 bytes as int
            byte[] length = new byte[4];
            int read = stream.read(length, 0, length.length);
            if(read != 4) return null;
            length = new byte[]{ length[3], length[2], length[1], length[0] };
            int contentLength = ByteBuffer.wrap(length).getInt();
            long[] timings = new long[contentLength / 2];
            int[] amplitudes = new int[contentLength / 2];
            for (int i = 0; i < timings.length; i++){
                timings[i] = ((byte)stream.read() & 0xFF);
                amplitudes[i] = ((byte)stream.read() & 0xFF);
            }
            return new VibrationPattern(timings, amplitudes);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public boolean isRepeating(){
        return repeat >= 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public VibrationEffect toVibrationEffect(){
        return VibrationEffect.createWaveform(timings, amplitudes, repeat);
    }
}
