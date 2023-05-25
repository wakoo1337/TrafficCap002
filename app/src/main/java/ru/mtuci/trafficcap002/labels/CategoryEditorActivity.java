package ru.mtuci.trafficcap002.labels;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.mtuci.trafficcap002.R;
import ru.mtuci.trafficcap002.ui.CaptureFragment;

public class CategoryEditorActivity extends AppCompatActivity {
    public static final String LABEL_NAMES = "ru.mtuci.trafficcap002.LABEL_NAMES";
    public static final String LABEL_DISPLAY_NAMES = "ru.mtuci.trafficcap002.LABEL_DISPLAY_NAMES";
    public static final String CATEGORY_DISPLAY_NAME = "ru.mtuci.trafficcap002.CATEGORY_DISPLAY_NAME";

    private EditText displayname_edit, name_edit;
    private Button add_label_button, save_button;
    private RecyclerView labels_recycler;
    private List<String> names, display_names;
    private int category_index;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_editor_layout);

        displayname_edit = findViewById(R.id.category_editor_displayname_edit);
        name_edit = findViewById(R.id.category_editor_name_edit);
        add_label_button = findViewById(R.id.category_editor_add_label_button);
        labels_recycler = findViewById(R.id.category_editor_labels_recycler);
        save_button = findViewById(R.id.category_editor_save_button);

        final Intent intent = getIntent();

        final String[] names_array = intent.getStringArrayExtra(LABEL_NAMES);
        final String[] display_names_array = intent.getStringArrayExtra(LABEL_DISPLAY_NAMES);

        assert names_array.length == display_names_array.length;

        category_index = intent.getIntExtra(CaptureFragment.CATEGORY_INDEX, 0);

        names = new ArrayList<>(Arrays.asList(names_array));
        display_names = new ArrayList<>(Arrays.asList(display_names_array));

        final RecyclerView.Adapter<?> adapter = new LabelAdapter(this, names, display_names);

        labels_recycler.setLayoutManager(new LinearLayoutManager(this));
        labels_recycler.setAdapter(adapter);

        add_label_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String display_name = displayname_edit.getEditableText().toString();
                final String name = name_edit.getEditableText().toString();
                if (!(display_name.equals("") || name.equals(""))) {
                    names.add(name);
                    display_names.add(display_name);
                    adapter.notifyItemInserted(names.size()-1);
                }
            }
        });
        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent result_intent;
                result_intent = new Intent();
                result_intent.putExtra(CaptureFragment.NEW_NAMES_ARRAY, names.toArray(new String[0]));
                result_intent.putExtra(CaptureFragment.NEW_DISPLAY_NAMES_ARRAY, display_names.toArray(new String[0]));
                result_intent.putExtra(CaptureFragment.CATEGORY_INDEX, category_index);
                setResult(Activity.RESULT_OK, result_intent);
                finish();
            }
        });
    }
}
