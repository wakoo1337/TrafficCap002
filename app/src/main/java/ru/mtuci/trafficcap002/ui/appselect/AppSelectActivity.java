package ru.mtuci.trafficcap002.ui.appselect;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.mtuci.trafficcap002.R;

public class AppSelectActivity extends AppCompatActivity {
    Toolbar appsel_toolbar;
    AppsAdapter adapter;
    RecyclerView apps_recycler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_selector_layout);
        appsel_toolbar = findViewById(R.id.appsel_toolbar);
        setSupportActionBar(appsel_toolbar);
        final PackageManager pm = getPackageManager();
        final List<ApplicationInfo> apps;
        apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        apps_recycler = findViewById(R.id.apps_recycler);
        apps_recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppsAdapter(this, apps);
        apps_recycler.setAdapter(adapter);
    }
}
