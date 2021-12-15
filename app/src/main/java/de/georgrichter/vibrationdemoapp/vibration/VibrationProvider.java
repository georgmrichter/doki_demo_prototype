package de.georgrichter.vibrationdemoapp.vibration;

import de.georgrichter.vibrationdemoapp.ContextProvider;

public abstract class VibrationProvider {

    protected ContextProvider context;

    public VibrationProvider(ContextProvider context) {
        this.context = context;
    }

    public abstract void vibrate();

    public abstract void stop();
}
