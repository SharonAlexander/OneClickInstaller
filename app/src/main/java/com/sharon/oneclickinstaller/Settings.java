package com.sharon.oneclickinstaller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.sharon.oneclickinstaller.util.IabHelper;
import com.sharon.oneclickinstaller.util.IabResult;
import com.sharon.oneclickinstaller.util.Purchase;

import static com.sharon.oneclickinstaller.Constants.ITEM_SKU_SMALL;

public class Settings extends PreferenceFragmentCompat {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result,
                                          Purchase purchase) {
            if (result.getResponse() == 7) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.purchase_success_title))
                        .setMessage(getString(R.string.purchase_success_message))
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
                Toast.makeText(getActivity(), getString(R.string.purchase_success_message2), Toast.LENGTH_SHORT).show();
            }
        }
    };
    private PrefManager prefManager;
    private Preference scandirectory, backupdirectory;
    private InterstitialAd mInterstitialAd;
    private boolean isPremium;
    private IabHelper mHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = new PrefManager(getActivity());
        isPremium = prefManager.getPremiumInfo();
        if (!isPremium) {
            adsInitialise();
            requestNewInterstitial();
        }

        getActivity().setTitle("Settings");
        scandirectory = findPreference("scandir");
        scandirectory.setSummary(prefManager.getScanPref());
        scandirectory.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(getActivity(), FilePickerActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, prefManager.getScanPref());
                startActivityForResult(i, 123);
                return false;
            }
        });

        backupdirectory = findPreference("backupdir");
        backupdirectory.setSummary(prefManager.getStoragePref());
        backupdirectory.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(getActivity(), FilePickerActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, prefManager.getStoragePref());
                startActivityForResult(i, 456);
                return false;
            }
        });

        Preference preferences = findPreference("rateus");
        preferences.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_url)));
                startActivity(intent);
                return false;
            }
        });

        Preference feedback = findPreference("feedback");
        feedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent email = new Intent(Intent.ACTION_SEND);
                email.setType("text/plain");
                email.putExtra(Intent.EXTRA_EMAIL, getString(R.string.email));
                email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
                startActivity(Intent.createChooser(email,
                        getString(R.string.email_chooser_intent)));
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
        if (isPremium) donate.setVisible(false);
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
        Preference purchased = findPreference("donate2");
        if (isPremium) purchased.setVisible(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_layout, rootKey);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123 && resultCode == Activity.RESULT_OK) {
            String path = String.valueOf(data.getData());
            if (path != null) {
                path = path.substring(path.lastIndexOf("root") + 4);
            } else {
                path = prefManager.getScanPref();
            }
            scandirectory.setSummary(path);
            prefManager.putScanPref(path);
            if (!isPremium) {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }
        } else if (requestCode == 456 && resultCode == Activity.RESULT_OK) {
            String path = String.valueOf(data.getData());
            if (path != null) {
                path = path.substring(path.lastIndexOf("root") + 4);
            } else {
                path = prefManager.getStoragePref();
            }
            backupdirectory.setSummary(path);
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
