package ru.mtuci.trafficcap002.ui.attributes;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.mtuci.trafficcap002.R;

public class AttributesAdapter extends RecyclerView.Adapter<AttributesAdapter.AttributeHolder> {
    private final Context context;
    private final LayoutInflater inflater;
    private final List<Attribute> attributes;

    public AttributesAdapter(Context context, List<Attribute> attributes) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.attributes = attributes;
    }

    @NonNull
    @Override
    public AttributeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AttributeHolder(inflater.inflate(R.layout.attr_fragment, parent, false), context);
    }

    @Override
    public void onBindViewHolder(@NonNull AttributeHolder holder, int position) {
        holder.bind(attributes.get(position));
    }

    @Override
    public int getItemCount() {
        return attributes.size();
    }

    public class AttributeHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final CheckBox attr_switch;
        private final TextView type_view;
        private final Button config_button;

        public AttributeHolder(@NonNull View view, Context context) {
            super(view);
            this.context = context;
            attr_switch = view.findViewById(R.id.attr_switch);
            type_view = view.findViewById(R.id.type_view);
            config_button = view.findViewById(R.id.config_button);
        }

        public void bind(Attribute attribute) {
            attr_switch.setChecked(attribute.getEnabled());
            attr_switch.setText(attribute.getDisplayName());
            type_view.setText((attribute instanceof AttributeNumberic) ? R.string.numberic_attribute : R.string.nominal_attribute);
            config_button.setOnClickListener((attribute instanceof AttributeNumberic) ? new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Intent intent;
                    intent = new Intent(context, AttributeScalerActivity.class);
                    context.startActivity(intent);
                }
            } : new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
        }
    }
}
