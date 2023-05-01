package com.wakoo.trafficcap002;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LabelsAdapter extends RecyclerView.Adapter<LabelsAdapter.ViewLabelHolder> {
    private final List<Label> labels;
    private final LayoutInflater inflater;

    LabelsAdapter(Context context) {
        this.labels = new ArrayList<>();
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public LabelsAdapter.ViewLabelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewLabelHolder(inflater.inflate(R.layout.label_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull LabelsAdapter.ViewLabelHolder holder, int position) {
        holder.setLabel(labels.get(position));
    }

    @Override
    public int getItemCount() {
        return labels.size();
    }

    public void setLabels(List<Label> labels) {
        this.labels.clear();
        this.labels.addAll(labels);
    }

    public static class ViewLabelHolder extends RecyclerView.ViewHolder {
        private final SwitchCompat sw;
        private Label label;

        public ViewLabelHolder(View view) {
            super(view);
            sw = (SwitchCompat) view.findViewById(R.id.label_switch);
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    label.setChecked(b);
                }
            });
        }

        public void setLabel(Label label) {
            this.label = label;
            this.sw.setText(label.getLabel());
            this.sw.setChecked(label.getChecked());
        }
    }
}
