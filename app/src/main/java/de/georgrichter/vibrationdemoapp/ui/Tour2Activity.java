package de.georgrichter.vibrationdemoapp.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import de.georgrichter.vibrationdemoapp.ActionTriggeredArtPiece;
import de.georgrichter.vibrationdemoapp.BluetoothHandler;
import de.georgrichter.vibrationdemoapp.ContextProvider;
import de.georgrichter.vibrationdemoapp.DistanceTriggeredArtPiece;
import de.georgrichter.vibrationdemoapp.R;
import de.georgrichter.vibrationdemoapp.audio.SoundManager;
import de.georgrichter.vibrationdemoapp.packets.BluetoothPacket;

public class Tour2Activity extends AppCompatActivity implements ContextProvider {
    private static final int DISTANCE_SCALING = 10000;
    private DistanceTriggeredArtPiece distanceTriggeredArtPiece;
    private SharedState sharedState;
    private SoundManager tutorialSoundManager;
    private LinkedHashMap<String, Integer> tutorialSounds;
    private int currentTutorialIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour2);
        sharedState = SharedState.getInstance();
        sharedState.device = getDevice(getIntent().getExtras().getString("MAC"));
        setupBluetoothHandler(sharedState.device.getAddress());
    }

    @Override
    public void onAttachedToWindow() {
        sharedState.updateValues(this);
        initArtPieceSpinner();
        initSeekbars();
        initTutorial();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                sharedState.updateValues(Tour2Activity.this);
                if(distanceTriggeredArtPiece != null && !distanceTriggeredArtPiece.isDisposed())
                    distanceTriggeredArtPiece.loadFromSharedState();
            }
        });

        sharedState.addUpdateListener(() -> {
            initSeekbars();
            Spinner spinner = findViewById(R.id.spinner_art_piece);
            String key = (String) spinner.getSelectedItem();
            loadArtPiece(key);
        });
    }

    private void initTutorial(){
        tutorialSounds = new LinkedHashMap<>();
        tutorialSoundManager = new SoundManager(this);
        tutorialSounds.put("Anlegen/Einrichten", R.raw.tut_1_0_anlegen_einrichten);
        tutorialSounds.put("Verbinden", R.raw.tut_1_1_verbinden);
        tutorialSounds.put("Führung", R.raw.tut_1_2_fuehrung);
        tutorialSounds.put("Grobe Erklärung", R.raw.tut_1_3_grobe_erklaerung);
        tutorialSounds.put("Erster Kreis", R.raw.tut_2_erster_kreis);
        tutorialSounds.put("Zweiter Kreis", R.raw.tut_3_zweiter_kreis);
        tutorialSounds.put("Dritter Kreis", R.raw.tut_4_dritter_kreis);
        tutorialSounds.put("Verlassen zweiter kreis", R.raw.tut_5_verlassen_2_kreis);
        tutorialSounds.put("Verlassen erster Kreis", R.raw.tut_6_verlassen_1_kreis);
        tutorialSounds.put("Ende", R.raw.tut_7_ende);

        for (Map.Entry<String, Integer> sound : tutorialSounds.entrySet()) {
            tutorialSoundManager.addSound(sound.getKey(), sound.getValue());
        }

        tutorialSoundManager.setSoundFinishedListener(s -> {
            TextView textView = findViewById(R.id.tutorial_text);
            textView.setText(s + " (Fertig abgespielt)");
            distanceTriggeredArtPiece.playPause();
        });

        currentTutorialIndex = -1;
        switchToNewTutorial(0);
    }

    private void switchToNewTutorial(int newIndex){

        TextView textView = findViewById(R.id.tutorial_text);
        String newSound = tutorialSounds.keySet().toArray(new String[0])[newIndex];
        textView.setText(newSound);
        if(currentTutorialIndex != newIndex) tutorialSoundManager.switchTo(newSound);
        currentTutorialIndex = newIndex;
    }

    private void initSeekbars(){
        SeekBar distSeekBar = findViewById(R.id.distance_seekbar);
        SeekBar outerSeekBar = findViewById(R.id.outer_seekbar);
        SeekBar innerSeekBar = findViewById(R.id.inner_seekbar);
        View.OnTouchListener onTouchListener = (v, event) -> true;
        outerSeekBar.setOnTouchListener(onTouchListener);
        innerSeekBar.setOnTouchListener(onTouchListener);

        float seekBarMax = sharedState.outerZoneDistance +
                (sharedState.outerZoneDistance - sharedState.innerZoneDistance);
        int max = (int)(seekBarMax * DISTANCE_SCALING);
        distSeekBar.setMin(0);
        outerSeekBar.setMin(0);
        innerSeekBar.setMin(0);
        distSeekBar.setMax(max);
        outerSeekBar.setMax(max);
        innerSeekBar.setMax(max);
        distSeekBar.setProgress(max);
        outerSeekBar.setProgress((int)(sharedState.outerZoneDistance * DISTANCE_SCALING));
        innerSeekBar.setProgress((int)(sharedState.innerZoneDistance * DISTANCE_SCALING));

        distSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    distanceTriggeredArtPiece.setDistance(progress / (float)DISTANCE_SCALING);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

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
        if(!sharedState.actionArtPieces.containsKey(key)) return;
        //sharedState.updateValues(this);
        distanceTriggeredArtPiece = sharedState.distArtPieces.get(key).create(this);
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

    public void onCancelClick(View view) {
        sharedState.disconnectBluetooth();
        distanceTriggeredArtPiece.dispose();
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

    private int repeat(int value, int min, int max){
        int range = max - min;
        if(value < min) return max - ((min - value - 1) % range);
        if(value > max) return min + ((value - max - 1) % range);
        return value;
    }

    public void onEnterSectionClick(View view) {
        sharedState.enterSection.vibrate();
    }

    public void onPlayerStopClick(View view) {
        tutorialSoundManager.stop();
    }

    public void onPlayerBackClick(View view) {
        tutorialSoundManager.pause();
        switchToNewTutorial(repeat(currentTutorialIndex - 1, 0, tutorialSounds.size() - 1));
    }

    public void onPlayerPlayPauseClick(View view) {
        switchToNewTutorial(currentTutorialIndex);
        tutorialSoundManager.playPause();
        distanceTriggeredArtPiece.playPause();
    }

    public void onPlayerForwardClick(View view) {
        tutorialSoundManager.pause();
        switchToNewTutorial(repeat(currentTutorialIndex + 1, 0, tutorialSounds.size() - 1));
    }

    public void onPauseArtPieceSound(View view) {
        distanceTriggeredArtPiece.playPause();
    }
}