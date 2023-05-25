package ru.mtuci.trafficcap002.ui;

import static ru.mtuci.trafficcap002.ui.NewExperimentActivity.CLASSIFIER_DISPLAY_NAMES;
import static ru.mtuci.trafficcap002.ui.NewExperimentActivity.CLASSIFIER_NAMES;
import static ru.mtuci.trafficcap002.ui.NewExperimentActivity.DATASET_DISPLAY_NAMES;
import static ru.mtuci.trafficcap002.ui.NewExperimentActivity.DATASET_NAMES;
import static ru.mtuci.trafficcap002.ui.NewExperimentActivity.MULTIPLE_CLASSIFIERS;
import static ru.mtuci.trafficcap002.ui.NewExperimentActivity.MULTIPLE_DATASETS;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import ru.mtuci.trafficcap002.R;

public class ExperimentsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.experiments_fragment, container, true);
    }

    private FloatingActionButton new_button;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        new_button = view.findViewById(R.id.experiments_new_button);
        new_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent;
                intent = new Intent(getActivity(), NewExperimentActivity.class);
                intent.putExtra(CLASSIFIER_NAMES, new String[]{"классификатор1", "классификатор2", "классификатор 3"});
                intent.putExtra(CLASSIFIER_DISPLAY_NAMES, new String[]{"Один классификатор", "Второй классификатор", "Третий классификатор"});
                intent.putExtra(MULTIPLE_CLASSIFIERS, false);
                intent.putExtra(DATASET_NAMES, new String[]{"датасет1", "датасет2", "датасет3"});
                intent.putExtra(DATASET_DISPLAY_NAMES, new String[]{"Один датасет", "Второй датасет", "Третий датасет"});
                intent.putExtra(MULTIPLE_DATASETS, true);
                startActivity(intent);
            }
        });
    }
}
