package ru.mtuci.trafficcap002.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import ru.mtuci.trafficcap002.R;
import ru.mtuci.trafficcap002.ui.attributes.AttributesLayoutActivity;
import ru.mtuci.trafficcap002.ui.spinner_adapters.MultipleChoiceSpinnerAdapter;
import ru.mtuci.trafficcap002.ui.spinner_adapters.SingleChoiceSpinnerAdapter;

public class NewExperimentActivity extends AppCompatActivity {
    public static final String CLASSIFIER_NAMES="ru.mtuci.trafficcap002.CLASSIFIER_NAMES";
    public static final String CLASSIFIER_DISPLAY_NAMES="ru.mtuci.trafficcap002.CLASSIFIER_DISPLAY_NAMES";
    public static final String MULTIPLE_CLASSIFIERS="ru.mtuci.trafficcap002.MULTIPLE_CLASSIFIERS";
    public static final String DATASET_NAMES="ru.mtuci.trafficcap002.DATASET_NAMES";
    public static final String DATASET_DISPLAY_NAMES="ru.mtuci.trafficcap002.DATASET_DISPLAY_NAMES";
    public static final String MULTIPLE_DATASETS="ru.mtuci.trafficcap002.MULTIPLE_DATASETS";


    private Toolbar toolbar;
    private Button attrs_button;
    private Spinner classifier_spinner, dataset_spinner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_experiment_layout);

        toolbar = findViewById(R.id.newex_toolbar);
        attrs_button = findViewById(R.id.attrs_button);
        classifier_spinner = findViewById(R.id.newex_classifier_spinner);
        dataset_spinner = findViewById(R.id.newex_dataset_spinner);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        attrs_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent;
                intent = new Intent(NewExperimentActivity.this, AttributesLayoutActivity.class);
                startActivity(intent);
            }
        });

        final Intent intent;
        intent = getIntent();
        final String[] classifier_names, classifier_display_names;
        classifier_names = intent.getStringArrayExtra(CLASSIFIER_NAMES);
        classifier_display_names = intent.getStringArrayExtra(CLASSIFIER_DISPLAY_NAMES);
        final boolean multiple_classifiers = intent.getBooleanExtra(MULTIPLE_CLASSIFIERS, false);
        final String[] dataset_names, dataset_display_names;
        dataset_names = intent.getStringArrayExtra(DATASET_NAMES);
        dataset_display_names = intent.getStringArrayExtra(DATASET_DISPLAY_NAMES);
        final boolean multiple_datasets = intent.getBooleanExtra(MULTIPLE_DATASETS, false);

        classifier_spinner.setAdapter(multiple_classifiers ? new MultipleChoiceSpinnerAdapter(classifier_spinner, classifier_names, classifier_display_names) : new SingleChoiceSpinnerAdapter(this, classifier_names, classifier_display_names));
        dataset_spinner.setAdapter(multiple_datasets ? new MultipleChoiceSpinnerAdapter(dataset_spinner, dataset_names, dataset_display_names) : new SingleChoiceSpinnerAdapter(this, dataset_names, dataset_display_names));
    }
}
