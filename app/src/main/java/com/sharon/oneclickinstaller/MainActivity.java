package com.sharon.oneclickinstaller;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.sharon.oneclickinstaller.backupuninstall.BackupActivity;
import com.sharon.oneclickinstaller.install.InstallerActivity;

import java.util.Arrays;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "SharonMainActivity";
    public static boolean phoneIsRooted = false;
    public NavigationView navigationView;
    InterstitialAd mInterstitialAd;
    boolean isPremium = false;
    PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fireBaseStart();
        prefManager = new PrefManager(this);
        CheckPurchase.checkpurchases(getApplicationContext());
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                RequestConfiguration requestConfiguration = new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("A3CDB132F7DEB4BC0EA8308B33DB1903")).build();
                MobileAds.setRequestConfiguration(requestConfiguration);
                isPremium = prefManager.getPremiumInfo();
                Log.d(TAG, "isPremium: " + isPremium);
                if (!isPremium) {
                    adsInterstitial();
                    requestNewInterstitial();
                }
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkRoot();
        makeSettingsChange();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            navigationView.getMenu().performIdentifierAction(R.id.backup, 1);
            navigationView.getMenu().getItem(1).setChecked(true);
        }
    }

    private void makeSettingsChange() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            prefManager.putStoragePref(Environment.getExternalStorageDirectory().getPath() + "/Android/data/com.sharon.oneclickinstaller");
        }
    }

    private void fireBaseStart() {
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }
                        String token = task.getResult().getToken();
                        Log.d(TAG, token);
                    }
                });
    }

    private void checkRoot() {
        Thread rootCheck = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Shell.SU.available()) {
                    phoneIsRooted = true;
                }
            }
        });
        rootCheck.start();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        Fragment frag = getSupportFragmentManager().findFragmentByTag("settings");
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (frag != null) {
            this.getSupportFragmentManager().beginTransaction().replace(R.id.mainFrame, new BackupActivity()).commit();
            navigationView.getMenu().getItem(1).setChecked(true);
            drawer.closeDrawer(GravityCompat.START);
        } else {
//            CheckPurchase.dispose();
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        Fragment fragment = new BackupActivity();
        if (id == R.id.install) {
            fragment = new InstallerActivity();
        } else if (id == R.id.backup) {
            fragment = new BackupActivity();
        } else if (id == R.id.settings) {
            if (!isPremium) {
                Log.d(TAG, "onNavigationItemSelected: " + mInterstitialAd);
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }
            fragment = new Settings();
            this.getSupportFragmentManager().beginTransaction().replace(R.id.mainFrame, fragment, "settings").commit();
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        this.getSupportFragmentManager().beginTransaction().replace(R.id.mainFrame, fragment).commit();
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CheckPurchase.dispose();
    }

    private void adsInterstitial() {
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(Constants.ads_interstitial_activity_settings_video);
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }
        });
    }

    private void requestNewInterstitial() {
        AdRequest interstitialAdRequest = new AdRequest.Builder()
                .build();
        mInterstitialAd.loadAd(interstitialAdRequest);
    }

}
