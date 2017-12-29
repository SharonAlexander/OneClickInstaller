package com.sharon.oneclickinstaller.install;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.sharon.oneclickinstaller.AppProperties;
import com.sharon.oneclickinstaller.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class InstallIntentService extends IntentService {

    public static final String PARAM_CANCELLED = "cancelled";
    public static final String PARAM_FINISHED = "finished";
    public static final String PARAM_RUNNING = "running";
    private static final int NOTIFICATION_ID = 678;
    List<String> output = new ArrayList<>();
    Intent broadcastIntent;
    NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public InstallIntentService() {
        super("InstallerIntentService");
        broadcastIntent = new Intent();
        broadcastIntent.setAction(InstallScreen.ResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        boolean finish = installStart();
        if (finish) {
            if (InstallScreen.serviceFinished && !InstallScreen.serviceCancelled) {
                broadcastIntent.putExtra(PARAM_FINISHED, true);
                sendBroadcast(broadcastIntent);
                endNotification();
            }
            if (InstallScreen.serviceCancelled) {
                broadcastIntent.putExtra(PARAM_CANCELLED, true);
                sendBroadcast(broadcastIntent);
                cancelNotification();
            }
            InstallScreen.appProgress = 0;
            InstallScreen.failedApps.clear();
            InstallScreen.selectedApplications.clear();
            InstallerActivity.operationRunning = false;
            InstallScreen.appName = "";
            InstallScreen.elasticProgress = 0;
            InstallScreen.totalSize = 0;
            InstallScreen.stopService = false;
            InstallScreen.serviceFinished = false;
            InstallScreen.serviceCancelled = false;
        }
        stopSelf();
    }

    private boolean installStart() {
        startNotification();
        for (AppProperties appProperties : InstallScreen.selectedApplications) {
            if (!InstallScreen.stopService) {
                InstallScreen.appName = appProperties.getAppname();
                InstallScreen.elasticProgress = InstallScreen.elasticProgress + (100 / InstallScreen.totalSize);
                ++InstallScreen.appProgress;
                broadcastIntent.putExtra(PARAM_RUNNING, true);
                sendBroadcast(broadcastIntent);
                boolean result = installApk(appProperties);
                if (!result) {
                    InstallScreen.failedApps.add(appProperties.getAppname());
                }
            } else {
                InstallScreen.failedApps.add(appProperties.getAppname());
            }
        }
        InstallScreen.serviceFinished = true;
        return true;
    }

    private boolean installApk(AppProperties appProperties) {
        updateNotification(appProperties.getIcon());
        if (Shell.SU.available()) {
            output.clear();
            String command = "pm install " + new File("\"" + appProperties.getApkpath() + "\"");
            output = Shell.SU.run(command);
            if (output.isEmpty())
                return true;
            else if (!output.isEmpty() && output.get(0).equals("Success"))
                return true;
        }
        return false;
    }

    private void startNotification() {
        String id = "installer_intent";
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(this, id)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .setChannelId(id)
                .setPriority(Notification.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(Color.BLUE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Installer Intent";
            String description = "Installer Intent Description";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
            mChannel.setDescription(description);
            mNotificationManager.createNotificationChannel(mChannel);
        }


        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, InstallScreen.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotification(Drawable icon) {
        builder.setProgress(InstallScreen.totalSize, InstallScreen.appProgress, false)
                .setContentText(InstallScreen.appProgress + "/" + InstallScreen.totalSize)
                .setOngoing(true)
                .setLargeIcon(((BitmapDrawable) icon).getBitmap())
                .setContentTitle("Installing:" + InstallScreen.appName);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void endNotification() {
        builder.setContentTitle("Installed Successfully")
                .setProgress(0, 0, false)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(Color.WHITE);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void cancelNotification() {
        builder.setContentTitle("Installation cancelled")
                .setProgress(0, 0, false)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(Color.WHITE);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
