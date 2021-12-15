package de.georgrichter.vibrationdemoapp;

import de.georgrichter.vibrationdemoapp.audio.MultiSoundManager;
import de.georgrichter.vibrationdemoapp.ui.SharedState;
import de.georgrichter.vibrationdemoapp.vibration.RangedVibrationProvider;
import de.georgrichter.vibrationdemoapp.vibration.VibrationProvider;

public class DistanceTriggeredArtPiece implements Disposable{
    private static final String SOUND_VOICE = "voice";
    private static final String SOUND_AMBIENT = "ambient";

    // distance to the art piece, 0 would be at the position of the art piece
    private float distance;
    private float prevDistance;
    private float prevprevDistance;

    private float innerZoneDistance;
    private float outerZoneDistance;
    private float tooCloseDistance;
    private float vibrationRampRange;
    private float closestDistance;

    private RangedVibrationProvider outerEnterRamp;
    private RangedVibrationProvider innerEnterRamp;
    private RangedVibrationProvider outerLeaveRamp;
    private RangedVibrationProvider innerLeaveRamp;
    private RangedVibrationProvider tooCloseRamp;
    private VibrationProvider outerEnter;
    private VibrationProvider innerEnter;
    private VibrationProvider outerLeave;
    private VibrationProvider innerLeave;
    private VibrationProvider voiceEnd;

    private boolean inOuterEnterRamp;
    private boolean inInnerEnterRamp;
    private boolean inOuterLeaveRamp;
    private boolean inInnerLeaveRamp;
    private boolean inTooCloseRamp;

    // fade duration between audio
    private int fadeDurationMs;
    private final MultiSoundManager soundManager;
    private boolean paused;
    private float ambientVol;

    private boolean isDisposed;

    public DistanceTriggeredArtPiece(ContextProvider contextProvider,
                                      int ambientSoundID,
                                      int voiceSoundID, boolean loadFromSharedState){
        soundManager = new MultiSoundManager(contextProvider, this::onSoundCompleted);
        soundManager.addSound(SOUND_AMBIENT, ambientSoundID);
        soundManager.addSound(SOUND_VOICE, voiceSoundID);
        if(loadFromSharedState) loadFromSharedState();
        distance = Float.MAX_VALUE;
        prevDistance = Float.MAX_VALUE;
        prevprevDistance = Float.MAX_VALUE;
        closestDistance = Float.MAX_VALUE;
        inOuterEnterRamp = false;
        inInnerEnterRamp = false;
        inOuterLeaveRamp = false;
        inInnerLeaveRamp = false;
        inTooCloseRamp = false;
        fadeDurationMs = 2000;
        isDisposed = false;
        paused = false;
        ambientVol = 1f;
    }

    public void loadFromSharedState(){
        SharedState sharedState = SharedState.getInstance();
        innerZoneDistance = sharedState.innerZoneDistance;
        outerZoneDistance = sharedState.outerZoneDistance;
        tooCloseDistance = sharedState.tooCloseDistance;
        vibrationRampRange = sharedState.vibrationRampRange;
        outerEnterRamp = sharedState.outerEnterRamp;
        innerEnterRamp = sharedState.innerEnterRamp;
        outerLeaveRamp = sharedState.outerLeaveRamp;
        innerLeaveRamp = sharedState.innerLeaveRamp;
        tooCloseRamp = sharedState.tooCloseRamp;
        outerEnter = sharedState.outerEnter;
        innerEnter = sharedState.innerEnter;
        outerLeave = sharedState.outerLeave;
        innerLeave = sharedState.innerLeave;
        voiceEnd = sharedState.voiceEnd;
    }

    public void playPause(){
        if(paused){
            if(inInnerZone(distance) || inOuterZone(distance)){
                if(inOuterZone(distance)) soundManager.setVolume(SOUND_AMBIENT, 1f);
                else soundManager.setVolume(SOUND_AMBIENT, 0.2f);
                soundManager.playSound(SOUND_AMBIENT);
            }
            if(inInnerZone(distance)) {
                soundManager.setVolume(SOUND_VOICE, 1f);
                soundManager.playSound(SOUND_VOICE);
            }
        }
        else {
            if(inInnerZone(distance) || inOuterZone(distance)) {
                soundManager.pauseSound(SOUND_AMBIENT);
            }
            if(inInnerZone(distance)) {
                soundManager.pauseSound(SOUND_VOICE);
            }
        }
        paused = !paused;
    }

    public void dispose(){
        soundManager.disposeAllPlayers();
        isDisposed = true;
    }

    private float getRampValue(float distance, float begin, float end){
        return (distance - begin) / (end - begin);
    }

    private boolean isValidRamp(float rampValue){
        return rampValue <= 0.95f && rampValue >= 0.05f;
    }

    private boolean inInnerZone(float distance) {
        return distance >= 0f && distance < innerZoneDistance;
    }

    private boolean inOuterZone(float distance){
        return distance >= innerZoneDistance && distance < outerZoneDistance;
    }

    private boolean outsideOuter(float distance){
        return distance >= outerZoneDistance;
    }

    private boolean movingAway(){
        return prevDistance < distance && prevprevDistance < prevDistance;
    }

    private void checkDistance(){
        if(inInnerZone(distance)){
            checkInnerZone();
        }
        else if(inOuterZone(distance)){
            checkOuterZone();
        }
        else if(outsideOuter(distance)){
            checkOutside();
        }
    }

    private void checkOutside(){
        float enterOuterRampValue = getRampValue(distance,
                outerZoneDistance, outerZoneDistance + vibrationRampRange);

        if(inOuterEnterRamp){
            if (isValidRamp(enterOuterRampValue)) {
                outerEnterRamp.vibrate(1f - enterOuterRampValue);
            }
            else {
                outerEnterRamp.vibrate(0);
                inOuterEnterRamp = false;
            }
        }
        if(movingAway()) {
            if (inOuterZone(prevDistance)) {
                outerLeave.vibrate();
                if(!paused) soundManager.fadeOut(SOUND_AMBIENT, fadeDurationMs);
                soundManager.stopSound(SOUND_VOICE);
            }
        }
        else {
            if (!inOuterEnterRamp && isValidRamp(enterOuterRampValue)) {
                outerEnterRamp.vibrate(1f - enterOuterRampValue);
                inOuterEnterRamp = true;
            }
        }
    }

    private void checkOuterZone() {
        float innerEnterRampValue = getRampValue(distance,
                innerZoneDistance, innerZoneDistance + vibrationRampRange);
        float outerLeaveRampValue = getRampValue(distance,
                outerZoneDistance - vibrationRampRange, outerZoneDistance);

        if(inInnerEnterRamp){
            if(isValidRamp(innerEnterRampValue)) {
                innerEnterRamp.vibrate(1f - innerEnterRampValue);
            } else{
                innerEnterRamp.vibrate(0);
                inInnerEnterRamp = false;
            }
        }
        if(inOuterLeaveRamp){
            if(isValidRamp(outerLeaveRampValue)) {
                outerLeaveRamp.vibrate(outerLeaveRampValue);
            } else {
                outerLeaveRamp.vibrate(0);
                inOuterLeaveRamp = false;
            }
        }
        if (movingAway()) {
            // We are moving away
            if(inInnerZone(prevDistance)){
                innerLeave.vibrate();
                if(!paused) soundManager.fadeOut(SOUND_VOICE, fadeDurationMs);
                if(!paused) soundManager.fadeToVolume(SOUND_AMBIENT, fadeDurationMs, 1f);
            }

            if (!inOuterLeaveRamp && isValidRamp(outerLeaveRampValue)) {
                outerLeaveRamp.vibrate(outerLeaveRampValue);
                inOuterLeaveRamp = true;
            }
        }
        else {
            if(outsideOuter(prevDistance)){
                outerEnter.vibrate();
                if(!paused) soundManager.fadeIn(SOUND_AMBIENT, fadeDurationMs);
            }

            if (!inInnerEnterRamp && isValidRamp(innerEnterRampValue)) {
                innerEnterRamp.vibrate(1f - innerEnterRampValue);
                inInnerEnterRamp = true;
            }
        }
    }

    private void checkInnerZone() {
        float innerLeaveRampValue = getRampValue(distance,
                innerZoneDistance - vibrationRampRange, innerZoneDistance);
        float tooCloseRampValue = getRampValue(closestDistance == Float.MAX_VALUE ? distance : closestDistance,
                0, tooCloseDistance);
        float tooCloseCheckRampValue = getRampValue(distance, 0, tooCloseDistance);

        if(inInnerLeaveRamp){
            if(isValidRamp(innerLeaveRampValue)) {
                innerLeaveRamp.vibrate(innerLeaveRampValue);
            } else {
                innerLeaveRamp.vibrate(0);
                inInnerLeaveRamp = false;
            }
        }
        if(inTooCloseRamp){
            if(distance < closestDistance) closestDistance = distance;
            if (tooCloseCheckRampValue <= 0.95f) {
                tooCloseRamp.vibrate(1f - tooCloseRampValue);
            } else {
                tooCloseRamp.vibrate(0);
                tooCloseRamp.stop();
                inTooCloseRamp = false;
                closestDistance = Float.MAX_VALUE;
            }
        }
        if(movingAway()){
            // We are moving away
            if (!inInnerLeaveRamp && isValidRamp(innerLeaveRampValue)) {
                innerLeaveRamp.vibrate(innerLeaveRampValue);
                inInnerLeaveRamp = true;
            }
        }
        else{
            if(inOuterZone(prevDistance)){
                innerEnter.vibrate();
                if(!paused) soundManager.fadeToVolume(SOUND_AMBIENT, fadeDurationMs, 0.2f);
                if(!paused) soundManager.fadeIn(SOUND_VOICE, fadeDurationMs);
            }
            if (!inTooCloseRamp)
                if (tooCloseCheckRampValue <= 0.95f) {
                    tooCloseRamp.vibrate(1f - tooCloseRampValue);
                    inTooCloseRamp = true;
                }
        }
    }

    private void onSoundCompleted(String sound){
        if(sound.equals(SOUND_AMBIENT)){
            soundManager.restartSound(SOUND_AMBIENT);
        }
        else if(sound.equals(SOUND_VOICE)){
            voiceEnd.vibrate();
        }
    }

    public void setDistance(float distance) {
        prevprevDistance = prevDistance;
        prevDistance = this.distance;
        this.distance = distance;
        checkDistance();
    }

    public float getDistance() {
        return distance;
    }

    public float getInnerZoneDistance() {
        return innerZoneDistance;
    }

    public void setInnerZoneDistance(float innerZoneDistance) {
        this.innerZoneDistance = innerZoneDistance;
    }

    public float getOuterZoneDistance() {
        return outerZoneDistance;
    }

    public void setOuterZoneDistance(float outerZoneDistance) {
        this.outerZoneDistance = outerZoneDistance;
    }

    public float getVibrationRampRange() {
        return vibrationRampRange;
    }

    public void setVibrationRampRange(float vibrationRampRange) {
        this.vibrationRampRange = vibrationRampRange;
    }

    public float getTooCloseDistance() {
        return tooCloseDistance;
    }

    public void setTooCloseDistance(float tooCloseDistance) {
        this.tooCloseDistance = tooCloseDistance;
    }

    public RangedVibrationProvider getOuterEnterRamp() {
        return outerEnterRamp;
    }

    public void setOuterEnterRamp(RangedVibrationProvider outerEnterRamp) {
        this.outerEnterRamp = outerEnterRamp;
    }

    public RangedVibrationProvider getInnerEnterRamp() {
        return innerEnterRamp;
    }

    public void setInnerEnterRamp(RangedVibrationProvider innerEnterRamp) {
        this.innerEnterRamp = innerEnterRamp;
    }

    public RangedVibrationProvider getOuterLeaveRamp() {
        return outerLeaveRamp;
    }

    public void setOuterLeaveRamp(RangedVibrationProvider outerLeaveRamp) {
        this.outerLeaveRamp = outerLeaveRamp;
    }

    public void setInnerLeaveRamp(RangedVibrationProvider innerLeaveRamp) {
        this.innerLeaveRamp = innerLeaveRamp;
    }

    public RangedVibrationProvider getTooCloseRamp() {
        return tooCloseRamp;
    }

    public void setTooCloseRamp(RangedVibrationProvider tooCloseRamp) {
        this.tooCloseRamp = tooCloseRamp;
    }

    public VibrationProvider getVoiceEnd() {
        return voiceEnd;
    }

    public void setVoiceEnd(VibrationProvider voiceEnd) {
        this.voiceEnd = voiceEnd;
    }

    public int getFadeDurationMs() {
        return fadeDurationMs;
    }

    public void setFadeDurationMs(int fadeDurationMs) {
        this.fadeDurationMs = fadeDurationMs;
    }

    public VibrationProvider getOuterEnter() {
        return outerEnter;
    }

    public void setOuterEnter(VibrationProvider outerEnter) {
        this.outerEnter = outerEnter;
    }

    public VibrationProvider getInnerEnter() {
        return innerEnter;
    }

    public void setInnerEnter(VibrationProvider innerEnter) {
        this.innerEnter = innerEnter;
    }

    public boolean isDisposed() {
        return isDisposed;
    }
}
