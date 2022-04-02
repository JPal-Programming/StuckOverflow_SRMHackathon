package com.bbobj.pubtran;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NewVehicleActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener, View.OnClickListener {

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private LocationManager mLocationManager;

    private Map<String, Object> vehicle = new HashMap<>();
    private Map<String, Object> stops;
    private ArrayList<String> stopNames;
    private ArrayList<String> stopNamesAll;
    private int currentStop;
    private String currentStopName;
    private String vehicleName;

    private static final String TAG = "NewVehicleActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private ArrayList<LatLng> mPoints = new ArrayList<>();
    private ArrayList<ArrayList<String>> mVehicleInfo = new ArrayList<>();
    private ArrayList<ArrayList<String>> mNames = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> mDistances = new ArrayList<>();
    private ArrayList<ArrayList<ArrayList<Date>>> mArrDep = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> mTimes = new ArrayList<>();

    private GoogleMap mMap;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private StopNameAdapter stopNameAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_vehicle);

        findViewById(R.id.add_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_up);
                ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.stops_prompt_fragment_container);
                hiddenPanel.startAnimation(bottomUp);
                hiddenPanel.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.stops_prompt_fragment_container).findViewById(R.id.btn_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation bottomDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_down);
                ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.stops_prompt_fragment_container);
                hiddenPanel.startAnimation(bottomDown);
                hiddenPanel.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.stops_prompt_fragment_container).findViewById(R.id.btn_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getInfo(false);
            }
        });

        findViewById(R.id.show_stops).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_up);
                ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.stops_fragment_container);
                hiddenPanel.startAnimation(bottomUp);
                hiddenPanel.setVisibility(View.VISIBLE);

                RecyclerView recyclerView = findViewById(R.id.stops_fragment_container).findViewById(R.id.rv_stopsList);
                recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                stopNameAdapter = new StopNameAdapter(getApplicationContext(), stopNames);
                recyclerView.setAdapter(stopNameAdapter);
            }
        });

        findViewById(R.id.stops_fragment_container).findViewById(R.id.btn_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation bottomDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_down);
                ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.stops_fragment_container);
                hiddenPanel.startAnimation(bottomDown);
                hiddenPanel.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.stops_fragment_container).findViewById(R.id.btn_dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation bottomDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_down);
                ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.stops_fragment_container);
                hiddenPanel.startAnimation(bottomDown);
                hiddenPanel.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.name_fragment_container).findViewById(R.id.btn_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(NewVehicleActivity.this, CheckInActivity.class));
            }
        });

        findViewById(R.id.name_fragment_container).findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            TextInputLayout name_container = findViewById(R.id.name_container);
            vehicleName = name_container.getEditText().getText().toString();

            if (!vehicleName.equals("")) {
                checkDocumentFound();
            } else {
                name_container.setError("Please enter a valid name");
            }
        });

        Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_up);
        ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.name_fragment_container);
        hiddenPanel.startAnimation(bottomUp);
        hiddenPanel.setVisibility(View.VISIBLE);

        findViewById(R.id.reset_view).setOnClickListener(this);

        hideSystemUI();

        mAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                updateUI(mAuth.getCurrentUser());
            }
        });

        updateUI(mAuth.getCurrentUser());

        getLocationPermission();
    }

    private void addStop(String stopName, boolean checked) {
        if (checked) {
            //user is near stop -> get location data
            Map<String, Object> stop = new HashMap<>();
            getDeviceLocation(new NearbyVehiclesLocationActivity.GPSCallback() {
                @Override
                public void onLocation(Location location) {
                    stop.put("pos", new GeoPoint(location.getLatitude(), location.getLongitude()));
                    stop.put("travelTime", 0);

                    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
                    try {
                        stop.put("arr", sdf.parse("01-01-2000"));
                        stop.put("dep", sdf.parse("01-01-2000"));
                    } catch (Exception e) {
                        // cry
                    }

                    stops.put(stopName, stop);

                    db.collection("routes").document(vehicleName)
                            .update("stops", stops);
                }
            });
        }
        final Map<String, Object> addUserToArrayMap = new HashMap<>();
        addUserToArrayMap.put("stopsAll", FieldValue.arrayUnion(stopName));

        db.collection("routes").document(vehicleName)
                .update(addUserToArrayMap);
    }

    private void checkDocumentFound() {
        db.collection("routes").document(vehicleName).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        TextInputLayout name_container = findViewById(R.id.name_container);
                        name_container.setError("Name already exists");
                    } else {
                        startLocationUpdates(vehicleName);

                        Animation bottomDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_down);
                        ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.name_fragment_container);
                        hiddenPanel.startAnimation(bottomDown);
                        hiddenPanel.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Connection to server failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getInfo(boolean writeData) {
        db.collection("routes").document(vehicleName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            vehicle = document.getData();
                            if (writeData) updateData();
                            else getUpdates(false);
                        } else {
                            Toast.makeText(getApplicationContext(), "Connection to server failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateData() {
        new CountDownTimer(10000, 1000) {

            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                getUpdates(true);
            }

        }.start();
    }

    private void getUpdates(boolean writeData) {
        getDeviceLocation(new NearbyVehiclesLocationActivity.GPSCallback() {
            @Override
            public void onLocation(Location location) {
                Map<String, Object> updates = new HashMap<>();

                stops = (Map<String, Object>) vehicle.get("stops");
                stopNames = new ArrayList<>(stops.keySet());
                stopNamesAll = (ArrayList<String>) vehicle.get("stopsAll");

                updates.put("currentPos", new GeoPoint(location.getLatitude(), location.getLongitude()));

                GeoPoint prevGeoPoint = (GeoPoint) vehicle.get("currentPos");
                Location prevLocation = new Location("");
                prevLocation.setLatitude(prevGeoPoint.getLatitude());
                prevLocation.setLongitude(prevGeoPoint.getLongitude());

                double prevSpeed = (double) vehicle.get("speed");

                Timestamp lastUpdated = (Timestamp) vehicle.get("lastUpdated");

                Date currentTime = new Date(System.currentTimeMillis());

                long differenceInMilliSeconds
                        = Math.abs(currentTime.getTime() - lastUpdated.toDate().getTime());

                double differenceInHours = (double) differenceInMilliSeconds / (1000 * 60 * 60);

                double currentSpeed = (prevLocation.distanceTo(location) * 0.00062137) / differenceInHours;

                if (prevSpeed == 0) {
                    updates.put("speed", currentSpeed);
                } else {
                    updates.put("speed", (currentSpeed + prevSpeed) / 2);
                }

                if (writeData) updateDocument(vehicleName, updates, true);
                else {
                    TextInputLayout stopNameContainer = findViewById(R.id.stops_prompt_fragment_container).findViewById(R.id.name_container);
                    String stopName = stopNameContainer.getEditText().getText().toString();

                    if (stopName.equals("")) {
                        stopNameContainer.setError("Please enter a valid stop name");
                    } else if (stopNamesAll.contains(stopName)) {
                        stopNameContainer.setError("Stop name already exists");
                    } else {
                        addStop(stopName, ((CheckBox) findViewById(R.id.stops_prompt_fragment_container).findViewById(R.id.near_stop)).isChecked());

                        Animation bottomDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_down);
                        ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.stops_prompt_fragment_container);
                        hiddenPanel.startAnimation(bottomDown);
                        hiddenPanel.setVisibility(View.GONE);
                    }
                }
            }
        });
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

    private void startLocationUpdates(String name) {
        Map<String, Object> vehicle = new HashMap<>();
        vehicle.put("name", name);
        vehicle.put("lastUpdated", FieldValue.serverTimestamp());

        getDeviceLocation(new NearbyVehiclesLocationActivity.GPSCallback() {
            @Override
            public void onLocation(Location location) {
                GeoPoint currentPos = new GeoPoint(location.getLatitude(), location.getLongitude());
                vehicle.put("currentPos", currentPos);

                ArrayList<GeoPoint> pointsList = new ArrayList<>();
                pointsList.add(currentPos);
                vehicle.put("routePoints", pointsList);

                Map<String, Object> stops = new HashMap<>();
                ArrayList<String> stopsAll = new ArrayList<>();

                vehicle.put("stops", stops);
                vehicle.put("stopsAll", stopsAll);

                vehicle.put("speed", 0.0d);

                updateDocument(name, vehicle, false);
            }
        });
    }

    private void updateDocument(String name, Map<String, Object> vehicle, boolean callUpdate) {
        if (callUpdate) {
            db.collection("routes")
                    .document(name)
                    .update(vehicle)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "DocumentSnapshot successfully written");
                            updateData();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing document", e);
                        }
                    });
        } else {
            db.collection("routes")
                    .document(name)
                    .set(vehicle)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "DocumentSnapshot successfully written");
                            getInfo(true);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing document", e);
                        }
                    });
        }
    }

    interface GPSCallback{
        void onLocation(Location location);
    }

    private void getDeviceLocation(NearbyVehiclesLocationActivity.GPSCallback callback) {
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

    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }


    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(NewVehicleActivity.this);
    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
        }

        mMap.setOnCameraMoveListener(this);
        mMap.setMyLocationEnabled(true);

        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        findViewById(R.id.reset_view).callOnClick();

        getRoutes(googleMap);
    }

    private void getRoutes(GoogleMap map) {
        db.collection("routes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<Map<String, Object>> data = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                data.add(document.getData());
                            }
                            drawRoute(data, map);
                        } else {
                            Toast.makeText(getApplicationContext(), "Error reaching database.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void drawRoute(ArrayList<Map<String, Object>> data, GoogleMap map) {
        PolylineOptions polylineOptions = new PolylineOptions();

        for (int i = 0; i < data.size(); i++) {
            ArrayList<GeoPoint> coords = (ArrayList<GeoPoint>) data.get(i).get("routePoints");
            for (int j = 0; j < coords.size(); j++) {
                GeoPoint point = coords.get(j);
                polylineOptions.add(new LatLng(point.getLatitude(), point.getLongitude()));
            }

            GeoPoint point = coords.get(0);
            polylineOptions.add(new LatLng(point.getLatitude(), point.getLongitude()));
        }

        polylineOptions.
                width(20)
                .color(Color.RED)
                .geodesic(true);


        Polyline polyline = map.addPolyline(polylineOptions);

        drawTrain(data, map);
    }

    private void drawTrain(ArrayList<Map<String, Object>> data, GoogleMap map) {
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> stops = (Map<String,Object>) data.get(i).get("stops");
            String currentStop = (String) data.get(i).get("currentStop");
            ArrayList<String> stopNames = new ArrayList<>(stops.keySet());
            int currentInd = stopNames.indexOf(currentStop);

            ArrayList<GeoPoint> stopPos = new ArrayList<>();

            for (Map.Entry<String, Object> stop : stops.entrySet()) {
                Map<String, Object> stopInfo = (Map<String, Object>) stop.getValue();
                stopPos.add((GeoPoint) stopInfo.get("pos"));
            }

            ArrayList<GeoPoint> routePoints = (ArrayList<GeoPoint>) data.get(i).get("routePoints");
            GeoPoint currentPos = (GeoPoint) data.get(i).get("currentPos");

            double minDis = distBetweenCoords(routePoints.get(0), currentPos);
            int minInd = 0;

            for (int j = 1; j < routePoints.size(); j++) {
                if (distBetweenCoords(routePoints.get(j), currentPos) < minDis) {
                    minDis = distBetweenCoords(routePoints.get(j), currentPos);
                    minInd = j;
                }
            }

            float angle = (float) angleFromCoordinate(routePoints.get(minInd), currentPos);

            for (int j = 0; j < stopPos.size(); j++) {
                mMap.addMarker(new MarkerOptions().position(new LatLng(stopPos.get(j).getLatitude(), stopPos.get(j).getLongitude())).flat(true).icon(bitmapFromVector(getApplicationContext(), R.drawable.ic_stop_indicator)));
            }

            if (minInd == currentInd) {
                mMap.addMarker(new MarkerOptions().position(new LatLng(currentPos.getLatitude(), currentPos.getLongitude())).rotation(angle).flat(true).icon(bitmapFromVector(getApplicationContext(), R.drawable.ic_bus_marker_resized_sq))).setTag(i);
            } else {
                mMap.addMarker(new MarkerOptions().position(new LatLng(currentPos.getLatitude(), currentPos.getLongitude())).rotation(angle*-1).flat(true).icon(bitmapFromVector(getApplicationContext(), R.drawable.ic_bus_marker_resized_sq))).setTag(i);
            }
        }
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

    private double angleFromCoordinate(GeoPoint geoPoint1, GeoPoint geoPoint2) {
        double lat1 = geoPoint1.getLatitude();
        double lat2 = geoPoint2.getLatitude();
        double long1 = geoPoint1.getLongitude();
        double long2 = geoPoint2.getLongitude();

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        brng = 270 - brng; // count degrees counter-clockwise - remove to make clockwise

        return brng;
    }

    private BitmapDescriptor bitmapFromVector(Context context, int vectorResId) {
        // below line is use to generate a drawable.
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        // below line is use to set bounds to our vector drawable.
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        // below line is use to create a bitmap for our
        // drawable which we have added.
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicWidth(), Bitmap.Config.ARGB_8888);
        // below line is use to add bitmap in our canvas.
        Canvas canvas = new Canvas(bitmap);
        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas);
        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onCameraMove() {
        VisibleRegion vr = mMap.getProjection().getVisibleRegion();
        double left = vr.latLngBounds.southwest.longitude;
        double top = vr.latLngBounds.northeast.latitude;
        double right = vr.latLngBounds.northeast.longitude;
        double bottom = vr.latLngBounds.southwest.latitude;

        Location center = new Location("center");
        center.setLatitude(vr.latLngBounds.getCenter().latitude);
        center.setLongitude(vr.latLngBounds.getCenter().longitude);

        Location middleLeftCornerLocation = new Location("center");
        middleLeftCornerLocation.setLatitude(center.getLatitude());
        middleLeftCornerLocation.setLongitude(left);

        int dis = Math.round(center.distanceTo(middleLeftCornerLocation) / 1000);
        TextView radius = findViewById(R.id.radius);
        if (dis != 0) radius.setText(Integer.toString(dis) + "mi");
        else radius.setText("<1mi");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.reset_view) {
            // Get the center of the Map.
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                getLocationPermission();
                return;
            }
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                        .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                        .zoom(15)
                                        .bearing(0)
                                        .tilt(0)
                                        .build();
                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                            }
                        }
                    });
        }
    }

    private void updateUI(FirebaseUser user)
    {
        if (user == null)
        {
            Toast.makeText(getApplicationContext(), "Session Timed Out", Toast.LENGTH_SHORT).show();
            Intent logoutIntent = new Intent(getApplicationContext(), PathwayActivity.class);
            startActivity(logoutIntent);
        }
    }
}