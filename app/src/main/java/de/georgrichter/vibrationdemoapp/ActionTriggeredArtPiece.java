package de.georgrichter.vibrationdemoapp;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import de.georgrichter.vibrationdemoapp.audio.SoundManager;
import de.georgrichter.vibrationdemoapp.ui.SharedState;
import de.georgrichter.vibrationdemoapp.vibration.VibrationProvider;

public class ActionTriggeredArtPiece implements Disposable{
    private enum Zone {
        Inner, Outer, None
    }

    private static final String AMBIENT_SOUND = "ambient";
    private static final String VOICE_SOUND = "voice";

    private final SoundManager soundManager;
    private VibrationProvider outerVibrationProvider;
    private VibrationProvider innerVibrationProvider;

    private Zone currentZone;
    private int fadeDurationMs;

    private boolean disposed;

    public ActionTriggeredArtPiece(ContextProvider context, int ambientSoundId, int voiceSoundId,
                                   VibrationProvider ambientVibrationProvider, VibrationProvider voiceVibrationProvider){
        fadeDurationMs = 5000;
        disposed = false;
        currentZone = Zone.None;
        soundManager = new SoundManager(context);
        soundManager.addSound(AMBIENT_SOUND, ambientSoundId);
        soundManager.addSound(VOICE_SOUND, voiceSoundId);
        innerVibrationProvider = voiceVibrationProvider;
        outerVibrationProvider = ambientVibrationProvider;
    }

    public void loadFromSharedState(){
        SharedState sharedState = SharedState.getInstance();
        outerVibrationProvider = sharedState.outerEnter;
        innerVibrationProvider = sharedState.innerEnter;
    }

    public int getFadeDurationMs() {
        return fadeDurationMs;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void setFadeDurationMs(int fadeDurationMs) {
        this.fadeDurationMs = fadeDurationMs;
    }

    public void dispose(){
        soundManager.disposeAllPlayers();
        disposed = true;
    }

    public void playCurrentZoneVibration(){
        if(currentZone == Zone.Inner) innerVibrationProvider.vibrate();
        else if(currentZone == Zone.Outer) outerVibrationProvider.vibrate();
    }

    public void enterOuterZone(){
        if(disposed) throw new UnsupportedOperationException("ArtPiece is disposed and can no longer be used.");
        currentZone = Zone.Outer;
        outerVibrationProvider.vibrate();
        soundManager.fadeTo(AMBIENT_SOUND, fadeDurationMs);
    }

    public void enterInnerZone(){
        if(disposed) throw new UnsupportedOperationException("ArtPiece is disposed and can no longer be used.");
        currentZone = Zone.Inner;
        innerVibrationProvider.vibrate();
        soundManager.fadeTo(VOICE_SOUND, fadeDurationMs);
    }

    public void leaveInnerZone(){
        if(disposed) throw new UnsupportedOperationException("ArtPiece is disposed and can no longer be used.");
        currentZone = Zone.Outer;
        soundManager.fadeTo(AMBIENT_SOUND, fadeDurationMs);
    }

    public void leaveOuterZone(){
        if(disposed) throw new UnsupportedOperationException("ArtPiece is disposed and can no longer be used.");
        currentZone = Zone.None;
        soundManager.fadeTo(null, fadeDurationMs);
    }
}
