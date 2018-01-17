package com.sharon.oneclickinstaller;

import android.content.Context;
import android.support.v7.app.AlertDialog;

public class Constants {

    public static String ads_app_id = "ca-app-pub-1740451756664908~5774446072";
    public static String ads_interstitial_activity_settings_video = "ca-app-pub-1740451756664908/4490699279";
    public static String ads_interstitial_activity_processing_video = "ca-app-pub-1740451756664908/1002211864";
    public static String ads_interstitial_folder_video = "ca-app-pub-1740451756664908/7251179275";

    public static void showAlertAboutUs(Context context) {
        String version = "5.2";
        new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage("Version:" + version + "\nDeveloped by MadRabbits\u00a9")
                .setPositiveButton(android.R.string.yes, null)
                .setIcon(R.mipmap.ic_launcher)
                .show();
    }

    public static String licensekey() {
        return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiKU4UphDjm82tpzRFVV1chcewlkmomHSZ9U7VGhWAegnEzvxLOk13UhjZzUdxgb9dTI83uYf7ZPl/uPoUuGKX5R10QSFw1NMV8+G2bhlHOrFDNOcGYp2qErW2L9OjBvMVOlWZD8YpQBVj9XhtwHFdMFekUWLTvVPcDd3JVUi8cUGf5xfV828IoN8sB2zFI+FWdLOime1lmRq3JVrKPkEj7+wdS2VAam+g3HYs96kIXVJIw03EgK4mFRibmc0+8xOH7v7TzjKvNMS+fmZnDw7qB27OKrjDV1xrZu2DbrJqIuFtAK8bWRJPZ7/D4h9I1Y/7TQEcM0R0VKEqF5bLNJt3wIDAQAB";
    }

}
