package com.bbobj.pubtran;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener, GoogleMap.OnMarkerClickListener, View.OnClickListener, VehicleListAdapter.onRecyclerViewUpdatedListener {

    private GoogleMap mMap;

    private static final String TAG = "MapsActivity";


    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    private Boolean mLocationPermissionsGranted = false;

    private ArrayList<String> mCurrentStops = new ArrayList<>();
    private ArrayList<ArrayList<String>> mVehicleInfo = new ArrayList<>();
    private ArrayList<Map<String, Object>> mStops = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> mDistances = new ArrayList<>();
    private ArrayList<ArrayList<ArrayList<Date>>> mArrDep = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> mTimes = new ArrayList<>();

    private BottomNavigationView bnv;

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        findViewById(R.id.reset_view).setOnClickListener(this);

        hideSystemUI();

        bnv = findViewById(R.id.bnv);
        bnv.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.explore) {
                    startActivity(new Intent(MapsActivity.this, MapsActivity.class));
                    return false;
                } else if (item.getItemId() == R.id.check_in) {
                    startActivity(new Intent(MapsActivity.this, CheckInActivity.class));
                    return false;
                } else {
                    startActivity(new Intent(MapsActivity.this, SettingsActivity.class));
                    return false;
                }
            }
        });

        mAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                updateUI(mAuth.getCurrentUser());
            }
        });

        updateUI(mAuth.getCurrentUser());

        getLocationPermission();
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

        mapFragment.getMapAsync(MapsActivity.this);
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

        initData();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
        }

        mMap.setOnCameraMoveListener(this);
        mMap.setOnMarkerClickListener(this);
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
            Map<String, Object> stops = (Map<String, Object>) data.get(i).get("stops");
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

    private void initData() {
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
                            initRecyclerView(data);
                        } else {
                            Toast.makeText(getApplicationContext(), "Error reaching database.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void initRecyclerView(ArrayList<Map<String, Object>> data) {
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> vehicle = data.get(i);

            String vehicleName = (String) vehicle.get("name");
            Date lastUpdated = ((Timestamp) vehicle.get("lastUpdated")).toDate();

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa");

            ArrayList<String> vehicleInfo = new ArrayList<>();
            vehicleInfo.add(vehicleName);
            vehicleInfo.add("Last Updated: " + sdf.format(lastUpdated));
            mVehicleInfo.add(vehicleInfo);

            mCurrentStops.add((String) vehicle.get("currentStop"));

            Map<String, Object> stops = (Map<String, Object>) vehicle.get("stops");

            ArrayList<GeoPoint> positions = new ArrayList<>();
            ArrayList<Integer> distances = new ArrayList<>();
            ArrayList<ArrayList<Date>> arrDep = new ArrayList<>();
            ArrayList<Integer> times = new ArrayList<>();

            for (Map.Entry<String, Object> stop : stops.entrySet()) {
                Map<String, Object> stopInfo = (Map<String, Object>) stop.getValue();

                positions.add((GeoPoint) stopInfo.get("pos"));

                ArrayList<Date> tempArrDep = new ArrayList<>();
                tempArrDep.add(((Timestamp) stopInfo.get("arr")).toDate());
                tempArrDep.add(((Timestamp) stopInfo.get("dep")).toDate());
                arrDep.add(tempArrDep);

                times.add(((Long) stopInfo.get("travelTime")).intValue());
            }

            for (int j = 0; j < positions.size()-1; j++) {
                distances.add((int) Math.round(distBetweenCoords(positions.get(j), positions.get(j+1))));
            }

            distances.add(0);

            mStops.add(stops);
            mDistances.add(distances);
            mArrDep.add(arrDep);
            mTimes.add(times);
        }

        RelativeLayout layout = findViewById(R.id.maps_activity_container);

        SmoothLinearLayoutManager layoutManager = new SmoothLinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        RecyclerView recyclerView = findViewById(R.id.vehiclesView);
        recyclerView.setLayoutManager(layoutManager);
        VehicleListAdapter adapter = new VehicleListAdapter(this, this, layout, layoutManager, mVehicleInfo, mStops, mCurrentStops, mDistances, mArrDep, mTimes);
        recyclerView.setAdapter(adapter);

        SnapHelper helper = new LinearSnapHelper();
        helper.attachToRecyclerView(recyclerView);
    }

    public static Date parseDate(String date) {
        try {
            return new SimpleDateFormat("HH:mm").parse(date);
        } catch (ParseException e) {
            return null;
        }
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
    public boolean onMarkerClick(@NonNull Marker marker) {
        try {
            int id = (int) marker.getTag();
            RecyclerView recyclerView = findViewById(R.id.vehiclesView);
            recyclerView.smoothScrollToPosition(id);
        } catch (Exception e) {}
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.reset_view) {// Get the center of the Map.
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

    @Override
    public void onExpanded() {
        findViewById(R.id.reset_view).setVisibility(View.GONE);
        RecyclerView recyclerView = findViewById(R.id.vehiclesView);
        recyclerView.getLayoutParams().height = RecyclerView.LayoutParams.MATCH_PARENT;
    }

    @Override
    public void onMinimized() {
        findViewById(R.id.reset_view).setVisibility(View.VISIBLE);
        RecyclerView recyclerView = findViewById(R.id.vehiclesView);
        recyclerView.getLayoutParams().height = (int) getResources().getDimension(R.dimen.recyclerview_height);
    }
}