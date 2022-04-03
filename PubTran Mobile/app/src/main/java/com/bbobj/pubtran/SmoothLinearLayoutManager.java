package com.bbobj.pubtran;

import android.content.Context;
import android.graphics.PointF;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class SmoothLinearLayoutManager extends LinearLayoutManager {
    private boolean isScrollEnabled = true;

    public SmoothLinearLayoutManager(Context context) {
        super(context, HORIZONTAL, false);
    }

    public SmoothLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                                       int position) {
        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext());
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    public void setScrollEnabled(boolean flag) {
        isScrollEnabled = flag;
    }

    @Override
    public boolean canScrollHorizontally() {
        return isScrollEnabled && super.canScrollHorizontally();
    }

    private class TopSnappedSmoothScroller extends LinearSmoothScroller {

    public TopSnappedSmoothScroller(Context context) {
        super(context);

    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        return SmoothLinearLayoutManager.this
                .computeScrollVectorForPosition(targetPosition);
    }

    @Override
    protected int getVerticalSnapPreference() {
        return SNAP_TO_START;
    }
    }
}
