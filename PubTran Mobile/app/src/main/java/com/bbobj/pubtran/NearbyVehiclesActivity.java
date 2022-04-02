package com.bbobj.pubtran;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class NearbyVehiclesActivity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private RecyclerView rv_nearbyVehicles;
    private NearbyVehicleAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    ArrayList<NearbyVehicle> mVehicles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_vehicles);

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), NewVehicleActivity.class));
            }
        });

        getRoutes();

        TextInputLayout search = findViewById(R.id.search_container);
        search.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable e) {
                filter(e.toString());
            }
        });
    }

    private void filter(String s) {
        ArrayList<NearbyVehicle> filteredList = new ArrayList<>();

        for (NearbyVehicle vehicle : mVehicles) {
            if (vehicle.getName().toLowerCase().contains(s.toLowerCase())) {
                filteredList.add(vehicle);
            }
        }

        mAdapter.filterList(filteredList);
    }

    private void getRoutes() {
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
            Map<String, Object> vehicleInfo = data.get(i);
            String currentStop = (String) vehicleInfo.get("currentStop");

            Map<String, Object> stops = (Map<String, Object>) data.get(i).get("stops");
            ArrayList<String> stopNames = new ArrayList<>(stops.keySet());
            int currentInd = stopNames.indexOf(currentStop);

            ArrayList<Long> stopTimes = new ArrayList<>();

            for (Map.Entry<String, Object> stop : stops.entrySet()) {
                Map<String, Object> stopInfo = (Map<String, Object>) stop.getValue();
                stopTimes.add((Long) stopInfo.get("travelTime"));
            }

            double totalTime = 0;

            for (int j = 0; j < stopTimes.size(); j++) {
                totalTime += stopTimes.get(j);
            }

            GeoPoint currentStopPos = (GeoPoint) ((Map<String, Object>) stops.get(stopNames.get(currentInd))).get("pos");
            GeoPoint nextStopPos = (GeoPoint) ((Map<String, Object>) stops.get(stopNames.get(currentInd+1))).get("pos");

            String status = "";
            boolean isDanger = false;

            if (currentInd == 0) {
                status = "On time";
                isDanger = false;
            } else {
                Timestamp prevArr = (Timestamp) ((Map<String, Object>) stops.get(currentInd-1)).get("arr");
                Timestamp prevDep = (Timestamp) ((Map<String, Object>) stops.get(currentInd-1)).get("dep");

                Date prevArrDate = prevArr.toDate();
                Date prevDepDate = prevDep.toDate();

                long differenceInMilliSeconds
                        = Math.abs(prevDepDate.getTime() - prevArrDate.getTime());

                // Calculating the difference in Hours
                long differenceInHours
                        = (differenceInMilliSeconds / (60 * 60 * 1000))
                        % 24;

                // Calculating the difference in Minutes
                long differenceInMinutes
                        = (differenceInMilliSeconds / (60 * 1000)) % 60;

                if (differenceInMinutes < 5) {
                    status = "On time";
                    isDanger = false;
                } else if (differenceInMilliSeconds < 0) {
                    differenceInHours *= -1;
                    differenceInMinutes *= -1;
                    status = "Ahead by " + ((differenceInHours > 0) ? differenceInHours + " hr " : "") + differenceInMinutes + " min";
                    isDanger = false;
                } else {
                    status = "Delayed by " + ((differenceInHours > 0) ? differenceInHours + " hr " : "") + differenceInMinutes + " min";
                    isDanger = true;
                }
            }

            String name = (String) vehicleInfo.get("name");

            NearbyVehicle vehicle = new NearbyVehicle(this, currentStop, stopNames.get(currentInd+1),(int) Math.round(totalTime), stopTimes.get(currentInd).intValue(), (int) Math.round(distBetweenCoords(currentStopPos, nextStopPos)), stops.size() - currentInd + 1, status, isDanger, name);
            mVehicles.add(vehicle);
        }

        rv_nearbyVehicles = findViewById(R.id.rv_vehiclesList);
        rv_nearbyVehicles.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new NearbyVehicleAdapter(mVehicles);

        rv_nearbyVehicles.setLayoutManager(mLayoutManager);
        rv_nearbyVehicles.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new NearbyVehicleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(getApplicationContext(), NearbyVehiclesLocationActivity.class);
                intent.putExtra("position", position);
                startActivity(intent);
            }
        });
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
}