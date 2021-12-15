package de.georgrichter.vibrationdemoapp.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import de.georgrichter.vibrationdemoapp.R;
import de.georgrichter.vibrationdemoapp.ui.Tour2Activity;
import de.georgrichter.vibrationdemoapp.ui.TourActivity;

public class TourTypeActivity extends AppCompatActivity {
    private String mac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_type);
        mac = getIntent().getExtras().getString("MAC");
    }

    public void onActionTriggeredClick(View view) {
        Intent intent = new Intent(this, TourActivity.class);
        intent.putExtra("MAC", mac);
        startActivity(intent);
    }

    public void onDistanceTriggeredClick(View view) {
        Intent intent = new Intent(this, Tour2Activity.class);
        intent.putExtra("MAC", mac);
        startActivity(intent);
    }
}