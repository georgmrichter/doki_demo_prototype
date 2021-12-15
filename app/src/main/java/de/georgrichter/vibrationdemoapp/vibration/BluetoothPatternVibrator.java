package de.georgrichter.vibrationdemoapp.vibration;

import de.georgrichter.vibrationdemoapp.BluetoothHandler;
import de.georgrichter.vibrationdemoapp.ContextProvider;
import de.georgrichter.vibrationdemoapp.packets.VibrationPatternPacket;

public class BluetoothPatternVibrator extends VibrationProvider{
    private final int resourceId;
    private final ContextProvider contextProvider;
    private final BluetoothHandler bluetoothHandler;
    private boolean isPaused;

    public BluetoothPatternVibrator(ContextProvider contextProvider, BluetoothHandler bluetoothHandler, int resourceID) {
        super(contextProvider);
        this.contextProvider = contextProvider;
        this.bluetoothHandler = bluetoothHandler;
        this.resourceId = resourceID;
        isPaused = false;
    }

    @Override
    public void vibrate() {
        if(isPaused){
            bluetoothHandler.send(VibrationPatternPacket.getResumePacket());
        }
        else {
            bluetoothHandler.send(VibrationPatternPacket.getPlayPacket(contextProvider, resourceId));
        }
        isPaused = true;
    }

    @Override
    public void stop() {
        bluetoothHandler.send(VibrationPatternPacket.getStopPacket());
        isPaused = true;
    }
}
