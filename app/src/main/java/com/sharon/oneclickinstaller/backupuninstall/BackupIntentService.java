package com.sharon.oneclickinstaller.backupuninstall;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.provider.DocumentFile;

import com.sharon.oneclickinstaller.AppProperties;
import com.sharon.oneclickinstaller.PrefManager;
import com.sharon.oneclickinstaller.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class BackupIntentService extends IntentService {

    public static final String PARAM_CANCELLED = "cancelled";
    public static final String PARAM_FINISHED = "finished";
    public static final String PARAM_RUNNING = "running";
    private static final int NOTIFICATION_ID = 789;
    List<String> output = new ArrayList<>();
    Intent broadcastIntent;
    NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    PrefManager prefManager;

    public BackupIntentService() {
        super("BackupIntentService");
        broadcastIntent = new Intent();
        broadcastIntent.setAction(BackupScreen.ResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefManager = new PrefManager(BackupIntentService.this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        boolean finish = backupStart();
        if (finish) {
            if (BackupScreen.serviceFinished && !BackupScreen.serviceCancelled) {
                broadcastIntent.putExtra(PARAM_FINISHED, true);
                sendBroadcast(broadcastIntent);
                endNotification();
            }
            if (BackupScreen.serviceCancelled) {
                broadcastIntent.putExtra(PARAM_CANCELLED, true);
                sendBroadcast(broadcastIntent);
                cancelNotification();
            }
            disposeValues();
        }
        stopSelf();
    }

    private void disposeValues() {
        BackupScreen.selectedApplications.clear();
        BackupActivity.operationRunning = false;
        BackupScreen.failedApps.clear();
        BackupScreen.appProgress = 0;
        BackupScreen.elasticProgress = 0;
        BackupScreen.stopService = false;
        BackupScreen.serviceFinished = false;
        BackupScreen.serviceCancelled = false;
    }

    private boolean backupStart() {
        startNotification();
        for (AppProperties appProperties : BackupScreen.selectedApplications) {
            if (!BackupScreen.stopService) {
                BackupScreen.appName = appProperties.getAppname();
                BackupScreen.elasticProgress = BackupScreen.elasticProgress + (100 / BackupScreen.totalSize);
                ++BackupScreen.appProgress;
                broadcastIntent.putExtra(PARAM_RUNNING, true);
                sendBroadcast(broadcastIntent);
                boolean result = backupApk(appProperties);
                if (!result) {
                    BackupScreen.failedApps.add(appProperties.getAppname());
                }
            } else {
                BackupScreen.failedApps.add(appProperties.getAppname());
            }
        }
        BackupScreen.serviceFinished = true;
        return true;
    }

    private boolean backupApk(AppProperties appProperties) {
        updateNotification(getAppIcon(appProperties));
        if (Shell.SU.available()) {
            output.clear();
            String command = "cp " + "\"" + appProperties.getApkpath() + "\" " + "\"" + BackupScreen.destinationPath + "/\" ";
            output = Shell.SU.run(command);
            String rename = "mv " + "\"" + BackupScreen.destinationPath + "/base.apk" + "\" " + "\"" + BackupScreen.destinationPath + "/" + appProperties.getAppname() + ".apk\"";
            List<String> renameoutput = Shell.SU.run(rename);
            if (output.isEmpty())
                return true;
        } else {
            try {
                getPackageManager().getPackageInfo(appProperties.getPname(), 0);// to check existence i guess i forgot
                nonSUBackup(appProperties);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    private void nonSUBackup(AppProperties appProperties) {
        Intent mainIntent;
        mainIntent = getPackageManager()
                .getLaunchIntentForPackage(appProperties.getPname());
        if (mainIntent != null) {
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        }
        final List pkgAppsList = getPackageManager()
                .queryIntentActivities(mainIntent, 0);
        for (Object object : pkgAppsList) {
            ResolveInfo info = (ResolveInfo) object;
            File f1 = new File(
                    info.activityInfo.applicationInfo.publicSourceDir);

            try {
                PackageInfo pi = null;
                try {
                    pi = getPackageManager().getPackageInfo(appProperties.getPname(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                String file_name = (String) getPackageManager().getApplicationLabel(pi.applicationInfo);
                InputStream in = new FileInputStream(f1);
                OutputStream out = checkStoragePath(file_name, pi);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.flush();
                out.close();
            } catch (Exception e) {
            }
        }
    }

    private OutputStream checkStoragePath(String file_name, PackageInfo pi) throws FileNotFoundException {
        OutputStream out = null;
        String path = prefManager.getStoragePref();
        if (path.contains(Environment.getExternalStorageDirectory().toString())) {
            File f2;
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED))
                f2 = new File(path);
            else
                f2 = getCacheDir();
            if (!f2.exists())
                f2.mkdirs();
            f2 = new File(f2.getPath() + "/" + file_name + ".apk");
            try {
                f2.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            out = new FileOutputStream(f2);
        } else {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, prefManager.getTreeUri());
            int pos = path.indexOf("/");
            int n = 3;
            while (--n > 0 && pos != -1)
                pos = path.indexOf("/", pos + 1);
            path = path.substring(pos + 1);
//            path = path.replaceFirst("/storage/", "");
//            path = path.substring(path.indexOf("/") + 1);
            String[] folders = path.split("/");
            DocumentFile temp = pickedDir;
            for (String folder : folders) {
                temp = temp.findFile(folder);
                if (temp == null) {
                    temp.createDirectory(folder);
                    temp = temp.findFile(folder);
                }
            }
            pickedDir = temp;
            if (pickedDir.findFile(file_name + ".apk") != null) {
                pickedDir = pickedDir.findFile(file_name + ".apk");
                pickedDir.delete();
            }
            pickedDir = temp;
            pickedDir = pickedDir.createFile("application", file_name + ".apk");
            out = getContentResolver().openOutputStream(pickedDir.getUri());
        }
        return out;
    }

    private void startNotification() {
        String id = "backup intent";
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(this, id)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .setChannelId(id)
                .setPriority(Notification.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(Color.BLUE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Backup Intent";
            String description = "Backup Intent Description";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
            mChannel.setDescription(description);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, BackupScreen.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotification(Bitmap bitmap) {
        builder.setProgress(BackupScreen.totalSize, BackupScreen.appProgress, false)
                .setContentText(BackupScreen.appProgress + "/" + BackupScreen.totalSize)
                .setOngoing(true)
                .setLargeIcon(bitmap)
                .setContentTitle("Backing up:" + BackupScreen.appName);
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
        builder.setContentTitle("Backedup Successfully")
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
