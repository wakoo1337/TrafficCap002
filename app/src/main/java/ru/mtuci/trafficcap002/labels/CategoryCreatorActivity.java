package ru.mtuci.trafficcap002.labels;

import static ru.mtuci.trafficcap002.ui.CaptureFragment.NEW_CATEGORY_DISPLAY_NAME;
import static ru.mtuci.trafficcap002.ui.CaptureFragment.NEW_CATEGORY_NAME;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ru.mtuci.trafficcap002.R;

public class CategoryCreatorActivity extends AppCompatActivity {
    private EditText displayname_edit, name_edit;
    private Button create_button, back_button;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_creator_layout);

        displayname_edit = findViewById(R.id.category_creator_displayname_edit);
        name_edit = findViewById(R.id.category_creator_name_edit);
        create_button = findViewById(R.id.category_creator_create_button);
        back_button = findViewById(R.id.category_creator_back_button);

        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        create_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                final String name = name_edit.getEditableText().toString();
                final String display_name = displayname_edit.getEditableText().toString();
                if (!(display_name.equals("") || name.equals(""))) {
                    final Intent result_intent;
                    result_intent = new Intent();
                    result_intent.putExtra(NEW_CATEGORY_NAME, name);
                    result_intent.putExtra(NEW_CATEGORY_DISPLAY_NAME, display_name);
                    setResult(Activity.RESULT_OK, result_intent);
                    finish();
                }
            }
        });
    }
}
