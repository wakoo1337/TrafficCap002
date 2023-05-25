package ru.mtuci.trafficcap002.labels;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.mtuci.trafficcap002.R;
import ru.mtuci.trafficcap002.ui.CaptureFragment;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryHolder> {
    private final Context context;
    private final List<Category> categories;
    private final LayoutInflater inflater;
    private final ActivityResultLauncher<Intent> add_result;

    public CategoriesAdapter(Context context, List<Category> categories, ActivityResultLauncher<Intent> add_result) {
        this.context = context;
        this.categories = categories;
        this.inflater = LayoutInflater.from(context);
        this.add_result = add_result;
    }

    @NonNull
    @Override
    public CategoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CategoryHolder(context, inflater.inflate(R.layout.category_fragment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryHolder holder, int position) {
        holder.bind(categories.get(position), position);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public class CategoryHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final SwitchCompat enable_switch;
        private final Spinner value_spinner;
        private final Button add_button;

        public CategoryHolder(Context context, View view) {
            super(view);
            this.context = context;
            this.enable_switch = view.findViewById(R.id.enable_switch);
            this.value_spinner = view.findViewById(R.id.value_spinner);
            this.add_button = view.findViewById(R.id.add_button);
        }

        public void bind(Category category, int index) {
            enable_switch.setChecked(category.getEnabled());
            enable_switch.setText(category.getDisplayName());
            enable_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    category.setEnabled(b);
                }
            });
            LabelsAdapter adapter = new LabelsAdapter(context, category.getLabels());
            value_spinner.setAdapter(adapter);
            value_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    category.setIndex(i);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            add_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Intent intent;
                    intent = new Intent(context, CategoryEditorActivity.class);
                    final List<Label> labels = category.getLabels();
                    final String[] names, display_names;
                    names = new String[labels.size()];
                    display_names = new String[labels.size()];
                    int i=0;
                    for (final Label label : labels) {
                        names[i] = label.getName();
                        display_names[i] = label.getDisplayName();
                        i++;
                    }
                    intent.putExtra(CategoryEditorActivity.LABEL_NAMES, names);
                    intent.putExtra(CategoryEditorActivity.LABEL_DISPLAY_NAMES, display_names);
                    intent.putExtra(CaptureFragment.CATEGORY_INDEX, index);
                    intent.putExtra(CategoryEditorActivity.CATEGORY_DISPLAY_NAME, category.getDisplayName());
                    CategoriesAdapter.this.add_result.launch(intent);
                }
            });
        }

        private class LabelsAdapter implements SpinnerAdapter {
            private final Context context;
            private final List<Label> labels;
            private final LayoutInflater inflater;

            public LabelsAdapter(Context context, List<Label> labels) {
                this.context = context;
                this.labels = labels;
                this.inflater = LayoutInflater.from(this.context);
            }

            @Override
            public View getDropDownView(int i, View old_view, ViewGroup viewGroup) {
                final View view = (old_view != null) ? old_view : inflater.inflate(R.layout.label_dropdown, viewGroup, false);
                final TextView dispname_view;
                dispname_view = view.findViewById(R.id.label_dispname_view);
                final TextView name_view;
                name_view = view.findViewById(R.id.label_name_view);
                final Label label = labels.get(i);
                dispname_view.setText(label.getDisplayName());
                name_view.setText(label.getName());
                return view;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver dataSetObserver) {

            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

            }

            @Override
            public int getCount() {
                return labels.size();
            }

            @Override
            public Object getItem(int i) {
                return labels.get(i).getDisplayName();
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                return getDropDownView(i, view, viewGroup);
            }

            @Override
            public int getItemViewType(int i) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return labels.isEmpty();
            }
        }
    }
}
