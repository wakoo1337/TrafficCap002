package ru.mtuci.trafficcap002.ui.experiments.datasets;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.mtuci.trafficcap002.R;

public class MultipleChoiceDatasetAdapter extends RecyclerView.Adapter<MultipleChoiceDatasetAdapter.DatasetHolder> {
    private final String[] names, display_names;
    private final boolean[] selected;
    private final Context context;
    private final LayoutInflater inflater;

    public MultipleChoiceDatasetAdapter(Context context, String[] names, String[] display_names) {
        assert names.length == display_names.length;
        this.selected = new boolean[names.length];
        this.context = context;
        this.inflater = LayoutInflater.from(this.context);
        this.names = names;
        this.display_names = display_names;
    }

    @NonNull
    @Override
    public MultipleChoiceDatasetAdapter.DatasetHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MultipleChoiceDatasetAdapter.DatasetHolder(inflater.inflate(R.layout.dataset_multiple_choice_fragment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MultipleChoiceDatasetAdapter.DatasetHolder holder, int position) {
        holder.bind(position, names[position], display_names[position]);
    }

    @Override
    public int getItemCount() {
        return names.length;
    }

    public List<String> getSelectedNames() {
        final List<String> selected_set = new ArrayList<>();
        for (int i=0;i < selected.length;i++) {
            if (selected[i]) selected_set.add(names[i]);
        }
        return selected_set;
    }

    public class DatasetHolder extends RecyclerView.ViewHolder {
        private TextView displayname_view, name_view;
        private AppCompatCheckBox select_checkbox;

        public DatasetHolder(@NonNull View itemView) {
            super(itemView);

            displayname_view = itemView.findViewById(R.id.dataset_multiple_displayname_view);
            name_view = itemView.findViewById(R.id.dataset_multiple_name_view);
            select_checkbox = itemView.findViewById(R.id.dataset_multiple_select_checkbox);
        }

        public void bind(int index, String name, String display_name) {
            displayname_view.setText(display_name);
            name_view.setText(name);
            select_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
                    selected[index] = value;
                }
            });
        }
    }
}
