package ru.mtuci.trafficcap002.labels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import ru.mtuci.trafficcap002.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public final class LabelsAdapter extends RecyclerView.Adapter<LabelsAdapter.ViewLabelHolder> {
    private final List<Label> labels;
    private final Set<String> active;
    private final LayoutInflater inflater;

    public LabelsAdapter(Context context) {
        this.labels = new ArrayList<>();
        this.active = new ConcurrentSkipListSet<>();
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

    public void setLabels(Set<String> names) {
        this.labels.clear();
        this.active.clear();
        for (final String name : names)
            this.labels.add(new Label(name, active, false));
        Collections.sort(this.labels);
        notifyDataSetChanged();
    }

    public Set<String> getActiveLabels() {
        return active;
    }

    public static class ViewLabelHolder extends RecyclerView.ViewHolder {
        private final SwitchCompat sw;
        private Label label;

        public ViewLabelHolder(View view) {
            super(view);
            sw = view.findViewById(R.id.label_switch);
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
