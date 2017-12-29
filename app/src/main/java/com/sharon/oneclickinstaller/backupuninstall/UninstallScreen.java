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
    public static int totalSize;
    public static boolean serviceFinished = false, serviceCancelled = false, stopService = false;
    public static String appName = "";
    public static List<String> failedApps;
    public static List<AppProperties> selectedApplications;
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
            totalSize = BackupActivity.selectedApps.size();
        } catch (NullPointerException e) {
            e.printStackTrace();
            totalSize = 1;
        }
        failedApps = new ArrayList<>();
        selectedApplications = new ArrayList<>(BackupActivity.selectedApps);
        BackupActivity.selectedApps.clear();

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
                stopService = true;
                serviceCancelled = true;
                stopButton.setVisibility(View.GONE);
                UninstallScreen.selectedApplications.clear();
                UninstallScreen.totalSize = 0;
                BackupActivity.operationRunning = false;

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void setValues() {
        if (serviceFinished) {
            elasticDownloadView.success();
            progressText.setText("Uninstalled Succesfully");
            stopButton.setVisibility(View.GONE);
            if (!failedApps.isEmpty())
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
        } else if (serviceCancelled) {
            elasticDownloadView.fail();
            progressText.setText("Un-installation Cancelled");
            stopButton.setVisibility(View.GONE);
            if (!failedApps.isEmpty())
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
        } else {
            if (BackupActivity.operationRunning) {
                String text = "Uninstalling:" + appProgress + "/" + totalSize + "->" + appName;
                progressText.setText(text);
                stopButton.setVisibility(View.VISIBLE);
                elasticDownloadView.setVisibility(View.VISIBLE);
                elasticDownloadView.setProgress((float) elasticProgress);
            } else {
                elasticDownloadView.success();
                stopButton.setVisibility(View.GONE);
                progressText.setText("Process Finished");
            }
        }
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
            if (intent.getBooleanExtra(UninstallerIntentService.PARAM_CANCELLED, false)) {
                elasticDownloadView.fail();
                progressText.setText("Un-installation Cancelled");
                stopButton.setVisibility(View.GONE);
                if (!failedApps.isEmpty())
                    Toast.makeText(context, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
            } else if (intent.getBooleanExtra(UninstallerIntentService.PARAM_FINISHED, false)) {
                elasticDownloadView.success();
                progressText.setText("Uninstalled Succesfully");
                stopButton.setVisibility(View.GONE);
                if (!failedApps.isEmpty())
                    Toast.makeText(context, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
            } else {
                setValues();
            }
        }
    }
}
