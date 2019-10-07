package com.sharon.oneclickinstaller.backupuninstall;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.sharon.oneclickinstaller.AppProperties;
import com.sharon.oneclickinstaller.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class UninstallerIntentService extends IntentService {

    public static final String PARAM_CANCELLED = "cancelled";
    public static final String PARAM_FINISHED = "finished";
    public static final String PARAM_RUNNING = "running";
    private static final int NOTIFICATION_ID = 890;
    List<String> output = new ArrayList<>();
    Intent broadcastIntent;
    NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public UninstallerIntentService() {
        super("UninstallerIntentService");
        broadcastIntent = new Intent();
        broadcastIntent.setAction(UninstallScreen.ResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        boolean finish = uninstallStart();
        if (finish) {
            if (UninstallScreen.serviceFinished && !UninstallScreen.serviceCancelled) {
                broadcastIntent.putExtra(PARAM_FINISHED, true);
                sendBroadcast(broadcastIntent);
                endNotification();
            }
            if (UninstallScreen.serviceCancelled) {
                broadcastIntent.putExtra(PARAM_CANCELLED, true);
                sendBroadcast(broadcastIntent);
                cancelNotification();
            }
            disposeValues();
        }
        stopSelf();
    }

    private void disposeValues() {
        Log.e("disposeValues: ", "disposing");
        UninstallScreen.selectedApplications.clear();
        BackupActivity.operationRunning = false;
        UninstallScreen.failedApps.clear();
        UninstallScreen.appProgress = 0;
        UninstallScreen.elasticProgress = 0;
        UninstallScreen.stopService = false;
        UninstallScreen.serviceFinished = false;
        UninstallScreen.serviceCancelled = false;
    }

    private boolean uninstallStart() {
        startNotification();
        for (AppProperties appProperties : UninstallScreen.selectedApplications) {
            if (!UninstallScreen.stopService) {
                UninstallScreen.appName = appProperties.getAppname();
                UninstallScreen.elasticProgress = UninstallScreen.elasticProgress + (100 / UninstallScreen.totalSize);
                ++UninstallScreen.appProgress;
                broadcastIntent.putExtra(PARAM_RUNNING, true);
                sendBroadcast(broadcastIntent);
                boolean result = uninstallApk(appProperties);
                if (!result) {
                    UninstallScreen.failedApps.add(appProperties.getAppname());
                }
            } else {
                UninstallScreen.failedApps.add(appProperties.getAppname());
            }
        }
        UninstallScreen.serviceFinished = true;
        return true;
    }

    private boolean uninstallApk(AppProperties appProperties) {
        updateNotification(getAppIcon(appProperties));
        if (Shell.SU.available()) {
            output.clear();
            String command = "pm uninstall " + new File("\"" + appProperties.getPname() + "\"");
            output = Shell.SU.run(command);
            if (output.isEmpty())
                return true;
            if (!output.isEmpty() && output.get(0).equals("Success"))
                return true;
        }
        return false;
    }

    private void startNotification() {
        String id = "uninstall intent";
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(this,id)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .setChannelId(id)
                .setPriority(Notification.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(Color.BLUE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Uninstall Intent";
            String description = "Uninstall Intent Description";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
            mChannel.setDescription(description);
            mNotificationManager.createNotificationChannel(mChannel);
        }


        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, UninstallScreen.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotification(Bitmap bitmap) {
        builder.setProgress(UninstallScreen.totalSize, UninstallScreen.appProgress, false)
                .setContentText(UninstallScreen.appProgress + "/" + UninstallScreen.totalSize)
                .setOngoing(true)
                .setLargeIcon(bitmap)
                .setContentTitle("Uninstalling:" + UninstallScreen.appName);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private Bitmap getAppIcon(AppProperties appProperties) {
        try {
            Drawable drawable = appProperties.getIcon();

            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            } else if (drawable instanceof AdaptiveIconDrawable) {
                Drawable backgroundDr = null;
                Drawable foregroundDr = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    backgroundDr = ((AdaptiveIconDrawable) drawable).getBackground();
                    foregroundDr = ((AdaptiveIconDrawable) drawable).getForeground();
                }

                Drawable[] drr = new Drawable[2];
                drr[0] = backgroundDr;
                drr[1] = foregroundDr;

                LayerDrawable layerDrawable = new LayerDrawable(drr);

                int width = layerDrawable.getIntrinsicWidth();
                int height = layerDrawable.getIntrinsicHeight();

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmap);

                layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                layerDrawable.draw(canvas);

                return bitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void endNotification() {
        builder.setContentTitle("Uninstalled Successfully")
                .setProgress(0, 0, false)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(Color.WHITE);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void cancelNotification() {
        builder.setContentTitle("Backup cancelled")
                .setProgress(0, 0, false)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(Color.WHITE);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

}
