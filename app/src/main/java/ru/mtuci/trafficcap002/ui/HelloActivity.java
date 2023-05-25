package ru.mtuci.trafficcap002.ui;

import static java.net.HttpURLConnection.HTTP_OK;
import static ru.mtuci.trafficcap002.ui.MainActivity.PREFERENCES;
import static ru.mtuci.trafficcap002.ui.MainActivity.PREFERENCE_SITE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import ru.mtuci.trafficcap002.R;

public class HelloActivity extends AppCompatActivity {
    Toolbar hello_toolbar;
    TextView hello_site_edit;
    CheckBox hello_ok_checkbox;
    Button hello_siteselect_button;
    Thread check_thread;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hello_layout);

        hello_toolbar = findViewById(R.id.hello_toolbar);
        hello_site_edit = findViewById(R.id.hello_site_edit);
        hello_ok_checkbox = findViewById(R.id.hello_ok_checkbox);
        hello_siteselect_button = findViewById(R.id.hello_siteselect_button);

        final SharedPreferences prefs;
        prefs = getSharedPreferences(PREFERENCES, 0);
        if (prefs.getString(PREFERENCE_SITE, null) != null) {
            openMainActivity();
        }
        hello_site_edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (check_thread != null) check_thread.interrupt();
                check_thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String url_string;
                        url_string = editable.toString();
                        try {
                            final URL url;
                            url = new URL(url_string + "/checker");
                            final HttpURLConnection connection;
                            connection = (HttpURLConnection) url.openConnection();
                            setCheckbox(connection.getResponseCode() == HTTP_OK);
                            connection.disconnect();
                        } catch (MalformedURLException malformed_url) {
                            setCheckbox(false);
                        } catch (IOException ioexcp) {
                            HelloActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(HelloActivity.this, getString(R.string.check_io_error) + ioexcp.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }

                    private void setCheckbox(boolean value) {
                        HelloActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hello_ok_checkbox.setChecked(value);
                                hello_siteselect_button.setEnabled(value);
                            }
                        });
                    }
                });
                check_thread.start();
            }
        });
        hello_siteselect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hello_ok_checkbox.isChecked()) {
                    prefs.edit().putString(PREFERENCE_SITE, hello_site_edit.getEditableText().toString()).apply();
                    openMainActivity();
                }
            }
        });
    }

    private void openMainActivity() {
        final Intent main_intent;
        main_intent = new Intent(this, MainActivity.class);
        startActivity(main_intent);
    }
}
