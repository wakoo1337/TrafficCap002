package ru.mtuci.trafficcap002.ui.attributes;

import static androidx.recyclerview.widget.ItemTouchHelper.DOWN;
import static androidx.recyclerview.widget.ItemTouchHelper.END;
import static androidx.recyclerview.widget.ItemTouchHelper.START;
import static androidx.recyclerview.widget.ItemTouchHelper.UP;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.mtuci.trafficcap002.R;

public class AttributeScalerActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView scalers_recycler;
    private List<Scaler> scalers;
    private ItemTouchHelper touch_helper;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.attr_numberic_layout);

        toolbar = findViewById(R.id.attr_numberic_toolbar);
        scalers_recycler = findViewById(R.id.scalers_recycler);

        final AttributeScalerAdapter adapter;
        scalers = new ArrayList<>();
        scalers.add(new Scaler() {
            @Override
            public double[] scale(double[] x) {
                return new double[0];
            }

            @Override
            public int getConfigLayout() {
                return R.layout.scaler_multiply_view;
            }
        });
        scalers.add(new Scaler() {
            @Override
            public double[] scale(double[] x) {
                return new double[0];
            }

            @Override
            public int getConfigLayout() {
                return R.layout.scaler_modulo_view;
            }
        });
        scalers.add(new Scaler(){
            @Override
            public double[] scale(double[] x) {
                return new double[0];
            }

            @Override
            public int getConfigLayout() {
                return R.layout.scaler_thresholds_view;
            }
        });
        adapter = new AttributeScalerAdapter(this, scalers);
        final ScalerTouchHelper helper;
        helper = new ScalerTouchHelper(UP | DOWN | START | END, 0);

        touch_helper = new ItemTouchHelper(helper);
        touch_helper.attachToRecyclerView(scalers_recycler);
        scalers_recycler.setLayoutManager(new LinearLayoutManager(this));
        scalers_recycler.setAdapter(adapter);

        setSupportActionBar(toolbar);
    }

    void startDragging(RecyclerView.ViewHolder holder) {
        touch_helper.startDrag(holder);
    }
}
