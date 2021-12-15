package de.georgrichter.vibrationdemoapp.vibration;

import android.bluetooth.BluetoothManager;

import java.util.function.Function;

import de.georgrichter.vibrationdemoapp.BluetoothHandler;
import de.georgrichter.vibrationdemoapp.ContextProvider;
import de.georgrichter.vibrationdemoapp.packets.VibrationRealtimePacket;

public class BluetoothRealtimeVibrator extends RangedVibrationProvider{
    private final BluetoothHandler bluetoothHandler;
    private final int maxVal;
    private final Function<Float, Float> interpolationFunction;

    public BluetoothRealtimeVibrator(ContextProvider contextProvider, BluetoothHandler handler){
        this(contextProvider, handler, 255);
    }

    public BluetoothRealtimeVibrator(ContextProvider contextProvider, BluetoothHandler handler,
                                     Function<Float, Float> interpolationFunction){
        this(contextProvider, handler, 255, interpolationFunction);
    }

    public BluetoothRealtimeVibrator(ContextProvider contextProvider, BluetoothHandler handler, int maxVal){
        this(contextProvider, handler, maxVal, x -> x);
    }

    public BluetoothRealtimeVibrator(ContextProvider contextProvider, BluetoothHandler handler,
                                     int maxVal, Function<Float, Float> interpolationFunction){
        super(contextProvider);
        bluetoothHandler = handler;
        this.maxVal = maxVal;
        this.interpolationFunction = interpolationFunction;
    }

    @Override
    public void vibrate(float value) {
        value = interpolationFunction.apply(value);
        bluetoothHandler.send(new VibrationRealtimePacket((short) Math.round(value * maxVal)));
    }

    @Override
    public void stop() {
        bluetoothHandler.send(new VibrationRealtimePacket((short) 0));
    }
}
