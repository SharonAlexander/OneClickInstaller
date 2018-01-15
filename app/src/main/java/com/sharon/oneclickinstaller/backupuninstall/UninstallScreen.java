package com.sharon.oneclickinstaller.backupuninstall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.sharon.oneclickinstaller.AppProperties;
import com.sharon.oneclickinstaller.PrefManager;
import com.sharon.oneclickinstaller.R;

import java.util.ArrayList;
import java.util.List;

import is.arontibo.library.ElasticDownloadView;

public class UninstallScreen extends AppCompatActivity {

    public static int appProgress = 0;
    public static int elasticProgress = 0;
    public static int totalSize = BackupActivity.selectedApps.size();
    public static boolean serviceFinished = false, serviceCancelled = false, stopService = false;
    public static String appName = "";
    public static List<String> failedApps = new ArrayList<>();
    public static List<AppProperties> selectedApplications = new ArrayList<>();
    ElasticDownloadView elasticDownloadView;
    TextView progressText;
    Button stopButton;
    UninstallScreen.ResponseReceiver responseReceiver;
    AdView mAdView;
    private boolean isPremium = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress_screen);

        isPremium = new PrefManager(this).getPremiumInfo();
        mAdView = findViewById(R.id.adView);
        if (!isPremium) {
            adsInitialise();
        } else {
            mAdView.setVisibility(View.GONE);
        }

        try {
            if (!BackupActivity.selectedApps.isEmpty()) {
                selectedApplications.addAll(BackupActivity.selectedApps);
            }
            BackupActivity.selectedApps.clear();
        } catch (NullPointerException e) {
            Toast.makeText(this, "Unknown error on selection. Restart the app!", Toast.LENGTH_SHORT).show();
        }
        elasticDownloadView = findViewById(R.id.elastic_download_view);
        progressText = findViewById(R.id.progress_operation_text);
        stopButton = findViewById(R.id.stopButton);

        elasticDownloadView.startIntro();

        if (!selectedApplications.isEmpty()) {
            Intent msgIntent = new Intent(this, UninstallerIntentService.class);
            startService(msgIntent);
        } else {
            elasticDownloadView.setVisibility(View.GONE);
            stopButton.setVisibility(View.GONE);
            progressText.setText("Unable to show progress");
        }

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(UninstallScreen.this, UninstallerIntentService.class));
                progressText.setText("Stopping...");
                stopButton.setEnabled(false);
                stopService = true;
                serviceCancelled = true;

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setValues();
        IntentFilter filter = new IntentFilter(UninstallScreen.ResponseReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        responseReceiver = new UninstallScreen.ResponseReceiver();
        registerReceiver(responseReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(responseReceiver);
    }


    private void setValues() {
        if (serviceCancelled) {
            elasticDownloadView.fail();
            progressText.setText("Un-installation Cancelled");
            if (!failedApps.isEmpty())
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
            disposeValues();
        } else if (serviceFinished) {
            elasticDownloadView.success();
            progressText.setText("Uninstalled Succesfully");
            if (!failedApps.isEmpty())
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
            disposeValues();
        } else {
            if (BackupActivity.operationRunning) {
                String text = "Uninstalling:" + appProgress + "/" + totalSize + "->" + appName;
                progressText.setText(text);
                stopButton.setVisibility(View.VISIBLE);
                elasticDownloadView.setVisibility(View.VISIBLE);
                elasticDownloadView.setProgress((float) elasticProgress);
            } else {
                elasticDownloadView.success();
                progressText.setText("Process Finished");
                disposeValues();
            }
        }
    }

    private void disposeValues() {
        stopButton.setVisibility(View.GONE);
        UninstallScreen.selectedApplications.clear();
        BackupActivity.operationRunning = false;
        UninstallScreen.failedApps.clear();
        UninstallScreen.appProgress = 0;
        UninstallScreen.elasticProgress = 0;
        UninstallScreen.stopService = false;
        UninstallScreen.serviceFinished = false;
        UninstallScreen.serviceCancelled = false;
    }

    private void adsInitialise() {
        AdRequest bannerAdRequest = new AdRequest.Builder()
                .addTestDevice("D0ACF42C29771A79DA18B6D5E91A43E0")
                .build();
        mAdView.loadAd(bannerAdRequest);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                mAdView.setVisibility(View.VISIBLE);
            }
        });
    }

    public class ResponseReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP = "MESSAGE_PROCESSED";

        @Override
        public void onReceive(Context context, Intent intent) {
//            if (intent.getBooleanExtra(UninstallerIntentService.PARAM_CANCELLED, false)) {
//                elasticDownloadView.fail();
//                progressText.setText("Un-installation Cancelled");
//                stopButton.setVisibility(View.GONE);
//                if (!failedApps.isEmpty())
//                    Toast.makeText(context, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
//            } else if (intent.getBooleanExtra(UninstallerIntentService.PARAM_FINISHED, false)) {
//                elasticDownloadView.success();
//                progressText.setText("Uninstalled Succesfully");
//                stopButton.setVisibility(View.GONE);
//                if (!failedApps.isEmpty())
//                    Toast.makeText(context, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
//            } else {
            setValues();
//            }
        }
    }
}
