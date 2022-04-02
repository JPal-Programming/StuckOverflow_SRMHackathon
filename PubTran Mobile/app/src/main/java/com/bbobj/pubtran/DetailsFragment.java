package com.bbobj.pubtran;

import static android.view.View.GONE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class DetailsFragment extends Fragment {
    private String TAG = "DetailsFragment";
    private Context mContext;
    private View mView;
    private GoogleMap mMap;
    private LatLng storeLocation = new LatLng(41.712002143875374, -88.20353417832028);

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int PLACE_PICKER_REQUEST = 1;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));

    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private void updateInformation() {
        TextView nameTV = mView.findViewById(R.id.store_name);
        TextView addressTV = mView.findViewById(R.id.store_address);
        TextView[] storeHours = {mView.findViewById(R.id.sunday_hours), mView.findViewById(R.id.monday_hours), mView.findViewById(R.id.tuesday_hours), mView.findViewById(R.id.wednesday_hours), mView.findViewById(R.id.thursday_hours), mView.findViewById(R.id.friday_hours), mView.findViewById(R.id.saturday_hours)};
        TextView locationTV = mView.findViewById(R.id.location_type);
        TextView directionsTV = mView.findViewById(R.id.pickup_directions);
        TextView phoneTV = mView.findViewById(R.id.phone_number);
        TextView emailTV = mView.findViewById(R.id.email);
        TextView websiteTV = mView.findViewById(R.id.website);

        String name = this.getArguments().getString("name");
        String info = this.getArguments().getString("info");
        String address = this.getArguments().getString("address");
        String[] hours = this.getArguments().getStringArray("hours");
        String locationType = this.getArguments().getString("locationType");
        String pickupDirections = this.getArguments().getString("pickupDirections");
        String phoneNumber = this.getArguments().getString("phoneNumber");
        String email = this.getArguments().getString("email");
        String website = this.getArguments().getString("website");

        phoneTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
                startActivity(intent);
            }
        });

        emailTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_EMAIL, email);
                if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        websiteTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(website));
                startActivity(i);
            }
        });

        nameTV.setText(name);

        ImageView backArrow = mView.findViewById(R.id.details_back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mView.setVisibility(GONE);
            }
        });

        if (!pickupDirections.equals("")) {
            directionsTV.setText(pickupDirections);
        }

        if (!phoneNumber.equals("")) {
            phoneTV.setText(phoneNumber);
        }

        addressTV.setText(address);
        phoneTV.setPaintFlags(phoneTV.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
        emailTV.setText(email);
        emailTV.setPaintFlags(emailTV.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
        websiteTV.setText(website);
        websiteTV.setPaintFlags(websiteTV.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.layout_store_details, container, false);
        updateInformation();

        mView.findViewById(R.id.btn_directions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("google.navigation:q=jewel+osco+95th+street"));
                startActivity(intent);
            }
        });

        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                mMap = googleMap;

                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    getLocationPermission();
                }

                mMap.setMyLocationEnabled(true);

                mMap.getUiSettings().setZoomControlsEnabled(false);
                mMap.getUiSettings().setCompassEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);

                zoomIn();

                // Add a marker in Naperville and move the camera
                BitmapDescriptor icon2 = BitmapDescriptorFactory.fromResource(R.drawable.rescued_marker_primary_resized);
                mMap.addMarker(new MarkerOptions().position(storeLocation).icon(bitmapFromVector(mContext, R.drawable.rescued_marker_primary_resized))).setTag(1);
            }
        });

        mContext = getContext();

        return mView;
    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(mContext, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(mContext, COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
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

    private void zoomIn() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
            return;
        }
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(storeLocation.latitude, storeLocation.longitude))
                .zoom(15)
                .bearing(0)
                .tilt(0)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
}
