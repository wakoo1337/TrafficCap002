package ru.mtuci.trafficcap002.ui.attributes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.mtuci.trafficcap002.R;

public class AttributeScalerAdapter extends RecyclerView.Adapter<AttributeScalerAdapter.ScalerHolder> {
    private final AttributeScalerActivity activity;
    private final LayoutInflater inflater;
    private final List<Scaler> scalers;

    public AttributeScalerAdapter(AttributeScalerActivity activity, List<Scaler> scalers) {
        this.activity = activity;
        this.inflater = LayoutInflater.from(activity);
        this.scalers = scalers;
    }

    @NonNull
    @Override
    public AttributeScalerAdapter.ScalerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ScalerHolder(inflater.inflate(R.layout.scaler_generic, parent, false), activity);
    }

    @Override
    public void onBindViewHolder(@NonNull AttributeScalerAdapter.ScalerHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return scalers.size();
    }

    public void move(int from, int to) {
        Scaler temp;
        temp = scalers.remove(from);
        scalers.add(to, temp);
        notifyItemMoved(from, to);
    }

    public class ScalerHolder extends RecyclerView.ViewHolder {
        private final View view;
        private final Context context;

        public ScalerHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.view = itemView;
            this.context = context;
            final TextView dots_view;
            dots_view = itemView.findViewById(R.id.dots_view);
            dots_view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        activity.startDragging(ScalerHolder.this);
                    }
                    return true;
                }
            });
        }

        public void bind(int position) {
            ViewGroup attr_container;
            attr_container = view.findViewById(R.id.attr_container);
            attr_container.removeAllViews();
            LayoutInflater.from(context).inflate(scalers.get(position).getConfigLayout(), (ViewGroup) attr_container, true);
        }
    }
}
