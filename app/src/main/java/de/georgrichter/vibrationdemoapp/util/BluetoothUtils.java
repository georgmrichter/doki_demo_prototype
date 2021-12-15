package de.georgrichter.vibrationdemoapp.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class BluetoothUtils {
    private static final long BLUETOOTH_SCAN_TIMEOUT_MS = 10000;

    public interface TryPairCallback{
        void onCompleted(TryPairResult result, BluetoothDevice device);
    }

    public enum TryPairResult {
        ALREADY_PAIRED,
        PAIRED,
        TIMEOUT,
        NO_BLUETOOTH;

        public boolean isSuccess() {
            return this == ALREADY_PAIRED || this == PAIRED;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void TryPairWithDevice(Context context, String macAddress, TryPairCallback callback){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()){
            callback.onCompleted(TryPairResult.NO_BLUETOOTH, null);
            return;
        }

        Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bd : paired) {
            if(bd.getAddress().equals(macAddress)){
                callback.onCompleted(TryPairResult.ALREADY_PAIRED, bd);
                return;
            }
        }

        Timer timer = new Timer();
        bluetoothAdapter.startDiscovery();
        Wrapper<BroadcastReceiver> receiver = new Wrapper<>(null);
        Wrapper<BluetoothDevice> foundDevice = new Wrapper<>(null);
        receiver.setValue(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    System.out.println("Found device " + device.getName());
                    if(device.getAddress().equals(macAddress)){
                        foundDevice.setValue(device);
                        context.unregisterReceiver(receiver.getValue());
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                        context.registerReceiver(receiver.getValue(), filter);
                        device.createBond();
                    }
                }
                else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(device.getAddress().equals(foundDevice.getValue().getAddress()) &&
                            foundDevice.getValue().getBondState() == BluetoothDevice.BOND_BONDED) {
                        bluetoothAdapter.cancelDiscovery();
                        context.unregisterReceiver(receiver.getValue());
                        timer.cancel();
                        timer.purge();
                        callback.onCompleted(TryPairResult.PAIRED, device);
                    }
                }
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver.getValue(), filter);


        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                bluetoothAdapter.cancelDiscovery();
                context.unregisterReceiver(receiver.getValue());
                timer.cancel();
                timer.purge();
                callback.onCompleted(TryPairResult.TIMEOUT, null);
            }
        }, BLUETOOTH_SCAN_TIMEOUT_MS);
    }
}
