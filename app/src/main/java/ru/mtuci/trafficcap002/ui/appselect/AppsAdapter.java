package ru.mtuci.trafficcap002.ui.appselect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.mtuci.trafficcap002.R;
import ru.mtuci.trafficcap002.ui.MainActivity;

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.AppHolder> {
    private final Activity activity;
    private final LayoutInflater inflater;
    private final List<ApplicationInfo> apps;

    public AppsAdapter(Activity activity, List<ApplicationInfo> apps) {
        this.activity = activity;
        this.inflater = LayoutInflater.from(activity);
        this.apps = apps;
    }

    @NonNull
    @Override
    public AppsAdapter.AppHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AppHolder(inflater.inflate(R.layout.app_fragment, parent, false), activity);
    }

    @Override
    public void onBindViewHolder(@NonNull AppHolder holder, int position) {
        holder.bind(apps.get(position));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public class AppHolder extends RecyclerView.ViewHolder {
        private final ImageView app_icon;
        private final TextView readable_view, package_view;
        private final Button select_button;
        private final Context context;

        public AppHolder(@NonNull View view, Context context) {
            super(view);
            this.context = context;
            app_icon = view.findViewById(R.id.image_icon);
            readable_view = view.findViewById(R.id.readable_view);
            package_view = view.findViewById(R.id.package_view);
            select_button = view.findViewById(R.id.select_button);
        }

        private void bind(ApplicationInfo app) {
            final PackageManager pm;
            pm = context.getPackageManager();
            try {
                final Resources res;
                res = pm.getResourcesForApplication(app.packageName);
                app_icon.setImageBitmap(BitmapFactory.decodeResource(res, app.icon));
                final String app_name = pm.getApplicationLabel(app).toString();
                readable_view.setText(app_name);
                package_view.setText(app.packageName);
                select_button.setOnClickListener(new View.OnClickListener() {
                                                     @Override
                                                     public void onClick(View view) {
                                                         final Intent result_intent;
                                                         result_intent = new Intent();
                                                         result_intent.putExtra(MainActivity.APP_PACKAGE_KEY, app.packageName);
                                                         result_intent.putExtra(MainActivity.APP_NAME_KEY, app_name);
                                                         AppsAdapter.this.activity.setResult(Activity.RESULT_OK, result_intent);
                                                         AppsAdapter.this.activity.finish();
                                                     }
                                                 }
                );
            } catch (
                    PackageManager.NameNotFoundException nnfexcp) {
                Log.e("Связывание с пакетом", "Нет такого пакета", nnfexcp);
            }
        }
    }
}
