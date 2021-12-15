package de.georgrichter.vibrationdemoapp.vibration;

import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.georgrichter.vibrationdemoapp.ContextProvider;

import static android.content.Context.VIBRATOR_SERVICE;

public class PhoneVibrator extends VibrationProvider {
    private final Vibrator phoneVibrator;
    private final VibrationPattern pattern;

    public PhoneVibrator(ContextProvider context, VibrationPattern pattern){
        super(context);
        phoneVibrator = (Vibrator) context.getContext().getSystemService(VIBRATOR_SERVICE);
        this.pattern = pattern;
    }

    @Override
    public void vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playBackWhole(pattern);
        }
    }

    @Override
    public void stop() {
        phoneVibrator.cancel();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void playBackTimedChunks(VibrationPattern pattern){
        if(pattern.isRepeating())
            throw new UnsupportedOperationException("Cannot play repeating Pattern as timed chunks.");
        final long chunkSizeMs = 2000;
        ArrayList<VibrationEffect> vibrations = getVibrationChunks(pattern, chunkSizeMs);
        Timer timer = new Timer();
        for (int i = 0; i < vibrations.size(); i++) {
            VibrationEffect vibration = vibrations.get(i);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    phoneVibrator.cancel();
                    phoneVibrator.vibrate(vibration);
                }
            }, i * chunkSizeMs);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private ArrayList<VibrationEffect> getVibrationChunks(VibrationPattern pattern,
                                                          final long chunkLengthMs){
        // split pattern into chunks
        ArrayList<VibrationEffect> vibrations = new ArrayList<>();
        long currentTimeMs = 0;
        ArrayList<Long> timings = new ArrayList<>();
        ArrayList<Integer> amplitudes = new ArrayList<>();
        for (int i = 0; i < pattern.timings.length; i++) {
            timings.add(pattern.timings[i]);
            amplitudes.add(pattern.amplitudes[i]);
            currentTimeMs += pattern.timings[i];
            if(currentTimeMs >= chunkLengthMs){
                vibrations.add(VibrationEffect.createWaveform(
                        timings.stream().mapToLong(l -> l).toArray(),
                        amplitudes.stream().mapToInt(integer -> integer).toArray(),
                        -1
                ));
                timings.clear();
                amplitudes.clear();
                currentTimeMs = 0;
            }
        }
        if(!timings.isEmpty()){
            vibrations.add(VibrationEffect.createWaveform(
                    timings.stream().mapToLong(l -> l).toArray(),
                    amplitudes.stream().mapToInt(integer -> integer).toArray(),
                    -1
            ));
        }
        return vibrations;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void playBackWhole(VibrationPattern pattern){
        phoneVibrator.cancel();
        phoneVibrator.vibrate(pattern.toVibrationEffect());
    }
}
