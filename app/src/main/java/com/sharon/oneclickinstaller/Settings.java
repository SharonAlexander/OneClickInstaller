package com.sharon.oneclickinstaller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.sharon.oneclickinstaller.util.IabHelper;
import com.sharon.oneclickinstaller.util.IabResult;
import com.sharon.oneclickinstaller.util.Purchase;

public class Settings extends PreferenceFragment {

    static final String ITEM_SKU_SMALL = "com.sharon.donate_small";

    //    static final String ITEM_SKU_SMALL = "android.test.purchased";
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
    PrefManager prefManager;
    Preference directory;
    InterstitialAd mInterstitialAd;
    boolean isPremium;
    IabHelper mHelper;
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result,
                                          Purchase purchase) {
            if (result.getResponse() == 7) {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Wow!!")
                        .setMessage("You are awesome! You already purchased this item")
                        .setPositiveButton(android.R.string.ok, null)
                        .setIcon(R.mipmap.ic_launcher)
                        .show();
            } else if (result.isFailure()) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.purchase_error)
                        .setMessage(R.string.purchase_already_owned)
                        .setPositiveButton(android.R.string.ok, null)
                        .setIcon(R.mipmap.ic_launcher)
                        .show();
            } else if (purchase.getSku().equals(ITEM_SKU_SMALL)) {
                Toast.makeText(getActivity(), "Thanks for Purchasing!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_layout);

        prefManager = new PrefManager(getActivity());
        isPremium = prefManager.getPremiumInfo();
        if (!isPremium) {
            adsInitialise();
            requestNewInterstitial();
        }

        getActivity().setTitle("Settings");
        directory = findPreference("filepick");
        directory.setSummary(prefManager.getStoragePref());
        directory.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(getActivity(), FilePickerActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, prefManager.getStoragePref());
                startActivityForResult(i, 123);
                return false;
            }
        });

        Preference preferences = findPreference("rateus");
        preferences.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.sharon.oneclickinstaller"));
                startActivity(intent);
                return false;
            }
        });
        Preference about = findPreference("about");
        about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Constants.showAlertAboutUs(getActivity());
                return false;
            }
        });
        Preference donate = findPreference("donate");
        donate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (mHelper != null) mHelper.flagEndAsync();
                try {
                    mHelper.launchPurchaseFlow(getActivity(), ITEM_SKU_SMALL, 10001,
                            mPurchaseFinishedListener, "donateSmallPurchase");
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123 && resultCode == Activity.RESULT_OK) {
            String path = String.valueOf(data.getData());
            if (path != null) {
                path = path.substring(path.lastIndexOf("root") + 4);
            } else {
                path = prefManager.getStoragePref();
            }
            directory.setSummary(path);
            prefManager.putStoragePref(path);
            if (!isPremium) {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        String base64EncodedPublicKey = Constants.licensekey();
        mHelper = new IabHelper(getActivity(), base64EncodedPublicKey);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                } else {
                }
            }
        });
    }

    private void adsInitialise() {
        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId(Constants.ads_interstitial_folder_video);
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }
        });
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mInterstitialAd.loadAd(adRequest);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHelper != null) try {
            mHelper.dispose();
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
        mHelper = null;
    }
}
