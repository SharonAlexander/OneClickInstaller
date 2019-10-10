package com.sharon.oneclickinstaller.install;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

    public static int totalSize = InstallerActivity.selectedApps.size();
    public static int appProgress = 0;
    public static int elasticProgress = 0;
    public static boolean serviceFinished = false, serviceCancelled = false, stopService = false;
    public static String appName = "";
    public static List<String> failedApps = new ArrayList<>();
    public static List<AppProperties> selectedApplications = new ArrayList<>();
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
            if (!InstallerActivity.selectedApps.isEmpty()) {
                selectedApplications.addAll(InstallerActivity.selectedApps);
            }
            InstallerActivity.selectedApps.clear();
        } catch (NullPointerException e) {
            Toast.makeText(this, "Unknown error on selection. Restart the app!", Toast.LENGTH_SHORT).show();
        }
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

    private void setValues() {
        if (serviceCancelled) {
            elasticDownloadView.fail();
            progressText.setText("Installation Cancelled");
            if (!failedApps.isEmpty())
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
            disposeValues();
        } else if (serviceFinished) {
            elasticDownloadView.success();
            progressText.setText("Finished Succesfully");
            if (!failedApps.isEmpty())
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
            disposeValues();
        } else {
            if (InstallerActivity.operationRunning) {
                String text = "Installing:" + appProgress + "/" + totalSize + "->" + appName;
                progressText.setText(text);
                elasticDownloadView.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.VISIBLE);
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
        InstallScreen.selectedApplications.clear();
        InstallerActivity.operationRunning = false;
        InstallScreen.failedApps.clear();
        InstallScreen.appProgress = 0;
        InstallScreen.elasticProgress = 0;
        InstallScreen.stopService = false;
        InstallScreen.serviceFinished = false;
        InstallScreen.serviceCancelled = false;
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
//            if (intent.getBooleanExtra(InstallIntentService.PARAM_CANCELLED, false)) {
//                elasticDownloadView.fail();
//                progressText.setText("Installation Cancelled");
//                stopButton.setVisibility(View.GONE);
//                if (!failedApps.isEmpty())
//                    Toast.makeText(context, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
//            } else if (intent.getBooleanExtra(InstallIntentService.PARAM_FINISHED, false)) {
//                elasticDownloadView.success();
//                progressText.setText("Finished Succesfully");
//                stopButton.setVisibility(View.GONE);
//                if (!failedApps.isEmpty())
//                    Toast.makeText(context, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_LONG).show();
//            } else {
            setValues();
//            }
        }
    }
}
