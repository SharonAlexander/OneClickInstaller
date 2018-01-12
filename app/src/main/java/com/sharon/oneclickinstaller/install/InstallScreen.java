package com.sharon.oneclickinstaller.install;

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

public class InstallScreen extends AppCompatActivity {

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
    InstallScreen.ResponseReceiver responseReceiver;
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
            totalSize = InstallerActivity.selectedApps.size();
        } catch (NullPointerException e) {
            e.printStackTrace();
            totalSize = 1;
        }
        try {
            failedApps = new ArrayList<>();
            selectedApplications = new ArrayList<>();
            selectedApplications.addAll(InstallerActivity.selectedApps);
        } catch (NullPointerException e) {
            Toast.makeText(this, "Unknown error on selection. Restart the app!", Toast.LENGTH_SHORT).show();
        }
        InstallerActivity.selectedApps.clear();
        elasticDownloadView = findViewById(R.id.elastic_download_view);
        progressText = findViewById(R.id.progress_operation_text);
        stopButton = findViewById(R.id.stopButton);

        elasticDownloadView.startIntro();

        if (!selectedApplications.isEmpty()) {
            Intent msgIntent = new Intent(this, InstallIntentService.class);
            startService(msgIntent);
        } else {
            elasticDownloadView.setVisibility(View.GONE);
            stopButton.setVisibility(View.GONE);
            progressText.setText("Unable to show progress!");
        }

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(InstallScreen.this, InstallIntentService.class));
                stopService = true;
                serviceCancelled = true;
                stopButton.setVisibility(View.GONE);
                InstallScreen.selectedApplications.clear();
                InstallScreen.totalSize = 0;
                InstallerActivity.operationRunning = false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setValues();
        IntentFilter filter = new IntentFilter(InstallScreen.ResponseReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        responseReceiver = new InstallScreen.ResponseReceiver();
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
            progressText.setText("Finished Succesfully");
            stopButton.setVisibility(View.GONE);
            if (!failedApps.isEmpty())
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
        } else if (serviceCancelled) {
            elasticDownloadView.fail();
            progressText.setText("Installation Cancelled");
            stopButton.setVisibility(View.GONE);
            if (!failedApps.isEmpty())
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
        } else {
            if (InstallerActivity.operationRunning) {
                String text = "Installing:" + appProgress + "/" + totalSize + "->" + appName;
                progressText.setText(text);
                elasticDownloadView.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.VISIBLE);
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
            if (intent.getBooleanExtra(InstallIntentService.PARAM_CANCELLED, false)) {
                elasticDownloadView.fail();
                progressText.setText("Installation Cancelled");
                stopButton.setVisibility(View.GONE);
                if (!failedApps.isEmpty())
                    Toast.makeText(context, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
            } else if (intent.getBooleanExtra(InstallIntentService.PARAM_FINISHED, false)) {
                elasticDownloadView.success();
                progressText.setText("Finished Succesfully");
                stopButton.setVisibility(View.GONE);
                if (!failedApps.isEmpty())
                    Toast.makeText(context, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
            } else {
                setValues();
            }
        }
    }
}
