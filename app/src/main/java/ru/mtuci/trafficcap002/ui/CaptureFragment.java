package ru.mtuci.trafficcap002.ui;

import static android.app.Activity.RESULT_OK;

import static ru.mtuci.trafficcap002.ui.MainActivity.APP_NAME_KEY;
import static ru.mtuci.trafficcap002.ui.MainActivity.APP_PACKAGE_KEY;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.mtuci.trafficcap002.R;
import ru.mtuci.trafficcap002.labels.CategoriesAdapter;
import ru.mtuci.trafficcap002.labels.Category;
import ru.mtuci.trafficcap002.labels.CategoryCreatorActivity;
import ru.mtuci.trafficcap002.labels.Label;
import ru.mtuci.trafficcap002.ui.appselect.AppSelectActivity;

public class CaptureFragment extends Fragment {
    public static final String NEW_CATEGORY_NAME = "ru.mtuci.trafficcap002.NEW_CATEGORY_NAME";
    public static final String NEW_CATEGORY_DISPLAY_NAME = "ru.mtuci.trafficcap002.NEW_CATEGORY_DISPLAY_NAME";

    private final List<Category> categories;

    public CaptureFragment(List<Category> categories) {
        this.categories = categories;
        categories.addAll(List.of(
                new Category("services", "Сервисы", List.of(new Label("vk", "ВКонтакте"), new Label("youtube", "YouTube"), new Label("telegram", "Telegram"))),
                new Category("browsers", "Браузеры", List.of(new Label("firefox", "Firefox"), new Label("chrome", "Chrome"))),
                new Category("app_types", "Типы приложений", List.of(new Label("im", "IM"), new Label("web", "Веб"), new Label("music", "Музыка"), new Label("malware", "Вредоносное ПО")))
        ));
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.capture_fragment, container, true);
    }

    private final ActivityResultLauncher<Intent> app_select_result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                final Intent data = result.getData();
                app_edit.setText(data.getStringExtra(APP_NAME_KEY));
                app_package = data.getStringExtra(APP_PACKAGE_KEY);
                toggle_button.setEnabled(true);
            }
        }
    });

    public static final String NEW_NAMES_ARRAY = "ru.mtuci.trafficcap002.NEW_NAMES_ARRAY";
    public static final String NEW_DISPLAY_NAMES_ARRAY = "ru.mtuci.trafficcap002.NEW_DISPLAY_NAMES_ARRAY";
    public static final String CATEGORY_INDEX = "ru.mtuci.trafficcap002.CATEGORY_INDEX";

    private final ActivityResultLauncher<Intent> add_label_result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                final Intent data = result.getData();
                final int index = data.getIntExtra(CATEGORY_INDEX, 0);
                final String[] names = data.getStringArrayExtra(NEW_NAMES_ARRAY);
                final String[] display_names = data.getStringArrayExtra(NEW_DISPLAY_NAMES_ARRAY);

                final Category old = categories.get(index);
                final Label[] labels = new Label[names.length];
                for (int i=0;i < names.length;i++) {
                    labels[i] = new Label(names[i], display_names[i]);
                }
                categories.set(index, new Category(old.getName(), old.getDisplayName(), Arrays.asList(labels)));
                categories_recycler.getAdapter().notifyItemChanged(index);
            }
        }
    });

    private final ActivityResultLauncher<Intent> add_category_result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                final Intent data = result.getData();
                final String name = data.getStringExtra(NEW_CATEGORY_NAME);
                final String display_name = data.getStringExtra(NEW_CATEGORY_DISPLAY_NAME);
                categories.add(new Category(name, display_name, new ArrayList<>()));
                categories_recycler.getAdapter().notifyItemInserted(categories.size()-1);
            }
        }
    });

    private String app_package;
    private EditText app_edit;
    private Button appselect_button, toggle_button;
    private RecyclerView categories_recycler;
    private FloatingActionButton newcategory_button;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        app_edit = view.findViewById(R.id.capture_app_edit);
        appselect_button = view.findViewById(R.id.capture_appselect_button);
        toggle_button = view.findViewById(R.id.capture_toggle_button);
        categories_recycler = view.findViewById(R.id.capture_categories_recycler);
        newcategory_button = view.findViewById(R.id.capture_newcategory_button);

        appselect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent;
                intent = new Intent(CaptureFragment.this.getActivity(), AppSelectActivity.class);
                app_select_result.launch(intent);
            }
        });
        newcategory_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent;
                intent = new Intent(CaptureFragment.this.getActivity(), CategoryCreatorActivity.class);
                add_category_result.launch(intent);
            }
        });

        categories_recycler.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        categories_recycler.setAdapter(new CategoriesAdapter((AppCompatActivity) this.getActivity(), categories, add_label_result));
    }
}
