package de.georgrichter.vibrationdemoapp.audio;

import android.media.MediaPlayer;
import android.os.Build;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import de.georgrichter.vibrationdemoapp.ContextProvider;

public class SoundManager {

    @FunctionalInterface
    public interface InterpolationFunction{
        float interpolate(float input);
    }

    public interface OnSoundFinishedListener{
        void onSoundFinished(String sound);
    }

    private static final InterpolationFunction INTP_FADE_LINEAR = x -> x;
    private static final InterpolationFunction INTP_FADE_LOGISTIC
            = x -> (float)(1 / (1 + Math.exp(-10 * (x - 0.5))));

    private final ContextProvider context;
    private final HashMap<String, MediaPlayer> players;
    private String currentSound;
    private OnSoundFinishedListener soundFinishedListener;

    public SoundManager(ContextProvider context){
        currentSound = null;
        this.context = context;
        soundFinishedListener = s -> {};
        players = new HashMap<>();
    }

    public void disposeAllPlayers(){
        currentSound = null;
        for (MediaPlayer player : players.values()) {
            player.stop();
            player.release();
        }
        players.clear();
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
        players.put(name, player);
        player.setOnCompletionListener(mp -> soundFinishedListener.onSoundFinished(name));
    }

    public void fadeTo(String name, long fadeDurationMs){
        if(name != null && name.equals(currentSound)) return;

        MediaPlayer currentPlayer = null;
        MediaPlayer newPlayer = null;
        if(currentSound != null){
            currentPlayer = players.get(currentSound);
            runDelayed(fadeDurationMs + 10, currentPlayer::pause);
        }
        if(name != null){
            newPlayer = players.get(name);
            newPlayer.start();
        }

        doFadeBetween(currentPlayer, newPlayer, fadeDurationMs);
        currentSound = name;
    }

    public void switchTo(String name){
        if(name != null && name.equals(currentSound)) return;
        currentSound = name;
    }

    public void stop(){
        if(currentSound == null) return;
        MediaPlayer player = players.get(currentSound);
        player.stop();
        try {
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playPause(){
        if(currentSound == null) return;
        MediaPlayer player = players.get(currentSound);
        if(player.isPlaying()) player.pause();
        else player.start();
    }

    public void pause(){
        if(currentSound == null) return;
        MediaPlayer player = players.get(currentSound);
        if(player.isPlaying()) player.pause();
    }

    private void doFadeBetween(MediaPlayer p1, MediaPlayer p2, long duration){
        final long timeStep = 10; // 10 ms
        final long iterationCount = duration / timeStep;
        final float timeStepScaled = 1f / iterationCount;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            private long i;
            @Override
            public void run() {
                float time = timeStepScaled * i;
                setFadedVolumeBetween(p1, p2, time, INTP_FADE_LOGISTIC);
                i++;
                if(i >= iterationCount){
                    timer.cancel();
                    timer.purge();
                }
            }
        }, 0, timeStep);
    }

    private void setFadedVolumeBetween(MediaPlayer p1, MediaPlayer p2,
                             float value, InterpolationFunction intpFunc){
        if(p1 != null){
            float vol = intpFunc.interpolate(1f - value);
            p1.setVolume(vol, vol);
        }
        if(p2 != null){
            float vol = intpFunc.interpolate(value);
            p2.setVolume(vol, vol);
        }
    }
    public void setSoundFinishedListener(OnSoundFinishedListener soundFinishedListener) {
        this.soundFinishedListener = soundFinishedListener;
    }

    private static void runDelayed(long delay, Runnable runnable){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, delay);
    }
}
