package com.wakoo.trafficcap002;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private Button start_button, stop_button;
    private TextView status_view;
    private EditText appcapture_edit;
    private CaptureService.CaptureServiceBinder binder;
    private final ActivityResultLauncher<Intent> capture_launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                startVpn();
            } else {
                status_view.setText(R.string.forbidden_message);
            }
        }
    });
    private final ServiceConnection capture_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (CaptureService.CaptureServiceBinder) service;
            status_view.setText(R.string.connected_message);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
            unbindService(this);
            status_view.setText(R.string.unbound_message);
        }

        @Override
        public void onNullBinding(ComponentName name) {
            binder = null;
            unbindService(this);
            status_view.setText(R.string.nullbind_message);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start_button = findViewById(R.id.start_button);
        stop_button = findViewById(R.id.stop_button);
        status_view = findViewById(R.id.status_view);
        appcapture_edit = findViewById(R.id.appcapture_edit);

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent prepare;
                prepare = CaptureService.prepare(MainActivity.this);
                if (prepare == null) {
                    startVpn();
                } else {
                    capture_launcher.launch(prepare);
                }
            }
        });
        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(capture_connection);
            }
        });
    }

    private void startVpn() {
        Intent start_intent;
        start_intent = new Intent(this, CaptureService.class);
        final String capture = appcapture_edit.getText().toString();
        if (!capture.isEmpty()) {
            start_intent.putExtra(CaptureService.APP_TO_LISTEN, capture);
        }
        bindService(start_intent, capture_connection, BIND_AUTO_CREATE | BIND_ABOVE_CLIENT);
    }
}