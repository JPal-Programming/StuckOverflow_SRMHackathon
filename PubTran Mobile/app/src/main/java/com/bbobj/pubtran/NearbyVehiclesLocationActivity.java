package com.bbobj.pubtran;

import static android.view.animation.Animation.RELATIVE_TO_SELF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.skyfishjy.library.RippleBackground;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NearbyVehiclesLocationActivity extends AppCompatActivity {

    private static final String TAG = "TAG";
    private ProgressBar progressBarView;
    private int myProgress;
    private CountDownTimer countDownTimer;
    private int endTime = 60;
    private int progress;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ArrayList<Map<String, Object>> data = new ArrayList<>();
    private int vehiclePos;

    private Map<String, Object> stops;
    private ArrayList<String> stopNames;
    private int currentStop;
    private String currentStopName;
    private String vehicleName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_vehicles_location);

        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.content);
        ImageView imageView=(ImageView)findViewById(R.id.centerImage);
        rippleBackground.startRippleAnimation();

        findViewById(R.id.info_fragment).findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_down);
                ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.info_fragment_container);
                hiddenPanel.startAnimation(bottomUp);
                hiddenPanel.setVisibility(View.GONE);
            }
        });

        Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_up);
        ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.info_fragment_container);
        hiddenPanel.startAnimation(bottomUp);
        hiddenPanel.setVisibility(View.VISIBLE);

        getInfo();
    }

    private void getInfo() {
        db.collection("routes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                data.add(document.getData());
                            }
                            sendLocationUpdates();
                        } else {
                            Toast.makeText(getApplicationContext(), "Error reaching database.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void sendLocationUpdates() {
        vehiclePos = getIntent().getIntExtra("position", -1);
        Map<String, Object> vehicle = data.get(vehiclePos);

        vehicleName = (String) vehicle.get("name");

        updateData();
    }

    private void updateData() {
        new CountDownTimer(10000, 1000) {

            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                getUpdates();
            }

        }.start();
    }

    private void getUpdates() {
        getDeviceLocation(new GPSCallback() {
            @Override
            public void onLocation(Location location) {
                Map<String, Object> updates = new HashMap<>();

                stops = (Map<String, Object>) data.get(vehiclePos).get("stops");
                stopNames = new ArrayList<>(stops.keySet());

                updates.put("currentPos", new GeoPoint(location.getLatitude(), location.getLongitude()));

                currentStop = findCurrentStopIndex(new GeoPoint(location.getLatitude(), location.getLongitude()));
                currentStopName = stopNames.get(currentStop);
                updates.put("currentStop", currentStopName);

                GeoPoint nextStopPos = (GeoPoint) ((Map<String, Object>) stops.get(stopNames.get(currentStop + 1))).get("pos");

                Location targetLocation = new Location("");
                targetLocation.setLatitude(nextStopPos.getLatitude());
                targetLocation.setLongitude(nextStopPos.getLongitude());

                float distanceInMeters =  targetLocation.distanceTo(location);

                if (distanceInMeters <= 200) {
                    openConfirmDialog(currentStopName);
                }

                GeoPoint prevGeoPoint = (GeoPoint) data.get(vehiclePos).get("currentPos");
                Location prevLocation = new Location("");
                prevLocation.setLatitude(prevGeoPoint.getLatitude());
                prevLocation.setLongitude(prevGeoPoint.getLongitude());

                double prevSpeed = (double) data.get(vehiclePos).get("speed");

                Timestamp lastUpdated = (Timestamp) data.get(vehiclePos).get("lastUpdated");

                Date currentTime = new Date(System.currentTimeMillis());

                long differenceInMilliSeconds
                        = Math.abs(currentTime.getTime() - lastUpdated.toDate().getTime());

                double currentSpeed = (prevLocation.distanceTo(location) * 0.00062137) / (differenceInMilliSeconds / (1000 * 60 * 60));

                updates.put("speed", (currentSpeed + prevSpeed)/2);

                updates.put("lastUpdated", FieldValue.serverTimestamp());

                updateDB(updates);
            }
        });
    }

    private void updateDB(Map<String, Object> updates) {
        db.collection("routes")
                .document(vehicleName)
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), "Server Connection Failed. Please make sure you have an active internet connection or have cellular data enabled.", Toast.LENGTH_LONG).show();
                    }
                });

        updateData();
    }

    private int findCurrentStopIndex(GeoPoint currentPos) {
        ArrayList<GeoPoint> stopPos = new ArrayList<>();

        for (Map.Entry<String, Object> stop : stops.entrySet()) {
            Map<String, Object> stopInfo = (Map<String, Object>) stop.getValue();
            stopPos.add((GeoPoint) stopInfo.get("pos"));
        }

        double minDis = distBetweenCoords(stopPos.get(0), currentPos);
        int minInd = 0;

        for (int j = 1; j < stopPos.size(); j++) {
            if (distBetweenCoords(stopPos.get(j), currentPos) < minDis) {
                minDis = distBetweenCoords(stopPos.get(j), currentPos);
                minInd = j;
            }
        }

        if (minInd == 0) {
            return 0;
        } else if (distBetweenCoords(stopPos.get(minInd - 1), currentPos) < distBetweenCoords(stopPos.get(minInd + 1), currentPos)) {
            return minInd - 1;
        }
        return minInd;
    }

    private double distBetweenCoords(GeoPoint geoPoint1, GeoPoint geoPoint2) {
        double theta = geoPoint1.getLongitude() - geoPoint2.getLongitude();
        double dist = Math.sin(deg2rad(geoPoint1.getLatitude()))
                * Math.sin(deg2rad(geoPoint2.getLatitude()))
                + Math.cos(deg2rad(geoPoint1.getLatitude()))
                * Math.cos(deg2rad(geoPoint2.getLatitude()))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    interface GPSCallback{
        void onLocation(Location location);
    }

    private void getDeviceLocation(GPSCallback callback) {
        Log.d(TAG, "getDeviceLocation: getting the device's current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            final Task location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: found location!");
                    Location currentLocation = (Location) task.getResult();
                    callback.onLocation(currentLocation);
                } else {
                    Log.d(TAG, "onComplete: current location is null");
                    Toast.makeText(getApplicationContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(), "Cannot get Device Location. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openConfirmDialog(String stopName) {
        TextView tv_stopText = findViewById(R.id.confirm_fragment).findViewById(R.id.tv_stopName);
        tv_stopText.setText(stopName);

        findViewById(R.id.confirm_fragment).findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_down);
                ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.confirm_fragment_container);
                hiddenPanel.startAnimation(bottomUp);
                hiddenPanel.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.confirm_fragment).findViewById(R.id.btn_deny).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_down);
                ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.confirm_fragment_container);
                hiddenPanel.startAnimation(bottomUp);
                hiddenPanel.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.confirm_fragment).findViewById(R.id.btn_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_down);
                ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.confirm_fragment_container);
                hiddenPanel.startAnimation(bottomUp);
                hiddenPanel.setVisibility(View.GONE);
                updateCurrentStop();
            }
        });

        Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_up);
        ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.confirm_fragment_container);
        hiddenPanel.startAnimation(bottomUp);
        hiddenPanel.setVisibility(View.VISIBLE);

        progressBarView = (ProgressBar) findViewById(R.id.view_progress_bar);

        /*Animation*/
        RotateAnimation makeVertical = new RotateAnimation(0, -90, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f);
        makeVertical.setFillAfter(true);
        progressBarView.startAnimation(makeVertical);
        progressBarView.setSecondaryProgress(endTime);
        progressBarView.setProgress(0);

        fn_countdown();
    }

    private void updateCurrentStop() {
        getDeviceLocation(new GPSCallback() {
            @Override
            public void onLocation(Location location) {
                String stopUpdateName = (currentStop == findCurrentStopIndex(new GeoPoint(location.getLatitude(), location.getLongitude()))) ? stopNames.get(currentStop-1) : currentStopName;
                db.collection("routes").document(vehicleName).update(stopUpdateName + ".dep", FieldValue.serverTimestamp());
            }
        });
    }

    private void fn_countdown() {
        myProgress = 0;

        try {
            countDownTimer.cancel();
        } catch (Exception e) {}

        progress = 1;
        endTime = 60; // up to finish time

        countDownTimer = new CountDownTimer(endTime * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                setProgress(progress, endTime);
                progress = progress + 1;
            }

            @Override
            public void onFinish() {
                setProgress(progress, endTime);
            }
        };
        countDownTimer.start();
    }

    public void setProgress(int startTime, int endTime) {
        progressBarView.setMax(endTime);
        progressBarView.setSecondaryProgress(endTime);
        progressBarView.setProgress(startTime);
    }
}