package de.georgrichter.vibrationdemoapp.audio;

import android.media.MediaPlayer;
import android.os.Build;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import de.georgrichter.vibrationdemoapp.ContextProvider;
import de.georgrichter.vibrationdemoapp.util.TaskUtils;

public class MultiSoundManager {
    public interface SoundCompletedCallback{
        void onSoundCompleted(String sound);
    }

    private final ContextProvider context;
    private final HashMap<String, MediaPlayer> players;
    private final HashMap<String, Float> volumes;

    private SoundCompletedCallback soundCompletedCallback;

    public MultiSoundManager(ContextProvider context, SoundCompletedCallback soundCompletedCallback){
        this.context = context;
        this.soundCompletedCallback = soundCompletedCallback;
        players = new HashMap<>();
        volumes = new HashMap<>();
    }

    public static float interpolateLogistic(float x){
        return (float)(1 / (1 + Math.exp(-10 * (x - 0.5))));
    }

    public void disposeAllPlayers(){
        for (MediaPlayer player : players.values()) {
            player.stop();
            player.release();
        }
        players.clear();
        volumes.clear();
    }

    public void addSound(String name, int resourceId){
        if(name == null)
            throw new IllegalArgumentException("Parameter name can not be null.");
        MediaPlayer player = MediaPlayer.create(context.getContext(), resourceId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            player.setPlaybackParams(player.getPlaybackParams().setSpeed(0.9775f));
            player.stop();
            try {
                player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        player.setOnCompletionListener(mp -> soundCompletedCallback.onSoundCompleted(name));
        players.put(name, player);
        volumes.put(name, 1f);
    }

    public void restartSound(String sound){
        stopSound(sound);
        if(!players.containsKey(sound)) throw new IllegalArgumentException("Sound does not exist");
        MediaPlayer player = players.get(sound);
        player.start();
    }

    public void stopSound(String sound){
        if(!players.containsKey(sound)) throw new IllegalArgumentException("Sound does not exist");
        MediaPlayer player = players.get(sound);
        player.stop();
        try {
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playSound(String sound){
        if(!players.containsKey(sound)) throw new IllegalArgumentException("Sound does not exist");
        MediaPlayer player = players.get(sound);
        setVolume(sound, volumes.get(sound));
        player.start();
    }

    public void pauseSound(String sound){
        MediaPlayer player = players.get(sound);
        if(player.isPlaying()) player.pause();
    }

    public void fadeIn(String sound, long durationMs){
        if(!players.containsKey(sound)) throw new IllegalArgumentException("Sound does not exist");
        MediaPlayer player = players.get(sound);
        setVolume(sound, 0);
        fadeToVolume(sound, durationMs, 1f);
        player.start();
    }

    public void fadeOut(String sound, long durationMs){
        if(!players.containsKey(sound)) throw new IllegalArgumentException("Sound does not exist");
        if(volumes.get(sound) > 0){
            MediaPlayer player = players.get(sound);
            fadeToVolume(sound, durationMs, 0f);
            TaskUtils.runDelayed(durationMs + 10, player::pause);
        }
    }


    public void fadeToVolume(String soundName, long durationMs, float targetVolume){
        if(!players.containsKey(soundName)) throw new IllegalArgumentException("Sound does not exist");
        targetVolume = Math.min(1f, Math.max(0f, targetVolume));
        final long timeStepMs = 10;
        final long iterationCount = durationMs / timeStepMs;
        final float timeStepScaled = 1f / iterationCount;
        final float currentVolume = volumes.get(soundName);
        final float factorY =  -(currentVolume - targetVolume);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            private long i = 0;
            @Override
            public void run() {
                float time = i * timeStepScaled;
                float vol = factorY * interpolateLogistic(time) + currentVolume;
                setVolume(soundName, vol);
                i++;
                if(i >= iterationCount){
                    timer.cancel();
                    timer.purge();
                }
            }
        }, 0, timeStepMs);
    }

    public void setVolume(String soundName, float volume){
        if(!players.containsKey(soundName)) throw new IllegalArgumentException("Sound does not exist");
        players.get(soundName).setVolume(volume, volume);
        volumes.put(soundName, volume);
    }

    public float getVolume(String sound){
        if(!players.containsKey(sound)) throw new IllegalArgumentException("Sound does not exist");
        return volumes.get(sound);
    }

    public void setSoundCompletedCallback(SoundCompletedCallback soundCompletedCallback) {
        this.soundCompletedCallback = soundCompletedCallback;
    }
}
