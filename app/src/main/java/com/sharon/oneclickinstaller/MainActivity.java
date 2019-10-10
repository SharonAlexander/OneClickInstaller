package com.sharon.oneclickinstaller;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.navigation.NavigationView;
import com.sharon.oneclickinstaller.backupuninstall.BackupActivity;
import com.sharon.oneclickinstaller.install.InstallerActivity;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static boolean phoneIsRooted = false;
    public NavigationView navigationView;
    InterstitialAd mInterstitialAd;
    boolean isPremium = false;
    PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefManager = new PrefManager(this);
        CheckPurchase.checkpurchases(getApplicationContext());
        MobileAds.initialize(this, Constants.ads_app_id);
        isPremium = prefManager.getPremiumInfo();
        Log.e("isPremium: ", isPremium + "");
        if (!isPremium) {
            adsInterstitial();
            requestNewInterstitial();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkRoot();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            navigationView.getMenu().performIdentifierAction(R.id.install, 0);
            navigationView.getMenu().getItem(0).setChecked(true);
        }
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
        Fragment frag = getFragmentManager().findFragmentByTag("settings");
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (frag != null) {
            this.getFragmentManager().beginTransaction().replace(R.id.mainFrame, new InstallerActivity()).commit();
            navigationView.getMenu().getItem(0).setChecked(true);
            drawer.closeDrawer(GravityCompat.START);
        } else {
//            CheckPurchase.dispose();
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        Fragment fragment = null;
        if (id == R.id.install) {
            fragment = new InstallerActivity();
        } else if (id == R.id.backup) {
            fragment = new BackupActivity();
        } else if (id == R.id.settings) {
            if (!isPremium) {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }
            fragment = new Settings();
            this.getFragmentManager().beginTransaction().replace(R.id.mainFrame, fragment, "settings").commit();
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        this.getFragmentManager().beginTransaction().replace(R.id.mainFrame, fragment).commit();
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        onCreate(savedInstanceState);
//    }

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
