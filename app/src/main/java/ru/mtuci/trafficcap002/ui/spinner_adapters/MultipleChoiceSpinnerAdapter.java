package ru.mtuci.trafficcap002.ui.spinner_adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import ru.mtuci.trafficcap002.R;

public class MultipleChoiceSpinnerAdapter implements SpinnerAdapter {
    private final Spinner spinner;
    private final Context context;
    private final LayoutInflater inflater;
    private final String[] names, display_names;
    private final boolean[] selected;

    public MultipleChoiceSpinnerAdapter(Spinner spinner, String[] names, String[] display_names) {
        this.spinner = spinner;
        this.context = spinner.getContext();
        this.inflater = LayoutInflater.from(this.context);
        assert names.length == display_names.length;
        this.names = names;
        this.display_names = display_names;
        this.selected = new boolean[names.length];
    }

    @Override
    public View getDropDownView(int i, View old, ViewGroup viewGroup) {
        final View view;
        view = (old != null) ? old : inflater.inflate(R.layout.multiple_choice_dropdown, viewGroup, false);
        final CheckBox checkbox;
        checkbox = view.findViewById(R.id.multiple_choice_checkbox);
        checkbox.setText(display_names[i]);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                selected[i] = b;
            }
        });
        checkbox.setChecked(selected[i]);
        final TextView name_view;
        name_view = view.findViewById(R.id.multiple_choice_name_view);
        name_view.setText(names[i]);
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
        return names.length;
    }

    @Override
    public Object getItem(int i) {
        return names[i];
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
    public View getView(int i, View old, ViewGroup viewGroup) {
        final View view;
        view = (old != null) ? old : inflater.inflate(R.layout.multiple_choice_view, viewGroup, false);
        final TextView multiple_text;
        multiple_text = view.findViewById(R.id.multiple_text_view);
        int c=0;
        for (boolean b : selected) c += b ? 1 : 0;
        multiple_text.setText(context.getString(R.string.elements_selected) + c);
        return view;
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
        return names.length == 0;
    }
}
