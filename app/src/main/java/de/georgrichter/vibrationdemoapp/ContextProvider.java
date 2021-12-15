package de.georgrichter.vibrationdemoapp;

import android.content.Context;

public interface ContextProvider {
    Context getContext();

    void runOnUI(Runnable runnable);
}
