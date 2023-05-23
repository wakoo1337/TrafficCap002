package ru.mtuci.trafficcap002.ui.attributes;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.mtuci.trafficcap002.R;
import ru.mtuci.trafficcap002.ui.appselect.AppsAdapter;

public class AttributesLayoutActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView attributes_recycler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.attrs_layout);

        toolbar = findViewById(R.id.attrs_toolbar);
        attributes_recycler = findViewById(R.id.attributes_recycler);

        attributes_recycler.setLayoutManager(new LinearLayoutManager(this));
        AttributesAdapter adapter = new AttributesAdapter(this, List.of(new AttributeNumberic("port", "Порт"),
                new AttributeNominal("protocol", "Протокол")));
        attributes_recycler.setAdapter(adapter);

        setSupportActionBar(toolbar);
    }
}
