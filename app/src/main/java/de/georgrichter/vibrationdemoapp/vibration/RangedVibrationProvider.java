package de.georgrichter.vibrationdemoapp.vibration;

import de.georgrichter.vibrationdemoapp.ContextProvider;

public abstract class RangedVibrationProvider extends VibrationProvider {
    public RangedVibrationProvider(ContextProvider context) {
        super(context);
    }

    public abstract void vibrate(float value);

    public void vibrate(){
        vibrate(1f);
    }
}
