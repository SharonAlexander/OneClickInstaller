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

public class BackupScreen extends AppCompatActivity {

    public static int appProgress = 0;
    public static int elasticProgress = 0;
    public static int totalSize = BackupActivity.selectedApps.size();
    public static boolean serviceFinished = false, serviceCancelled = false, stopService = false;
    public static String appName = "";
    public static List<String> failedApps = new ArrayList<>();
    public static List<AppProperties> selectedApplications = new ArrayList<>();
    public static String destinationPath;
    ElasticDownloadView elasticDownloadView;
    TextView progressText;
    Button stopButton;
    BackupScreen.ResponseReceiver responseReceiver;
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
            if (!BackupActivity.selectedApps.isEmpty()) {//only enter this first and once
                //that's why operation running is true and enter.. exception present and not corrected
                selectedApplications.addAll(BackupActivity.selectedApps);
            }
            BackupActivity.selectedApps.clear();
        } catch (NullPointerException e) {
            Toast.makeText(this, "Unknown error on selection. Restart the app!", Toast.LENGTH_SHORT).show();
        }

        PrefManager prefManager = new PrefManager(this);
        destinationPath = prefManager.getStoragePref();

        elasticDownloadView = findViewById(R.id.elastic_download_view);
        progressText = findViewById(R.id.progress_operation_text);
        stopButton = findViewById(R.id.stopButton);

        elasticDownloadView.startIntro();

        if (!selectedApplications.isEmpty()) {
            Intent msgIntent = new Intent(this, BackupIntentService.class);
            startService(msgIntent);
        } else {
            elasticDownloadView.setVisibility(View.GONE);
            stopButton.setVisibility(View.GONE);
            progressText.setText("Unable to show progress");
        }

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(BackupScreen.this, BackupIntentService.class));
                stopService = true;
                serviceCancelled = true;
                progressText.setText("Stopping...");
                stopButton.setEnabled(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setValues();
        IntentFilter filter = new IntentFilter(BackupScreen.ResponseReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        responseReceiver = new BackupScreen.ResponseReceiver();
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
            progressText.setText("Backup Cancelled");
            if (!failedApps.isEmpty()) {
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_SHORT).show();
            }
            disposeValues();
        } else if (serviceFinished) {
            elasticDownloadView.success();
            progressText.setText("Backedup Succesfully");
            if (!failedApps.isEmpty()) {
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_SHORT).show();
            }
            disposeValues();
        } else {
            if (BackupActivity.operationRunning) {
                String text = "Backing up:" + appProgress + "/" + totalSize + "->" + appName;
                progressText.setText(text);
                stopButton.setVisibility(View.VISIBLE);
                elasticDownloadView.setVisibility(View.VISIBLE);
                elasticDownloadView.setProgress((float) elasticProgress);
            } else {
                elasticDownloadView.success();
                disposeValues();
                progressText.setText("Process Finished");
            }
        }
    }

    private void disposeValues() {
        stopButton.setVisibility(View.GONE);
        BackupScreen.selectedApplications.clear();
        BackupActivity.operationRunning = false;
        BackupScreen.failedApps.clear();
        BackupScreen.appProgress = 0;
        BackupScreen.elasticProgress = 0;
        BackupScreen.stopService = false;
        BackupScreen.serviceFinished = false;
        BackupScreen.serviceCancelled = false;
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
//            if (intent.getBooleanExtra(BackupIntentService.PARAM_CANCELLED, false)) {
//                elasticDownloadView.fail();
//                progressText.setText("Backup Cancelled");
//                stopButton.setVisibility(View.GONE);
//                if (!failedApps.isEmpty()) {
//                    Toast.makeText(context, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
//                }
//            } else if (intent.getBooleanExtra(BackupIntentService.PARAM_FINISHED, false)) {
//                elasticDownloadView.success();
//                progressText.setText("Backedup Succesfully");
//                stopButton.setVisibility(View.GONE);
//                if (!failedApps.isEmpty()) {
//                    Toast.makeText(context, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
//                }
//            } else {
                setValues();
//            }
        }
    }
}
