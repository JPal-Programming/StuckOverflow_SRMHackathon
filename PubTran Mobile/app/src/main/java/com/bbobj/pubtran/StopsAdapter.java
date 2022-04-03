package com.bbobj.pubtran;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class StopsAdapter extends RecyclerView.Adapter<StopsAdapter.ViewHolder> {

    private ArrayList<String> mVehicleInfo;
    private ArrayList<String> mNames;
    private ArrayList<Integer> mDistances;
    private ArrayList<ArrayList<Date>> mArrDep;
    private ArrayList<Integer> mTimes;
    private Context mContext;

    public StopsAdapter(Context context, ArrayList<String> vehicleInfo, ArrayList<String> names, ArrayList<Integer> distances, ArrayList<ArrayList<Date>> arrDep, ArrayList<Integer> times) {
        mContext = context;
        mVehicleInfo = vehicleInfo;
        mNames = names;
        mDistances = distances;
        mArrDep = arrDep;
        mTimes = times;
    }

    @NonNull
    @Override
    public StopsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_stop, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StopsAdapter.ViewHolder holder, int position) {
        holder.tv_distance.setText(mDistances.get(position) + " mi • ");
        holder.tv_name.setText(mNames.get(position));

        SimpleDateFormat sdf= new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getDefault());

        holder.tv_arrive.setText(sdf.format(mArrDep.get(position).get(0)));
        holder.tv_depart.setText(sdf.format(mArrDep.get(position).get(1)));

        holder.tv_travel.setText(mTimes.get(position) + " min");
    }

    @Override
    public int getItemCount() {
        return mNames.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_distance;
        TextView tv_name;
        TextView tv_arrive;
        TextView tv_depart;
        TextView tv_travel;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_distance = itemView.findViewById(R.id.tv_distance);
            tv_name = itemView.findViewById(R.id.tv_name);
            tv_arrive = itemView.findViewById(R.id.tv_arrive);
            tv_depart = itemView.findViewById(R.id.tv_depart);
            tv_travel = itemView.findViewById(R.id.tv_travel);
        }
    }
}
