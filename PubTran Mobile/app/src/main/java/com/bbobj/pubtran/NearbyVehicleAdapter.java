package com.bbobj.pubtran;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class NearbyVehicleAdapter extends RecyclerView.Adapter<NearbyVehicleAdapter.NearbyVehiclesViewHolder> {

    private ArrayList<NearbyVehicle> mVehiclesList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public static class NearbyVehiclesViewHolder extends RecyclerView.ViewHolder {

        public TextView mCurrentStop;
        public TextView mNextStop;
        public TextView mTotalMinutes;
        public TextView mTravelMinutes;
        public TextView mDistance;
        public TextView mStopsLeft;
        public CardView mStatus;
        public TextView mStatusText;
        public TextView mName;

        public NearbyVehiclesViewHolder(View itemView, OnItemClickListener listener) {
            super(itemView);

            mCurrentStop = itemView.findViewById(R.id.tv_currentStop);
            mNextStop = itemView.findViewById(R.id.tv_nextStop);
            mTotalMinutes = itemView.findViewById(R.id.tv_totalMinutes);
            mTravelMinutes = itemView.findViewById(R.id.tv_travelMinutes);
            mDistance = itemView.findViewById(R.id.tv_stopDistance);
            mStopsLeft = itemView.findViewById(R.id.tv_stopsLeft);
            mStatus = itemView.findViewById(R.id.cv_status);
            mStatusText = itemView.findViewById(R.id.tv_status);
            mName = itemView.findViewById(R.id.tv_name);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }

    }

    public NearbyVehicleAdapter(ArrayList<NearbyVehicle> vehiclesList) {
        mVehiclesList = vehiclesList;
    }

    @NonNull
    @Override
    public NearbyVehiclesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_vehicle_listitem, parent, false);
        NearbyVehiclesViewHolder vh = new NearbyVehiclesViewHolder(v, mListener);
        return vh;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(NearbyVehiclesViewHolder holder, int position) {
        NearbyVehicle v = mVehiclesList.get(position);

        holder.mCurrentStop.setText(v.getCurrentStop());
        holder.mNextStop.setText(v.getNextStop());
        if (v.getTotalMinutes() > 60) {
            holder.mTotalMinutes.setText(v.getTotalMinutes()/60 + "h " + v.getTotalMinutes()%60 + "m  |  ");
        } else {
            holder.mTotalMinutes.setText(v.getTotalMinutes() + " m");
        }
        if (v.getTravelMinutes() > 60) {
            holder.mTravelMinutes.setText(v.getTravelMinutes()/60 + "h " + v.getTravelMinutes()%60 + " m");
        } else {
            holder.mTravelMinutes.setText(v.getTravelMinutes() + " m");
        }
        holder.mDistance.setText(v.getDistance() + " mi");
        holder.mStopsLeft.setText(v.getStopsLeft() + " stops left");
        holder.mStatus.setCardBackgroundColor(v.getContext().getColor(v.isDanger() ? R.color.danger : R.color.success));
        holder.mStatusText.setTextColor(v.getContext().getColor(v.isDanger() ? R.color.textDanger : R.color.textSuccess));
        holder.mStatusText.setText(v.getStatus());
        holder.mName.setText(v.getName());
    }

    @Override
    public int getItemCount() { return mVehiclesList.size(); }

    public void filterList(ArrayList<NearbyVehicle> filteredList) {
        mVehiclesList = filteredList;
        notifyDataSetChanged();
    }
}
