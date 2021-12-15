package de.georgrichter.vibrationdemoapp;

import android.content.Context;
import android.widget.Toast;

import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.georgrichter.vibrationdemoapp.packets.BluetoothPacket;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class BluetoothHandler {
    public interface SetupCompletedCallback{
        void onSetupCompleted();
    }

    public interface PacketReceivedCallback {
        void onPacketReceived(BluetoothPacket packet);
    }

    public interface BluetoothConnectionError{
        void onConnectionError(Throwable throwable);
    }

    private final BluetoothManager bluetoothManager;

    private final String mac;

    private BluetoothSerialDevice device;
    private SimpleBluetoothDeviceInterface deviceInterface;
    private Disposable disp;
    private SetupCompletedCallback setupCompletedCallback;
    private PacketReceivedCallback packetReceivedCallback;

    private BluetoothConnectionError connectionErrorCallback;
    private volatile boolean connected;
    private ExecutorService listenerThread;
    // internal fields

    private OutputStream outputStream;
    private InputStream inputStream;
    private CompositeDisposable compositeDisposable;
    private Method requireNotClosed;
    public BluetoothHandler(Context context, String mac){
        bluetoothManager = BluetoothManager.getInstance();
        if (bluetoothManager == null) {
            Toast.makeText(context, "Bluetooth not available.", Toast.LENGTH_LONG).show();
            throw new IllegalStateException("Bluetooth not available");
        }
        connected = false;
        listenerThread = Executors.newSingleThreadExecutor();
        this.mac = mac;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getMac() {
        return mac;
    }

    public void connect(SetupCompletedCallback setupCompletedCallback,
                        PacketReceivedCallback packetReceivedCallback,
                        BluetoothConnectionError connectionErrorCallback){
        this.setupCompletedCallback = setupCompletedCallback;
        this.packetReceivedCallback = packetReceivedCallback;
        this.connectionErrorCallback = connectionErrorCallback;
        disp = bluetoothManager.openSerialDevice(mac, StandardCharsets.ISO_8859_1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnected, this::onError);
    }

    public void close(){
        connected = false;
        disp.dispose();
    }

    public void send(BluetoothPacket packet){
            compositeDisposable.add(Completable.fromAction(() -> {
                //requireNotClosed.invoke(device);
                System.out.println("Sending Packet " + packet.packetType);
                outputStream.write(packet.getData());
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {}, this::onError));
        //deviceInterface.sendMessage(packet.toStringData());
    }

    public void send(ArrayList<BluetoothPacket> packets){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            int i = 0;

            @Override
            public void run() {
                send(packets.get(i));
                i++;
            }
        }, 0, 100);
    }

    private void setInternalsViaReflection(){
        try {
            outputStream = getField(device, "outputStream");
            inputStream = getField(device, "inputStream");
            compositeDisposable = getField(deviceInterface, "compositeDisposable");
            requireNotClosed = device.getClass().getDeclaredMethod("requireNotClosed");
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private <T> T getField(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field outStrF = obj.getClass().getDeclaredField(fieldName);
        outStrF.setAccessible(true);
        return (T) outStrF.get(obj);
    }


    private void onConnected(BluetoothSerialDevice connectedDevice){
        device = connectedDevice;
        deviceInterface = device.toSimpleDeviceInterface();
        deviceInterface.setListeners(this::onMessageReceived, this::onMessageSent, this::onError);
        setInternalsViaReflection();
        connected = true;
        startListenTimer();
        setupCompletedCallback.onSetupCompleted();
    }

    private void startListenTimer(){
        listenerThread.submit(() -> {
            try {
                byte[] buffer = new byte[1000];
                while (connected) {
                    int read = inputStream.read(buffer);
                    System.out.println("Read " + read);
                    if (read >= buffer.length)
                        throw new IllegalStateException("Recieved packet was too large");
                    byte[] data = Arrays.copyOfRange(buffer, 0, read);
                    ArrayList<BluetoothPacket> packets = BluetoothPacket.fromData(data);
                    for (BluetoothPacket packet : packets)
                        packetReceivedCallback.onPacketReceived(packet);
                }
             } catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    public void setPacketReceivedCallback(PacketReceivedCallback packetReceivedCallback) {
        this.packetReceivedCallback = packetReceivedCallback;
    }

    public void setConnectionErrorCallback(BluetoothConnectionError connectionErrorCallback) {
        this.connectionErrorCallback = connectionErrorCallback;
    }

    private void onError(Throwable throwable){
        connectionErrorCallback.onConnectionError(throwable);
    }

    private void onMessageSent(String message) {
    }

    private void onMessageReceived(String message) {
    }
}
