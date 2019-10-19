package com.sharon.oneclickinstaller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

public class PrefManager {

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    @SuppressLint("CommitPrefEdits")
    public PrefManager(Context context) {
        this.preferences = context.getSharedPreferences("com.sharon.oneclickinstaller", Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public boolean isFirstTimeLaunch() {
        return preferences.getBoolean("first", true);
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean("first", isFirstTime);
        editor.commit();
    }

    public boolean getPremiumInfo() {
        return preferences.getBoolean("premium", false);
    }

    public void putPremiumInfo(boolean bool) {
        editor.putBoolean("premium", bool);
        editor.commit();
    }

    public String getStoragePref() {
        return preferences.getString("storage", Environment.getExternalStorageDirectory().getPath());
    }

    public void putStoragePref(String value) {
        editor.putString("storage", value);
        editor.commit();
    }

    public String getScanPref() {
        return preferences.getString("scan", Environment.getExternalStorageDirectory().getPath());
    }

    public void putScanPref(String value) {
        editor.putString("scan", value);
        editor.commit();
    }

    public void putTreeUri(Uri uri) {
        editor.putString("treeuri", uri.toString());
        editor.commit();
    }

    public Uri getTreeUri() {
        return Uri.parse(preferences.getString("treeuri", "nouri"));
    }
}
