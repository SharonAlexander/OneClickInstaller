package com.sharon.oneclickinstaller.backupuninstall;

import android.content.Intent;
import android.net.Uri;
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

public class NonSuUninstallScreen extends AppCompatActivity {
    public int appProgress = 0, elasticProgress = 0, totalSize;
    public boolean serviceFinished = false, serviceCancelled = false;
    public String appName = "";
    public List<String> failedApps = new ArrayList<>();
    public List<AppProperties> selectedApplications = new ArrayList<>();
    ElasticDownloadView elasticDownloadView;
    TextView progressText;
    Button stopButton;
    AdView mAdView;
    private boolean isPremium = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress_screen);

        isPremium = new PrefManager(this).getPremiumInfo();
        mAdView = findViewById(R.id.adView);
        if (!isPremium) {
            adsInitialise();
        } else {
            mAdView.setVisibility(View.GONE);
        }
        totalSize = BackupActivity.selectedApps.size();
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
            callUnInstallProcess();
        } else {
            BackupActivity.operationRunning = false;
            elasticDownloadView.setVisibility(View.GONE);
            stopButton.setVisibility(View.GONE);
            progressText.setText("ERROR!");
        }

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serviceCancelled = true;
                progressText.setText("Stopping...");
                stopButton.setEnabled(false);
            }
        });
    }

    private void callUnInstallProcess() {
        for (AppProperties appProperties : selectedApplications) {
            appName = appProperties.getAppname();
            elasticProgress = elasticProgress + (100 / totalSize);
            ++appProgress;
            setValues();
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + appProperties.getPname()));
            startActivity(intent);
        }
        serviceFinished = true;
        setValues();
    }

    private void setValues() {
        if (serviceCancelled) {
            elasticDownloadView.fail();
            progressText.setText("Uninstallation Cancelled");
            if (!failedApps.isEmpty()) {
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_SHORT).show();
            }
            disposeValues();
        } else if (serviceFinished) {
            elasticDownloadView.success();
            progressText.setText("Finished Succesfully");
            if (!failedApps.isEmpty()) {
                Toast.makeText(this, "Failed Apps:" + failedApps.toString(), Toast.LENGTH_SHORT).show();
            }
            disposeValues();
        } else {
            String text = "Uninstalling:" + appProgress + "/" + totalSize + "->" + appName;
            progressText.setText(text);
            elasticDownloadView.setProgress((float) elasticProgress);
        }
    }

    private void disposeValues() {
        stopButton.setVisibility(View.GONE);
        selectedApplications.clear();
        BackupActivity.operationRunning = false;
        failedApps.clear();
        appProgress = 0;
        elasticProgress = 0;
        serviceFinished = false;
        serviceCancelled = false;
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
}
