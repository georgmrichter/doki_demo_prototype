package de.georgrichter.vibrationdemoapp.util;

import java.util.Timer;
import java.util.TimerTask;

public class TaskUtils {
    public static Timer runDelayed(long delay, Runnable runnable){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, delay);
        return timer;
    }
}
