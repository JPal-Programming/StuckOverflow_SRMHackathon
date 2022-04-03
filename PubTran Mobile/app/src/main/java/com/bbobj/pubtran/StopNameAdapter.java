package com.bbobj.pubtran;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class StopNameAdapter extends RecyclerView.Adapter<StopNameAdapter.ViewHolder> {
    private ArrayList<String> mData;
    private LayoutInflater mInflater;

    // data is passed into the constructor
    public StopNameAdapter(Context context, ArrayList<String> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.layout_stop_name, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String animal = mData.get(position);
        holder.tv_stopName.setText(animal);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tv_stopName;

        ViewHolder(View itemView) {
            super(itemView);
            tv_stopName = itemView.findViewById(R.id.tv_stopName);
        }
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
    }
}
