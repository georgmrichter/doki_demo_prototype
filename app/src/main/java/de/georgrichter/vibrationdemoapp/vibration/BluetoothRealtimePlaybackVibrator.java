package de.georgrichter.vibrationdemoapp.vibration;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

import de.georgrichter.vibrationdemoapp.BluetoothHandler;
import de.georgrichter.vibrationdemoapp.ContextProvider;
import de.georgrichter.vibrationdemoapp.util.BluetoothUtils;

public class BluetoothRealtimePlaybackVibrator extends VibrationProvider{
    private final BluetoothRealtimeVibrator realtimeVibrator;
    private final int durationMs;
    private Timer currentTimer;

    public BluetoothRealtimePlaybackVibrator(ContextProvider context,
                                             BluetoothHandler handler,
                                             Function<Float, Float> playbackFunc,
                                             int durationMs) {
        super(context);
        realtimeVibrator = new BluetoothRealtimeVibrator(context, handler, playbackFunc);
        this.durationMs = durationMs;
        currentTimer = null;
    }

    @Override
    public void vibrate() {
        if(currentTimer != null) stop();
        currentTimer = startVibrationTimer();
    }

    @Override
    public void stop() {
        currentTimer.cancel();
        currentTimer.purge();
        realtimeVibrator.stop();
    }

    private Timer startVibrationTimer(){
        final long timeStepMs = 10;
        final long iterationCount = durationMs / timeStepMs;
        final float valueStep = 1f / iterationCount;

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            private long i;
            @Override
            public void run() {
                float currentVal = i * valueStep;
                realtimeVibrator.vibrate(currentVal);
                i++;
                if(i >= iterationCount){
                    stop();
                }
            }
        }, 0, timeStepMs);
        return timer;
    }
}
