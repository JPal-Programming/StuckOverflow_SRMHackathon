package com.bbobj.pubtran;

import android.content.Context;

public class NearbyVehicle {

    private Context mContext;
    private String mCurrentStop;
    private String mNextStop;
    private int mTotalMinutes;
    private int mTravelMinutes;
    private int mDistance;
    private int mStopsLeft;
    private String mStatus;
    private boolean mDanger;
    private String mName;

    public NearbyVehicle(Context context, String currentStop, String nextStop, int totalMinutes, int travelMinutes, int distance, int stopsLeft, String status, boolean danger, String name) {
        mContext = context;
        mCurrentStop = currentStop;
        mNextStop = nextStop;
        mTotalMinutes = totalMinutes;
        mTravelMinutes = travelMinutes;
        mDistance = distance;
        mStopsLeft = stopsLeft;
        mStatus = status;
        mDanger = danger;
        mName = name;
    }

    public Context getContext() {
        return mContext;
    }

    public String getCurrentStop() {
        return mCurrentStop;
    }

    public String getNextStop() {
        return mNextStop;
    }

    public int getTotalMinutes() {
        return mTotalMinutes;
    }

    public int getTravelMinutes() {
        return mTravelMinutes;
    }

    public int getDistance() {
        return mDistance;
    }

    public int getStopsLeft() {
        return mStopsLeft;
    }

    public String getStatus() {
        return mStatus;
    }

    public boolean isDanger() {
        return mDanger;
    }

    public String getName() {
        return mName;
    }
}
