package ru.mtuci.trafficcap002.ui.experiments.datasets;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

import ru.mtuci.trafficcap002.R;

public class DatasetSelectorActivity extends AppCompatActivity {
    public static final String DATASET_SELECTOR_NAMES = "ru.mtuci.trafficcap002.DATASET_SELECTOR_NAMES";
    public static final String DATASET_SELECTOR_DISPLAY_NAMES = "ru.mtuci.trafficcap002.DATASET_SELECTOR_DISPLAY_NAMES";
    public static final String DATASET_MULTIPLE_SELECT = "ru.mtuci.trafficcap002.DATASET_MULTIPLE_SELECT";

    private Toolbar selector_toolbar;
    private RecyclerView selector_recycler;
    private Button choose_button;

    private String[] names, display_names;
    private boolean multiple_select;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dataset_selector_layout);

        selector_toolbar = findViewById(R.id.dataset_selector_toolbar);
        selector_recycler = findViewById(R.id.dataset_selector_recycler);
        choose_button = findViewById(R.id.dataset_selector_choose);

        final Intent intent;
        intent = getIntent();
        names = intent.getStringArrayExtra(DATASET_SELECTOR_NAMES);
        display_names = intent.getStringArrayExtra(DATASET_SELECTOR_DISPLAY_NAMES);
        multiple_select = intent.getBooleanExtra(DATASET_MULTIPLE_SELECT, false);

        selector_recycler.setLayoutManager(new LinearLayoutManager(this));
        selector_recycler.setAdapter(multiple_select ? new MultipleChoiceDatasetAdapter(this, names, display_names) : new SingleChoiceDatasetAdapter(this, names, display_names));
    }
}
