package ru.mtuci.trafficcap002.ui.experiments.datasets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.mtuci.trafficcap002.R;
import ru.mtuci.trafficcap002.labels.LabelAdapter;

public class SingleChoiceDatasetAdapter extends RecyclerView.Adapter<SingleChoiceDatasetAdapter.DatasetHolder> {
    private final String[] names, display_names;
    private final Activity activity;
    private final LayoutInflater inflater;

    public SingleChoiceDatasetAdapter(Activity activity, String[] names, String[] display_names) {
        assert names.length == display_names.length;
        this.activity = activity;
        this.inflater = LayoutInflater.from(activity);
        this.names = names;
        this.display_names = display_names;
    }

    @NonNull
    @Override
    public SingleChoiceDatasetAdapter.DatasetHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DatasetHolder(inflater.inflate(R.layout.dataset_single_choice_fragment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SingleChoiceDatasetAdapter.DatasetHolder holder, int position) {
        holder.bind(names[position], display_names[position]);
    }

    @Override
    public int getItemCount() {
        return names.length;
    }

    public class DatasetHolder extends RecyclerView.ViewHolder {
        private TextView displayname_view, name_view;
        private Button select_button;

        public DatasetHolder(@NonNull View itemView) {
            super(itemView);

            displayname_view = itemView.findViewById(R.id.dataset_single_displayname_view);
            name_view = itemView.findViewById(R.id.dataset_single_name_view);
            select_button = itemView.findViewById(R.id.dataset_single_select_button);
        }

        public void bind(String name, String display_name) {
            displayname_view.setText(display_name);
            name_view.setText(name);
            select_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Intent result_intent;
                    result_intent = new Intent();
                    //result_intent.putExtra();
                    // TODO сделать возврат выбранного
                    activity.setResult(Activity.RESULT_OK);
                    activity.finish();
                }
            });
        }
    }
}
