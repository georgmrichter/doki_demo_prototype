package de.georgrichter.vibrationdemoapp.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.HashMap;
import java.util.prefs.Preferences;

import de.georgrichter.vibrationdemoapp.ActionTriggeredArtPiece;
import de.georgrichter.vibrationdemoapp.BluetoothHandler;
import de.georgrichter.vibrationdemoapp.ContextProvider;
import de.georgrichter.vibrationdemoapp.R;
import de.georgrichter.vibrationdemoapp.packets.BluetoothPacket;
import de.georgrichter.vibrationdemoapp.packets.VibrationEffectPacket;
import de.georgrichter.vibrationdemoapp.vibration.BluetoothVibrator;
import de.georgrichter.vibrationdemoapp.vibration.VibrationProvider;

public class TourActivity extends AppCompatActivity implements ContextProvider {
    private ActionTriggeredArtPiece actionTriggeredArtPiece;
    private SharedState sharedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour);
        sharedState = SharedState.getInstance();
        sharedState.device = getDevice(getIntent().getExtras().getString("MAC"));
        setupBluetoothHandler(sharedState.device.getAddress());

        //loadArtPiece(artPieces.keySet().toArray(new String[0])[0]);
    }

    @Override
    public void onAttachedToWindow() {
        sharedState.updateValues(this);
        initArtPieceSpinner();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(actionTriggeredArtPiece != null && !actionTriggeredArtPiece.isDisposed())
                    actionTriggeredArtPiece.loadFromSharedState();
            }
        });
    }

    private void setSpinnerValues(Spinner spinner, String[] values){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void initArtPieceSpinner(){
        Spinner spinner = findViewById(R.id.spinner_art_piece);
        setSpinnerValues(spinner, sharedState.actionArtPieces.keySet().toArray(new String[0]));
        setOnSpinnerChange(spinner);
        spinner.setSelection(0);
    }

    private void loadArtPiece(String key){
/*
        if(!artPieces.containsKey(key)) return;
        if(actionTriggeredArtPiece != null) actionTriggeredArtPiece.dispose();
        int[] resourceIDs = artPieces.get(key);
        actionTriggeredArtPiece = new ActionTriggeredArtPiece(this,
                resourceIDs[0],
                resourceIDs[1],
                outerVib,
                innerVib);
*/

        if(!sharedState.actionArtPieces.containsKey(key)) return;
        sharedState.updateValues(this);
        actionTriggeredArtPiece = sharedState.actionArtPieces.get(key).create(this);
    }

    private void setOnSpinnerChange(Spinner spinner){
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String key = (String) spinner.getSelectedItem();
                loadArtPiece(key);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private BluetoothDevice getDevice(String mac){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            if(device.getAddress().equals(mac)) return device;
        }
        return null;
    }

    private void setupBluetoothHandler(String mac) {
        sharedState.connectBluetooth(this, mac);
        sharedState.bluetoothHandler.setConnectionErrorCallback(this::onBluetoothError);
        sharedState.bluetoothHandler.setPacketReceivedCallback(this::onPacketReceived);
    }

    private void onBluetoothError(Throwable throwable){
        throwable.printStackTrace();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("StartState", "connection");
        intent.putExtra("MAC", sharedState.device.getAddress());
        startActivity(intent);
    }

    private void onPacketReceived(BluetoothPacket packet){

    }

    public void onEnterOuterZoneClick(View view) {
        actionTriggeredArtPiece.enterOuterZone();
    }

    public void onEnterInnerZoneClick(View view) {
        actionTriggeredArtPiece.enterInnerZone();
    }

    public void onLeaveInnerZoneClick(View view) {
        actionTriggeredArtPiece.leaveInnerZone();
    }

    public void onLeaveOuterZoneClick(View view) {
        actionTriggeredArtPiece.leaveOuterZone();
    }

    public void onCancelClick(View view) {
        sharedState.disconnectBluetooth();
        actionTriggeredArtPiece.dispose();
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void runOnUI(Runnable runnable) {
        runOnUiThread(runnable);
    }

    public void onSettingsClick(View view) {
        Intent intent = new Intent(this, TourSettingsActivity.class);
        startActivity(intent);
    }

    public void onEnterSectionClick(View view) {
        sharedState.enterSection.vibrate();
    }
}