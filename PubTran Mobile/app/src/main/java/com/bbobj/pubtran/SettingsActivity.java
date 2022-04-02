package com.bbobj.pubtran;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private BottomNavigationView bnv;
    private LinearLayout logoutBtn;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private Boolean logout = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btn_terms).setOnClickListener(this);
        findViewById(R.id.btn_code).setOnClickListener(this);
        findViewById(R.id.btn_privacy).setOnClickListener(this);

        hideSystemUI();

        bnv = findViewById(R.id.bnv);
        bnv.setSelectedItemId(R.id.settings);
        bnv.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.explore) {
                    startActivity(new Intent(SettingsActivity.this, MapsActivity.class));
                    return false;
                } else if (item.getItemId() == R.id.check_in) {
                    startActivity(new Intent(SettingsActivity.this, CheckInActivity.class));
                    return false;
                } else {
                    startActivity(new Intent(SettingsActivity.this, SettingsActivity.class));
                    return false;
                }
            }
        });

        logoutBtn = findViewById(R.id.logout);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                updateUI(mAuth.getCurrentUser());
            }
        });

        mAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                logout = true;
                updateUI(mAuth.getCurrentUser());
            }
        });

        updateUI(mAuth.getCurrentUser());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void updateUI(FirebaseUser user)
    {
        if (user == null)
        {
            if (!logout) Toast.makeText(getApplicationContext(), "Session Timed Out", Toast.LENGTH_SHORT).show();
            Intent logoutIntent = new Intent(getApplicationContext(), PathwayActivity.class);
            startActivity(logoutIntent);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_code) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/JPal-Programming/StuckOverflow_SRMHackathon"));
            startActivity(browserIntent);
        } else if (v.getId() == R.id.btn_privacy) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/document/d/e/2PACX-1vSn4UzRTjeAfCr-9A3l-AGnJZKgYWFhDVy0GL_vSLC_tmcA6_QJPEJ3JsGyCKDbh8MIz-dMqm1ZK44l/pub"));
            startActivity(browserIntent);
        } else if (v.getId() == R.id.btn_terms) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/document/d/e/2PACX-1vSVkke_lwg2WNFMayVsml6HJ43JH6bM_Sa4zTA7qWLSheycZ7Fper9rOfO-EIGFXhYNkPTwIswNdT_d/pub"));
            startActivity(browserIntent);
        }
    }
}