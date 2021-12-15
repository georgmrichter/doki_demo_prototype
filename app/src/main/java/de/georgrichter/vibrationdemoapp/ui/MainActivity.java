package de.georgrichter.vibrationdemoapp.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.List;

import de.georgrichter.vibrationdemoapp.BluetoothHandler;
import de.georgrichter.vibrationdemoapp.R;
import de.georgrichter.vibrationdemoapp.databinding.ActivityMainBinding;
import de.georgrichter.vibrationdemoapp.packets.BluetoothPacket;
import de.georgrichter.vibrationdemoapp.packets.VibrationEffectPacket;
import de.georgrichter.vibrationdemoapp.ui.connect.ConnectFragment;
import de.georgrichter.vibrationdemoapp.ui.enablebluetooth.EnableBluetoothFragment;
import de.georgrichter.vibrationdemoapp.ui.scanqr.ScanQRFragment;
import de.georgrichter.vibrationdemoapp.util.BluetoothUtils;
import de.georgrichter.vibrationdemoapp.util.CaptureActivityPortrait;

public class MainActivity extends AppCompatActivity{
    private ActivityMainBinding binding;

    private BluetoothAdapter bluetoothAdapter;
    private final ActivityResultLauncher<Intent> enableBTLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::onBTEnableRequestResult
    );
    private final ActivityResultLauncher<ScanOptions> scanLauncher = registerForActivityResult(
            new ScanContract(),
            this::onScanCompleted
    );

    private boolean successfullyScanned;
    private String mac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        for (int i = 0; i < navView.getMenu().size(); i++) {
            navView.getMenu().getItem(i).setEnabled(false);
        }
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_enable_bluetooth, R.id.navigation_scan_qr, R.id.navigation_connect)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        //NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        init();
    }

    private void init(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        successfullyScanned = false;

        if(getIntent().getExtras() == null) return;
        String startState = getIntent().getExtras().getString("StartState");
        if(startState != null && startState.equals("connection")){
            Toast.makeText(this, "Konnte keine Verbindung herstellen", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAttachedToWindow() {
        TextView text = findViewById(R.id.bluetooth_enable_text);
        Button button = findViewById(R.id.button_enable_bluetooth);
        if(bluetoothAdapter.isEnabled()){
            text.setText("Bluetooth ist bereits eingeschaltet.");
            button.setText("Weiter");
        }
        else {
            text.setText("Bluetooth ist ausgeschaltet.");
            button.setText("Bluetooth einschalten");
        }
    }

    private void onBTEnableRequestResult(ActivityResult result){
        TextView text = findViewById(R.id.bluetooth_enable_text);
        Button button = findViewById(R.id.button_enable_bluetooth);
        if(bluetoothAdapter.isEnabled()) {
            text.setText("Bluetooth wurde eingeschaltet.");
            button.setText("Weiter");
        }
        else {
            // TODO: warning message for user that enabling bluetooth is required
        }
    }

    public void OnBluetoothEnableClick(View view) {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBTLauncher.launch(enableBtIntent);
        }
        else {
            binding.navView.getMenu().getItem(1).setEnabled(true);
            binding.navView.setSelectedItemId(R.id.navigation_scan_qr);
            binding.navView.getMenu().getItem(1).setEnabled(false);
        }
    }

    private void onScanCompleted(ScanIntentResult result){
        TextView text = findViewById(R.id.scan_qr_text);

        if(result.getContents() == null){
            text.setText("Sie haben den QR Code Scan abgebrochen. Versuchen Sie es erneut.");
            Toast.makeText(this, "Scan wurde abgebrochen\nVersuchen Sie es erneut", Toast.LENGTH_LONG).show();
            return;
        }
        String mac = result.getContents();
        if(!BluetoothAdapter.checkBluetoothAddress(mac)){
            text.setText("Der QR Code war ungültig. Versuchen Sie es erneut.");
            Toast.makeText(this, "Ungültiger QR Code\nVersuchen Sie es erneut", Toast.LENGTH_LONG).show();
            return;
        }

        text.setText("Armband " + mac + " erkannt.");
        Button button = findViewById(R.id.button_scan_qr);
        button.setText("Weiter");
        successfullyScanned = true;
        this.mac = mac;
    }

    public void onScanQRClick(View view) {
        if(!successfullyScanned) {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
            options.setPrompt("Scannen Sie den QR Code auf dem Armband");
            options.setCameraId(0);
            options.setBeepEnabled(false);
            options.setBarcodeImageEnabled(false);
            options.setOrientationLocked(true);
            options.setCaptureActivity(CaptureActivityPortrait.class);
            scanLauncher.launch(options);
        }
        else {
            binding.navView.getMenu().getItem(2).setEnabled(true);
            binding.navView.setSelectedItemId(R.id.navigation_connect);
            binding.navView.getMenu().getItem(2).setEnabled(false);
            connectToDevice();
        }
    }

    public void connectToDevice(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                BluetoothUtils.TryPairWithDevice(this, mac, (res, device) -> {
                    runOnUiThread(() -> {
                        if (res.isSuccess()) {
                            Intent intent = new Intent(this, TourTypeActivity.class);
                            intent.putExtra("MAC", device.getAddress());
                            startActivity(intent);
                        } else {
                            //TODO: show error
                            Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
                        }
                    });
                });
            } else {
                Toast.makeText(this,
                        "Standortberechtigung ist erforderlich zum suchen von Bluetoothgeräten.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onTestClick(View view) {
        TextView text = findViewById(R.id.scan_qr_text);
        String mac = "B8:F0:09:CC:5A:3E";
        text.setText("Armband " + mac + " erkannt.");
        Button button = findViewById(R.id.button_scan_qr);
        button.setText("Weiter");
        successfullyScanned = true;
        this.mac = mac;
    }
}