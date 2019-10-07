package com.sharon.oneclickinstaller.install;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import is.arontibo.library.ElasticDownloadView;

public class NonSuInstallScreen extends AppCompatActivity {

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
        totalSize = InstallerActivity.selectedApps.size();
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", getPackageName()))), 1234);
                } else {
                    callInstallProcess();
                }
            } else {
                callInstallProcess();
            }
        } else {
            InstallerActivity.operationRunning = false;
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

    private void callInstallProcess() {
        for (AppProperties appProperties : selectedApplications) {
            appName = appProperties.getAppname();
            elasticProgress = elasticProgress + (100 / totalSize);
            ++appProgress;
            setValues();
            Uri uri;
            Intent promptInstall = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                promptInstall.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", new File(appProperties.getApkpath()));
            } else {
                uri = Uri.fromFile(new File(appProperties.getApkpath()));
            }
            promptInstall.setDataAndType(uri,
                    "application/vnd.android.package-archive");
            try {
                startActivity(promptInstall);
            } catch (ActivityNotFoundException e) {
                failedApps.add(appName);
            }
        }
        serviceFinished = true;
        setValues();
    }

    private void setValues() {
        if (serviceCancelled) {
            elasticDownloadView.fail();
            progressText.setText("Installation Cancelled");
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
            String text = "Installing:" + appProgress + "/" + totalSize + "->" + appName;
            progressText.setText(text);
            elasticDownloadView.setProgress((float) elasticProgress);
        }
    }

    private void disposeValues() {
        stopButton.setVisibility(View.GONE);
        selectedApplications.clear();
        InstallerActivity.operationRunning = false;
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234 && resultCode == Activity.RESULT_OK) {
            if (getPackageManager().canRequestPackageInstalls()) {
                callInstallProcess();
            }
        } else {
            elasticDownloadView.fail();
            progressText.setText("Permission to install from unknown sources is denied. Enable it from Settings");
            disposeValues();
        }
    }
}
