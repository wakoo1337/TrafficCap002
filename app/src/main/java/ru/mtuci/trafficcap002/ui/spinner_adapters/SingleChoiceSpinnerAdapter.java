package ru.mtuci.trafficcap002.ui.spinner_adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import ru.mtuci.trafficcap002.R;

public class SingleChoiceSpinnerAdapter implements SpinnerAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private final String[] names, display_names;

    public SingleChoiceSpinnerAdapter(Context context, String[] names, String[] display_names) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        assert names.length == display_names.length;
        this.names = names;
        this.display_names = display_names;
    }

    @Override
    public View getDropDownView(int i, View old, ViewGroup viewGroup) {
        final View view;
        view = (old != null) ? old : inflater.inflate(R.layout.label_dropdown, viewGroup, false);
        final TextView dispname_view;
        dispname_view = view.findViewById(R.id.label_dispname_view);
        dispname_view.setText(display_names[i]);
        final TextView name_view;
        name_view = view.findViewById(R.id.label_name_view);
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
        return names.length == 0;
    }
}
