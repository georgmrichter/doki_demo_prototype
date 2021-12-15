package de.georgrichter.vibrationdemoapp.vibration;


import de.georgrichter.vibrationdemoapp.BluetoothHandler;
import de.georgrichter.vibrationdemoapp.ContextProvider;
import de.georgrichter.vibrationdemoapp.audio.ZipAudioResource;
import de.georgrichter.vibrationdemoapp.packets.VibrationAudioPacket;
import de.georgrichter.vibrationdemoapp.packets.VibrationAudioRequestPacket;

public class BluetoothAudioVibrator extends VibrationProvider{
    private final ZipAudioResource audioResource;
    private final BluetoothHandler bluetoothHandler;

    public BluetoothAudioVibrator(ContextProvider context,
                                  BluetoothHandler bluetoothHandler,
                                  ZipAudioResource audioResource) {
        super(context);
        this.audioResource = audioResource;
        this.bluetoothHandler = bluetoothHandler;
    }

    @Override
    public void vibrate() {
        bluetoothHandler.send(new VibrationAudioPacket(context.getContext(), audioResource.getNextChunk(),
                audioResource.getSoundID(), audioResource.getCurrentChunkId()));
    }

    @Override
    public void stop() {
        bluetoothHandler.send(new VibrationAudioRequestPacket(VibrationAudioRequestPacket.REQUEST_STOP_PLAYBACK));
    }
}
