package ru.mtuci.trafficcap002.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import ru.mtuci.trafficcap002.CaptureService;
import ru.mtuci.trafficcap002.R;
import ru.mtuci.trafficcap002.labels.CategoriesAdapter;
import ru.mtuci.trafficcap002.labels.Category;
import ru.mtuci.trafficcap002.labels.Label;
import ru.mtuci.trafficcap002.ui.appselect.AppSelectActivity;

public class MainLayoutActivity extends AppCompatActivity {
    public static final String APP_PACKAGE_KEY = "ru.mtuci.trafficap002.APP_PACKAGE_KEY";
    public static final String APP_NAME_KEY = "ru.mtuci.trafficap002.APP_NAME_KEY";

    private boolean connected = false;
    private boolean url_ok = false;
    private List<Category> categories;
    private String capture_package;
    private final ActivityResultLauncher<Intent> app_select_result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                app_edit.setText(result.getData().getStringExtra(APP_NAME_KEY));
                capture_package = result.getData().getStringExtra(APP_PACKAGE_KEY);
            }
        }
    });
    private CaptureService.CaptureServiceBinder binder;
    private final ServiceConnection vpn_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (CaptureService.CaptureServiceBinder) service;
            binder.setCategories(categories);
            connected = true;
            setCaptureButton();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
            unbindService(this);
            connected = false;
            setCaptureButton();
        }

        @Override
        public void onNullBinding(ComponentName name) {
            binder = null;
            unbindService(this);
            connected = false;
            setCaptureButton();
        }
    };
    private DrawerLayout drawer;
    private Toolbar toolbar;
    private FloatingActionButton newexperiment_float;
    private ActionBarDrawerToggle drawer_toggle;
    private EditText server_edit, app_edit;
    private final ActivityResultLauncher<Intent> vpn_result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                startVpn();
            } else {
                Toast.makeText(MainLayoutActivity.this, R.string.allow_vpn, Toast.LENGTH_LONG).show();
            }
        }
    });
    private CheckBox server_check;
    private Button appsel_button, capture_button;
    private RecyclerView categories_recycler;

    private void setCaptureButton() {
        capture_button.setText(connected ? R.string.stop : R.string.start);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        drawer = findViewById(R.id.drawer);
        toolbar = findViewById(R.id.main_toolbar);
        newexperiment_float = findViewById(R.id.newexperiment_float);
        server_edit = findViewById(R.id.server_edit);
        server_check = findViewById(R.id.server_check);
        app_edit = findViewById(R.id.app_edit);
        appsel_button = findViewById(R.id.appsel_button);
        capture_button = findViewById(R.id.capture_button);
        categories_recycler = findViewById(R.id.categs_recycler);

        appsel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent;
                intent = new Intent(MainLayoutActivity.this, AppSelectActivity.class);
                app_select_result.launch(intent);
            }
        });
        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connected) {
                    unbindService(vpn_connection);
                    connected = false;
                    setCaptureButton();
                } else if (capture_package != null) {
                    Intent prepare;
                    prepare = CaptureService.prepare(MainLayoutActivity.this);
                    if (prepare == null) {
                        startVpn();
                    } else {
                        vpn_result.launch(prepare);
                    }
                } else
                    Toast.makeText(MainLayoutActivity.this, R.string.app_not_set, Toast.LENGTH_LONG).show();
            }
        });
        server_edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                final String url_string;
                url_string = editable.toString();
                try {
                    final URL url = new URL(url_string);
                    url_ok = true;
                } catch (
                        MalformedURLException mfexcp) {
                    url_ok = false;
                } finally {
                    server_check.setChecked(url_ok);
                }
            }
        });
        newexperiment_float.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final Intent intent;
                        intent = new Intent(MainLayoutActivity.this, NewExperimentLayoutActivity.class);
                        startActivity(intent);
                    }
                }
        );

        categories_recycler.setLayoutManager(new LinearLayoutManager(this));
        categories = List.of(
                new Category("services", "Сервисы", List.of(new Label("vk", "ВКонтакте"), new Label("youtube", "YouTube"), new Label("telegram", "Telegram"))),
                new Category("browsers", "Браузеры", List.of(new Label("firefox", "Firefox"), new Label("chrome", "Chrome"))),
                new Category("app_types", "Типы приложений", List.of(new Label("im", "IM"), new Label("web", "Веб"), new Label("music", "Музыка"), new Label("malware", "Вредоносное ПО")))
        );
        categories_recycler.setAdapter(new CategoriesAdapter(this, categories));
        setCaptureButton();

        setSupportActionBar(toolbar);

        drawer_toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.drawer_opened, R.string.drawer_closed);
        drawer_toggle.setDrawerIndicatorEnabled(true);
        drawer.addDrawerListener(drawer_toggle);
        drawer_toggle.syncState();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawer_toggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawer_toggle.onConfigurationChanged(newConfig);
    }

    private void startVpn() {
        Intent start_intent;
        if (!connected) {
            start_intent = new Intent(this, CaptureService.class);
            final String server = server_edit.getText().toString();
            start_intent.putExtra(CaptureService.SITE_TO_WRITE, url_ok ? null : server);
            start_intent.putExtra(CaptureService.APP_TO_LISTEN, capture_package);
            bindService(start_intent, vpn_connection, BIND_AUTO_CREATE | BIND_ABOVE_CLIENT);
        }
    }
}
