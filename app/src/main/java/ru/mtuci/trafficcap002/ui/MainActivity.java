package ru.mtuci.trafficcap002.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ru.mtuci.trafficcap002.CaptureService;
import ru.mtuci.trafficcap002.R;
import ru.mtuci.trafficcap002.labels.CategoriesAdapter;
import ru.mtuci.trafficcap002.labels.Category;
import ru.mtuci.trafficcap002.labels.Label;
import ru.mtuci.trafficcap002.ui.appselect.AppSelectActivity;

public class MainActivity extends AppCompatActivity {
    public static final String APP_PACKAGE_KEY = "ru.mtuci.trafficap002.APP_PACKAGE_KEY";
    public static final String APP_NAME_KEY = "ru.mtuci.trafficap002.APP_NAME_KEY";

    public static final String PREFERENCE_SITE = "site_address";
    public static final String PREFERENCES = "prefs";

    private boolean connected = false;
    private boolean url_ok = false;
    private List<Category> categories;
    private String capture_package;
    private final ActivityResultLauncher<Intent> app_select_result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                //app_edit.setText(result.getData().getStringExtra(APP_NAME_KEY));
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
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
            unbindService(this);
            connected = false;
        }

        @Override
        public void onNullBinding(ComponentName name) {
            binder = null;
            unbindService(this);
            connected = false;
        }
    };


    private final ActivityResultLauncher<Intent> vpn_result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                startVpn();
            } else {
                Toast.makeText(MainActivity.this, R.string.allow_vpn, Toast.LENGTH_LONG).show();
            }
        }
    });

    private final ActivityResultLauncher<Intent> new_category_result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult o) {

        }
    });

    public class TabsAdapter extends FragmentStateAdapter {
        private static final int POS_EXPERIMENTS=0;
        private static final int POS_CAPTURE = 1;

        public TabsAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case POS_EXPERIMENTS:
                    return new ExperimentsFragment();
                case POS_CAPTURE:
                    return new CaptureFragment(new ArrayList<>());
                default:
                    return null;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    TabLayout tab_layout;
    ViewPager2 pager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        tab_layout = findViewById(R.id.main_tab_layout);
        pager = findViewById(R.id.main_pager);

        pager.setAdapter(new TabsAdapter(this));
        (new TabLayoutMediator(tab_layout, pager, true, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText((new int[]{R.string.exps, R.string.capture})[position]);
            }
        })).attach();
    }


    private void startVpn() {
        Intent start_intent;
        if (!connected) {
            start_intent = new Intent(this, CaptureService.class);
            //final String server = server_edit.getText().toString();
            //start_intent.putExtra(CaptureService.SITE_TO_WRITE, url_ok ? null : server);
            start_intent.putExtra(CaptureService.APP_TO_LISTEN, capture_package);
            bindService(start_intent, vpn_connection, BIND_AUTO_CREATE | BIND_ABOVE_CLIENT);
        }
    }
}
