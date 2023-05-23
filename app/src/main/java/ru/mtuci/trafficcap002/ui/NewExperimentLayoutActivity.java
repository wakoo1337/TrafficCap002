package ru.mtuci.trafficcap002.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import ru.mtuci.trafficcap002.R;
import ru.mtuci.trafficcap002.ui.attributes.AttributesLayoutActivity;

public class NewExperimentLayoutActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private Button attrs_button;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_experiment_layout);

        toolbar = findViewById(R.id.newex_toolbar);
        attrs_button = findViewById(R.id.attrs_button);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        attrs_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent;
                intent = new Intent(NewExperimentLayoutActivity.this, AttributesLayoutActivity.class);
                startActivity(intent);
            }
        });
    }
}
