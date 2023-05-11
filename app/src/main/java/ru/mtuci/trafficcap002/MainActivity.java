package ru.mtuci.trafficcap002;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import ru.mtuci.trafficcap002.labels.LabelsAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public final class MainActivity extends AppCompatActivity {
    private Button start_button, stop_button, load_button;
    private TextView status_view;
    private EditText appcapture_edit, site_edit;
    private RecyclerView labels_recycler;
    private CaptureService.CaptureServiceBinder binder;
    private LabelsAdapter labels_adapter;
    private boolean connected = false;
    private final ServiceConnection capture_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (CaptureService.CaptureServiceBinder) service;
            binder.setActiveLabelsSet(labels_adapter.getActiveLabels());
            status_view.setText(R.string.connected_message);
            connected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
            unbindService(this);
            status_view.setText(R.string.unbound_message);
            connected = false;
        }

        @Override
        public void onNullBinding(ComponentName name) {
            binder = null;
            unbindService(this);
            status_view.setText(R.string.nullbind_message);
            connected = false;
        }
    };

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start_button = findViewById(R.id.start_button);
        stop_button = findViewById(R.id.stop_button);
        load_button = findViewById(R.id.load_button);
        status_view = findViewById(R.id.status_view);
        appcapture_edit = findViewById(R.id.appcapture_edit);
        site_edit = findViewById(R.id.site_edit);

        labels_recycler = findViewById(R.id.labels_recycler);
        labels_adapter = new LabelsAdapter(this);
        labels_recycler.setAdapter(labels_adapter);
        labels_adapter.setLabels(Set.of(
                "social",
                "web",
                "video",
                "music",
                "vpn",
                "im",
                "anonymize",
                "games",
                "p2p",
                "malware"
        ));

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
                if (connected) {
                    unbindService(capture_connection);
                    status_view.setText(R.string.unbound_message);
                    connected = false;
                }
            }
        });
        load_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    final URL url;
                    url = new URL(site_edit.getText().toString() + "/labels");
                    final Thread t = (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final HttpURLConnection connection;
                                connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("GET");
                                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                    try (final InputStream instream = connection.getInputStream()) {
                                        final List<byte[]> readed;
                                        readed = new LinkedList<>();
                                        int total = 0; // На старых версиях нет readAllBytes, поэтому увы и ах
                                        final int toread = 1024;
                                        int last;
                                        do
                                        {
                                            final byte[] next;
                                            next = new byte[toread];
                                            last = instream.read(next);
                                            total += last;
                                            readed.add(next);
                                        } while (last != -1);
                                        total++;
                                        final byte[] labels_bytes;
                                        labels_bytes = new byte[total];
                                        int offset = 0;
                                        for (final byte[] part : readed) {
                                            final int copied = Integer.min(toread, total - offset);
                                            System.arraycopy(part, 0, labels_bytes, offset, copied);
                                            offset += copied;
                                        }
                                        final String labels_string;
                                        labels_string = new String(labels_bytes, "utf-8");
                                        final StringTokenizer st;
                                        st = new StringTokenizer(labels_string, "\n");
                                        final Set<String> new_labels;
                                        new_labels = new HashSet<>();
                                        while (st.hasMoreElements())
                                            new_labels.add(st.nextToken());
                                        MainActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                labels_adapter.setLabels(new_labels);
                                            }
                                        });
                                    }
                                }
                            } catch (
                                    IOException ioexcp) {
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        status_view.setText(R.string.label_ioerr_message + ioexcp.getMessage());
                                    }
                                });
                            }
                        }
                    }));
                    t.start();
                    t.join();
                } catch (
                        MalformedURLException malurlexcp) {
                    status_view.setText(R.string.badurl_message + malurlexcp.getMessage());
                } catch (
                        InterruptedException interruptedException) {

                }
            }
        });
    }

    private void startVpn() {
        Intent start_intent;
        if (!connected) {
            start_intent = new Intent(this, CaptureService.class);
            final String capture = appcapture_edit.getText().toString().trim();
            if (!capture.isEmpty()) {
                start_intent.putExtra(CaptureService.APP_TO_LISTEN, capture);
            }
            final String site = site_edit.getText().toString();
            start_intent.putExtra(CaptureService.SITE_TO_WRITE, "".equals(site) ? null : site);
            bindService(start_intent, capture_connection, BIND_AUTO_CREATE | BIND_ABOVE_CLIENT);
        }
    }
}