package com.bbobj.pubtran;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class VehicleListAdapter extends RecyclerView.Adapter<VehicleListAdapter.VehicleListViewHolder> {

    private ArrayList<ArrayList<String>> mVehicleInfo;
    private ArrayList<Map<String, Object>> mStops;
    private ArrayList<String> mCurrentStops;
    private ArrayList<ArrayList<Integer>> mStopDistances;
    private ArrayList<ArrayList<ArrayList<Date>>> mArrDep;
    private ArrayList<ArrayList<Integer>> mTimes;
    private Context mContext;
    private onRecyclerViewUpdatedListener mListener;
    private RecyclerView mView;
    private RelativeLayout mLayout;
    SmoothLinearLayoutManager mLayoutManager;

    private boolean foodItemsExpanded = false;

    public VehicleListAdapter(Context context, VehicleListAdapter.onRecyclerViewUpdatedListener listener, RelativeLayout layout, SmoothLinearLayoutManager layoutManager, ArrayList<ArrayList<String>> vehicleInfo, ArrayList<Map<String, Object>> stops, ArrayList<String> currentStops, ArrayList<ArrayList<Integer>> distances, ArrayList<ArrayList<ArrayList<Date>>> arrDep, ArrayList<ArrayList<Integer>> times) {
        mVehicleInfo = vehicleInfo;
        mStops = stops;
        mCurrentStops = currentStops;
        mStopDistances = distances;
        mArrDep = arrDep;
        mTimes = times;
        mContext = context;
        mListener = listener;
        mLayout = layout;
        mLayoutManager = layoutManager;
    }

    @NonNull
    @Override
    public VehicleListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_vehicle_info, parent, false);
        return new VehicleListViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(VehicleListViewHolder holder, int position) {
        holder.vehicleName.setText(mVehicleInfo.get(position).get(0));
        holder.vehicleInfo.setText(mVehicleInfo.get(position).get(1));

        holder.mCurrentStop.setText(mCurrentStops.get(position));

        ArrayList<String> stopNames = new ArrayList<>(mStops.get(position).keySet());

        int currentStopInd = stopNames.indexOf(mCurrentStops.get(position));
        int stopsLeft = stopNames.size() - currentStopInd - 1;
        int travelTime = mTimes.get(position).get(currentStopInd);

        holder.mNextStop.setText(stopNames.get(currentStopInd+1));

        int totalMinutes = 0;
        for (int i = 0; i < mTimes.get(position).size(); i++) {
            totalMinutes += mTimes.get(position).get(i);
        }

        if (totalMinutes > 60) {
            holder.mTotalMinutes.setText(totalMinutes/60 + "h " + totalMinutes%60 + "m  |  ");
        } else {
            holder.mTotalMinutes.setText(totalMinutes + " m");
        }

        if (travelTime > 60) {
            holder.mTravelMinutes.setText(travelTime/60 + "h " + travelTime%60 + " m");
        } else {
            holder.mTravelMinutes.setText(travelTime + " m");
        }

        holder.mDistance.setText(mStopDistances.get(position).get(currentStopInd) + " mi");
        holder.mStopsLeft.setText(stopsLeft + " stops left");

        String status = "";
        boolean isDanger = false;

        if (currentStopInd == 0) {
            status = "On time";
            isDanger = false;
        } else {
            Timestamp prevArr = (Timestamp) ((Map<String, Object>) mStops.get(currentStopInd-1)).get("arr");
            Timestamp prevDep = (Timestamp) ((Map<String, Object>) mStops.get(currentStopInd-1)).get("dep");

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            holder.mStatus.setCardBackgroundColor(mContext.getColor(isDanger ? R.color.danger : R.color.success));
            holder.mStatusText.setTextColor(mContext.getColor(isDanger ? R.color.textDanger : R.color.textSuccess));
        }
        holder.mStatusText.setText(status);

        SmoothLinearLayoutManager layoutManager = new SmoothLinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
        holder.stops.setLayoutManager(layoutManager);
        StopsAdapter adapter = new StopsAdapter(mContext, mVehicleInfo.get(position), stopNames, mStopDistances.get(position), mArrDep.get(position), mTimes.get(position));
        holder.stops.setAdapter(adapter);
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
    public int getItemCount() {
        return mTimes.size();
    }

    public class VehicleListViewHolder extends RecyclerView.ViewHolder {

        public TextView mCurrentStop;
        public TextView mNextStop;
        public TextView mTotalMinutes;
        public TextView mTravelMinutes;
        public TextView mDistance;
        public TextView mStopsLeft;
        public CardView mStatus;
        public TextView mStatusText;

        ImageView expandVehicleDetails;

        TextView vehicleName;
        TextView vehicleInfo;

        RecyclerView stops;
        RelativeLayout vehicleContainer;

        View detailsFragment;

        private BottomSheetBehavior bottomSheetBehavior;

        public VehicleListViewHolder(View itemView, VehicleListAdapter.onRecyclerViewUpdatedListener listener) {
            super(itemView);

            mView = mLayout.findViewById(R.id.vehiclesView);

            stops = itemView.findViewById(R.id.stops);
            vehicleContainer = itemView.findViewById(R.id.vehicle_container);

            expandVehicleDetails = itemView.findViewById(R.id.expand_vehicle_details);

            mCurrentStop = itemView.findViewById(R.id.tv_currentStop);
            mNextStop = itemView.findViewById(R.id.tv_nextStop);
            mTotalMinutes = itemView.findViewById(R.id.tv_totalMinutes);
            mTravelMinutes = itemView.findViewById(R.id.tv_travelMinutes);
            mDistance = itemView.findViewById(R.id.tv_stopDistance);
            mStopsLeft = itemView.findViewById(R.id.tv_stopsLeft);
            mStatus = itemView.findViewById(R.id.cv_status);
            mStatusText = itemView.findViewById(R.id.tv_status);

            vehicleName = itemView.findViewById(R.id.store_name);
            vehicleInfo = itemView.findViewById(R.id.store_info);

            detailsFragment = itemView.findViewById(R.id.details_fragment);
            detailsFragment.findViewById(R.id.header_container).setElevation(0);

            bottomSheetBehavior = BottomSheetBehavior.from(detailsFragment);
            bottomSheetBehavior.setDraggable(false);

            ViewGroup.LayoutParams params = vehicleContainer.getLayoutParams();
            params.height = RecyclerView.LayoutParams.MATCH_PARENT;
            vehicleContainer.setLayoutParams(params);

            minimizeFragment(listener);

            detailsFragment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (foodItemsExpanded) {
                        minimizeFragment(listener);
                    } else {
                        expandFragment(listener);
                    }
                }
            });

            mView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    if (foodItemsExpanded) minimizeFragment(listener);
                }
            });
        }

        private void expandFragment(VehicleListAdapter.onRecyclerViewUpdatedListener listener) {
            listener.onExpanded();

            mLayoutManager.setScrollEnabled(false);

            detailsFragment.setBackgroundColor(Color.WHITE);

            ViewGroup.LayoutParams params = vehicleContainer.getLayoutParams();
            params.height = RecyclerView.LayoutParams.MATCH_PARENT;
            vehicleContainer.setLayoutParams(params);

            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            AnimationSet animSet = new AnimationSet(true);
            animSet.setInterpolator(new DecelerateInterpolator());
            animSet.setFillAfter(true);
            animSet.setFillEnabled(true);

            final RotateAnimation animRotate = new RotateAnimation(0.0f, 180.0f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f);

            animRotate.setDuration(500);
            animRotate.setFillAfter(true);
            animSet.addAnimation(animRotate);

            expandVehicleDetails.startAnimation(animSet);

            foodItemsExpanded = true;
        }

        private void minimizeFragment(VehicleListAdapter.onRecyclerViewUpdatedListener listener) {
            listener.onMinimized();

            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            mLayoutManager.setScrollEnabled(true);

            detailsFragment.setBackgroundColor(Color.TRANSPARENT);

            AnimationSet animSet = new AnimationSet(true);
            animSet.setInterpolator(new DecelerateInterpolator());
            animSet.setFillAfter(true);
            animSet.setFillEnabled(true);

            final RotateAnimation animRotate = new RotateAnimation(0.0f, 0.0f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f);

            animRotate.setDuration(500);
            animRotate.setFillAfter(true);
            animSet.addAnimation(animRotate);

            expandVehicleDetails.startAnimation(animSet);

            foodItemsExpanded = false;
        }
    }

    public void closeDetailsFragment() {
        mLayout.findViewById(R.id.store_details_container).setVisibility(View.GONE);
    }

    public interface onRecyclerViewUpdatedListener {
        default void onExpanded() {}
        default void onMinimized() {}
    }
}
