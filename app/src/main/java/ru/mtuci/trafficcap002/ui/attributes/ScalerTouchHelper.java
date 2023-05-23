package ru.mtuci.trafficcap002.ui.attributes;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ScalerTouchHelper extends ItemTouchHelper.SimpleCallback {

    public ScalerTouchHelper(int dragDirs, int swipeDirs) {
        super(dragDirs, swipeDirs);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        final AttributeScalerAdapter adapter = (AttributeScalerAdapter) recyclerView.getAdapter();
        final int from = viewHolder.getAdapterPosition();
        final int to = target.getAdapterPosition();

        adapter.move(from, to);

        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }
}