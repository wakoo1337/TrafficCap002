package ru.mtuci.trafficcap002.labels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.mtuci.trafficcap002.R;

public class LabelAdapter extends RecyclerView.Adapter<LabelAdapter.LabelHolder> {
    private final List<String> names, display_names;
    private final LayoutInflater inflater;

    public LabelAdapter(Context context, List<String> names, List<String> display_names) {
        this.names = names;
        this.display_names = display_names;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public LabelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LabelHolder(inflater.inflate(R.layout.label_fragment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull LabelHolder holder, int position) {
        holder.bind(display_names.get(position), names.get(position));
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    public class LabelHolder extends RecyclerView.ViewHolder {
        private final TextView human_view, computer_view;

        public LabelHolder(@NonNull View itemView) {
            super(itemView);
            human_view = itemView.findViewById(R.id.human_view);
            computer_view = itemView.findViewById(R.id.computer_view);
        }

        public void bind(String human, String computer) {
            human_view.setText(human);
            computer_view.setText(computer);
        }
    }
}
