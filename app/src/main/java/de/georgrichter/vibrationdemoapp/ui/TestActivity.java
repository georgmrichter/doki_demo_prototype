package de.georgrichter.vibrationdemoapp.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.georgrichter.vibrationdemoapp.ActionTriggeredArtPiece;
import de.georgrichter.vibrationdemoapp.BluetoothHandler;
import de.georgrichter.vibrationdemoapp.ContextProvider;
import de.georgrichter.vibrationdemoapp.R;
import de.georgrichter.vibrationdemoapp.audio.ZipAudioResource;
import de.georgrichter.vibrationdemoapp.packets.BluetoothPacket;
import de.georgrichter.vibrationdemoapp.packets.VibrationAudioRequestPacket;
import de.georgrichter.vibrationdemoapp.packets.VibrationEffectPacket;
import de.georgrichter.vibrationdemoapp.util.BluetoothUtils;
import de.georgrichter.vibrationdemoapp.util.CaptureActivityPortrait;
import de.georgrichter.vibrationdemoapp.vibration.BluetoothAudioVibrator;
import de.georgrichter.vibrationdemoapp.vibration.BluetoothPatternVibrator;
import de.georgrichter.vibrationdemoapp.vibration.BluetoothVibrator;
import de.georgrichter.vibrationdemoapp.vibration.PhoneVibrator;
import de.georgrichter.vibrationdemoapp.vibration.VibrationPattern;
import de.georgrichter.vibrationdemoapp.vibration.VibrationProvider;


public class TestActivity extends AppCompatActivity implements ContextProvider {
    public void onMinusClick(View view) {
        EditText number = findViewById(R.id.effectNumber);
        number.setText("" + (Integer.parseInt(number.getText().toString()) - 1));
    }

    public void onPlusClick(View view) {
        EditText number = findViewById(R.id.effectNumber);
        number.setText("" + (Integer.parseInt(number.getText().toString()) + 1));
    }

    private class SoundVibrationData{
        public String name;
        public int soundResourceId;
        public VibrationPattern vibrationPattern;
        public int vibrationPatternID;
        public ZipAudioResource audioChunks;

        public SoundVibrationData(String name, int soundResourceId, int vibrationPatternID, ZipAudioResource audioChunks) {
            this.name = name;
            this.soundResourceId = soundResourceId;
            this.vibrationPatternID = vibrationPatternID;
            this.vibrationPattern = VibrationPattern.fromVibFile(TestActivity.this, vibrationPatternID);
            this.audioChunks = audioChunks;
        }
    }

    private BluetoothManager bluetoothManager;
    private ArrayList<BluetoothDevice> devices;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;
    private BluetoothHandler bluetoothHandler;

    private ActionTriggeredArtPiece actionTriggeredArtPiece;
    private HashMap<String, SoundVibrationData> spinnerValues;

    private final VibrationPattern defaultOuterVib = new VibrationPattern(
            new long[]{500, 500, 500},
            new int[]{255, 0, 255}
    );

    private final VibrationPattern defaultInnerVib = new VibrationPattern(
            new long[]{1500},
            new int[]{255}
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        spinnerValues = new HashMap<>();

        barcodeLauncher = registerForActivityResult(new ScanContract(), this::onScanCompleted);
        devices = new ArrayList<>();
        bluetoothManager = BluetoothManager.getInstance();
        if (bluetoothManager == null) {
            Toast.makeText(this, "Bluetooth not available.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onAttachedToWindow(){
        buildUI();
        refreshArtPiece();
    }

    public void onConnectClick(View view) {
        Button button = findViewById(R.id.connectButton);
        PopupMenu popup = new PopupMenu(TestActivity.this, button);
        Menu menu = popup.getMenu();
        buildConnectionPopupMenu(menu);
        popup.getMenuInflater().inflate(R.menu.connect_popup, menu);

        popup.setOnMenuItemClickListener(item -> {
            setupBluetoothHandler(devices.get(item.getItemId()).getAddress());
            return true;
        });
        popup.show();
    }

    private void buildConnectionPopupMenu(Menu menu){
        menu.clear();
        devices.clear();
        List<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevicesList();
        for (int i = 0; i < pairedDevices.size(); i++) {
            BluetoothDevice device = pairedDevices.get(i);
            devices.add(device);
            menu.add(0, i, Menu.NONE, String.format("%s (%s)", device.getAddress(),
                    device.getName()));
        }
    }

    private void setupBluetoothHandler(String mac) {
        if(bluetoothHandler != null) bluetoothHandler.close();
        bluetoothHandler = new BluetoothHandler(this, mac);
        bluetoothHandler.connect(() -> {
            try {
                bluetoothHandler.send(new VibrationEffectPacket(1, 123, 1, 123, 1));
                refreshArtPiece();
            }catch (Exception e) {e.printStackTrace();}
        }, this::onPacketReceived, this::onBluetoothError);
    }

    private void onBluetoothError(Throwable throwable){

    }

    private void onPacketReceived(BluetoothPacket packet){
        if(packet.packetType == BluetoothPacket.PACKET_TYPE_VIBRATION_AUDIO_REQUEST){
            VibrationAudioRequestPacket varp = (VibrationAudioRequestPacket) packet;
            if(varp.requestType == VibrationAudioRequestPacket.REQUEST_NEXT_CHUNK){
                actionTriggeredArtPiece.playCurrentZoneVibration();
            }
        }
    }

    public void onScanCompleted(ScanIntentResult result){
        // scan has been canceled
        if(result.getContents() == null) return;
        String mac = result.getContents();
        if(!BluetoothAdapter.checkBluetoothAddress(mac)){
            Toast.makeText(this, "Ungültiger QR-Code", Toast.LENGTH_LONG).show();
            return;
        }
        BluetoothUtils.TryPairWithDevice(this, mac, (res, device) -> {
            runOnUiThread(() -> {
                if (res.isSuccess()) {
                    Toast.makeText(this, "Connected to " + device.getName() + " with Code " + res.toString(), Toast.LENGTH_LONG).show();
                    setupBluetoothHandler(device.getAddress());
                } else {
                    Toast.makeText(this, "Failed: " + res.toString(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    public void onScanClick(View view) {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setPrompt("Scanne den QR-Code auf dem Armband");
        options.setCameraId(0);
        options.setBeepEnabled(false);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureActivityPortrait.class);
        barcodeLauncher.launch(options);
    }

    public void onVibrateClicked(View view) {
        if (bluetoothHandler.isConnected()){
            EditText number = findViewById(R.id.effectNumber);
            bluetoothHandler.send(new VibrationEffectPacket(Byte.parseByte(number.getText().toString())));
        }
    }

    private void buildUI(){
        spinnerValues.put("Ocean", new SoundVibrationData(
                "Ocean",
                R.raw.ocean,
                R.raw.ocean_pattern,
                new ZipAudioResource(this, R.raw.ocean_chunks)
        ));

        spinnerValues.put("Forest", new SoundVibrationData(
                "Forest",
                R.raw.forest,
                R.raw.forest_pattern,
                new ZipAudioResource(this, R.raw.forest_chunks)
        ));

        spinnerValues.put("Voice", new SoundVibrationData(
                "Voice",
                R.raw.voice,
                R.raw.voice_pattern,
                new ZipAudioResource(this, R.raw.voice_chunks)
        ));

        Spinner outerSpinner = findViewById(R.id.outerSpinner);
        Spinner innerSpinner = findViewById(R.id.innerSpinner);
        String[] keys = spinnerValues.keySet().toArray(new String[0]);
        setSpinnerValues(outerSpinner, keys);
        setSpinnerValues(innerSpinner, keys);
        outerSpinner.setSelection(0);
        innerSpinner.setSelection(0);
        setOnSpinnerChange(outerSpinner);
        setOnSpinnerChange(innerSpinner);

        SwitchCompat outerSwitch = findViewById(R.id.outerVibSwitch);
        SwitchCompat innerSwitch = findViewById(R.id.innerVibSwitch);
        SwitchCompat vibSwitch = findViewById(R.id.bluetoothVibSwitch);
        SwitchCompat audioVibSwitch = findViewById(R.id.bluetoothAudioVibSwitch);
        setOnSwitchChange(outerSwitch);
        setOnSwitchChange(innerSwitch);
        setOnSwitchChange(vibSwitch);
        setOnSwitchChange(audioVibSwitch);

        SeekBar fadeDurationSeekbar = findViewById(R.id.fadeDurationSeekBar);
        setOnFadeDurationSeekBarChange(fadeDurationSeekbar);
    }

    private void refreshFadeDuration(){
        SeekBar fadeDurationSeekbar = findViewById(R.id.fadeDurationSeekBar);
        TextView fadeDurationText = findViewById(R.id.fadeDurationText);
        int newDuration = (fadeDurationSeekbar.getProgress() + 1) * 1000;
        fadeDurationText.setText("Fade duration: " + newDuration + "ms");
        actionTriggeredArtPiece.setFadeDurationMs(newDuration);
    }

    private void setOnFadeDurationSeekBarChange(SeekBar seekBar){
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                refreshFadeDuration();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void refreshArtPiece(){
        SwitchCompat bluetoothVibrationSwitch = findViewById(R.id.bluetoothVibSwitch);
        boolean bluetoothVibration = bluetoothVibrationSwitch.isChecked();
        SwitchCompat bluetoothAudioVibrationSwitch = findViewById(R.id.bluetoothAudioVibSwitch);
        boolean bluetoothAudioVibration = bluetoothAudioVibrationSwitch.isChecked();

        Spinner outerSpinner = findViewById(R.id.outerSpinner);
        Spinner innerSpinner = findViewById(R.id.innerSpinner);
        SoundVibrationData outer = spinnerValues.get(String.valueOf(outerSpinner.getSelectedItem()));
        SoundVibrationData inner = spinnerValues.get(String.valueOf(innerSpinner.getSelectedItem()));
        if(actionTriggeredArtPiece != null) actionTriggeredArtPiece.dispose();
        if(outer != null && inner != null) {
            SwitchCompat outerSwitch = findViewById(R.id.outerVibSwitch);
            SwitchCompat innerSwitch = findViewById(R.id.innerVibSwitch);
            if(bluetoothVibration && bluetoothAudioVibration){
                actionTriggeredArtPiece = new ActionTriggeredArtPiece(this, outer.soundResourceId, inner.soundResourceId,
                        new BluetoothAudioVibrator(this, bluetoothHandler, outer.audioChunks),
                        new BluetoothAudioVibrator(this, bluetoothHandler, inner.audioChunks)
                );
            } else if(bluetoothVibration){
                VibrationProvider outerProvider = outerSwitch.isChecked()
                        ? new BluetoothPatternVibrator(this, bluetoothHandler, outer.vibrationPatternID)
                        : new BluetoothVibrator(this, bluetoothHandler, new int[]{1, 123, 1});
                VibrationProvider innerProvider = innerSwitch.isChecked()
                        ? new BluetoothPatternVibrator(this, bluetoothHandler, inner.vibrationPatternID)
                        : new BluetoothVibrator(this, bluetoothHandler, new int[]{64, 64, 64});

                actionTriggeredArtPiece = new ActionTriggeredArtPiece(this, outer.soundResourceId, inner.soundResourceId,
                        outerProvider,
                        innerProvider);
            }
            else {
                VibrationPattern outerPattern = outerSwitch.isChecked() ? outer.vibrationPattern
                        : defaultOuterVib;
                VibrationPattern innerPattern = innerSwitch.isChecked() ? inner.vibrationPattern
                        : defaultInnerVib;
                actionTriggeredArtPiece = new ActionTriggeredArtPiece(this, outer.soundResourceId, inner.soundResourceId,
                        new PhoneVibrator(this, outerPattern),
                        new PhoneVibrator(this, innerPattern));
            }
        }
        refreshFadeDuration();
    }

    private void setOnSwitchChange(SwitchCompat switchCompat){
        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> refreshArtPiece());
    }

    private void setOnSpinnerChange(Spinner spinner){
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshArtPiece();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setSpinnerValues(Spinner spinner, String[] values){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public void onEnterOuterClick(View view) {
        actionTriggeredArtPiece.enterOuterZone();
    }

    public void onEnterInnerClick(View view) {
        actionTriggeredArtPiece.enterInnerZone();
    }

    public void onLeaveInnerClick(View view){
        actionTriggeredArtPiece.leaveInnerZone();
    }

    public void onLeaveOuterClick(View view){
        actionTriggeredArtPiece.leaveOuterZone();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void runOnUI(Runnable runnable) {
        runOnUI(runnable);
    }
}