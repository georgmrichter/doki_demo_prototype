package de.georgrichter.vibrationdemoapp.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import de.georgrichter.vibrationdemoapp.R;
import de.georgrichter.vibrationdemoapp.ui.MainActivity;
import de.georgrichter.vibrationdemoapp.ui.TestActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    public void onTestModeClicked(View view) {
        Intent intent = new Intent(this, TestActivity.class);
        startActivity(intent);
    }

    public void onSmartphoneClick(View view) {
    }

    public void onBraceletClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}