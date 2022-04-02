package com.bbobj.pubtran;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ProgressBar;
import android.widget.TextView;

import static android.view.animation.Animation.RELATIVE_TO_SELF;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CheckInActivity extends AppCompatActivity {

    private BottomNavigationView bnv;
    private CardView cv_nearbyVehicles;
    private CardView cv_createVehicle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);

        cv_nearbyVehicles = findViewById(R.id.cv_nearby);
        cv_createVehicle = findViewById(R.id.cv_create);

        cv_nearbyVehicles.setOnClickListener((v) -> startActivity(new Intent(getApplicationContext(), NearbyVehiclesActivity.class)));
        cv_createVehicle.setOnClickListener((v) -> startActivity(new Intent(getApplicationContext(), NewVehicleActivity.class)));

        bnv = findViewById(R.id.bnv);
        bnv.setSelectedItemId(R.id.check_in);
        bnv.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.explore) {
                    startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                    return false;
                } else if (item.getItemId() == R.id.check_in) {
                    startActivity(new Intent(getApplicationContext(), CheckInActivity.class));
                    return false;
                } else {
                    startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                    return false;
                }
            }
        });
    }
}