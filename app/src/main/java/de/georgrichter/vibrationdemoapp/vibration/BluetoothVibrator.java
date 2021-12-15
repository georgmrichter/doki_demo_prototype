package de.georgrichter.vibrationdemoapp.vibration;

import de.georgrichter.vibrationdemoapp.BluetoothHandler;
import de.georgrichter.vibrationdemoapp.ContextProvider;
import de.georgrichter.vibrationdemoapp.packets.VibrationEffectPacket;

public class BluetoothVibrator extends VibrationProvider{
    private final BluetoothHandler bluetoothHandler;
    private final int[] effects;

    public BluetoothVibrator(ContextProvider context, BluetoothHandler bluetoothHandler, int[] effects) {
        super(context);
        this.bluetoothHandler = bluetoothHandler;
        this.effects = effects;
    }

    @Override
    public void vibrate() {
        bluetoothHandler.send(new VibrationEffectPacket(effects));
    }

    @Override
    public void stop() {

    }
}
