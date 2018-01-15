package com.sharon.oneclickinstaller.install;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.ISelectionListener;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.listeners.OnClickListener;
import com.mikepenz.fastadapter.listeners.OnLongClickListener;
import com.mikepenz.fastadapter_extensions.ActionModeHelper;
import com.sharon.oneclickinstaller.AppProperties;
import com.sharon.oneclickinstaller.Constants;
import com.sharon.oneclickinstaller.MainActivity;
import com.sharon.oneclickinstaller.PrefManager;
import com.sharon.oneclickinstaller.R;
import com.sharon.oneclickinstaller.Settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class InstallerActivity extends Fragment implements EasyPermissions.PermissionCallbacks {

    public static List<AppProperties> selectedApps;
    public static boolean operationRunning = false;
    private final int READ_PERMISSION_CALLBACK_CONSTANT = 1111;
    private final int WRITE_PERMISSION_CALLBACK_CONSTANT = 2222;
    PackageManager packageManager;
    PackageInfo pi;
    AppProperties app;
    FastItemAdapter<AppProperties> fastAdapter;
    ProgressBar progressBar;
    private RecyclerView recyclerView;
    private Button change_directory;
    private List<AppProperties> appList;
    private ActionModeHelper actionModeHelper;
    private PrefManager prefManager;
    private InterstitialAd mInterstitialAd;
    private AdRequest bannerAdRequest;
    private AdView mAdView;
    private boolean isPremium = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appList = new ArrayList<>();
        selectedApps = new ArrayList<>();
        prefManager = new PrefManager(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_install, container, false);

        isPremium = prefManager.getPremiumInfo();
        mAdView = view.findViewById(R.id.adListViewbanner);
        if (!isPremium) {
            adsInitialise();
            requestNewInterstitial();
        } else {
            mAdView.setVisibility(View.GONE);
        }

        getActivity().setTitle(getActivity().getString(R.string.installer_title));
        packageManager = getActivity().getPackageManager();
        recyclerView = view.findViewById(R.id.installer_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        fastAdapter = new FastItemAdapter<>();
        fastAdapter.setHasStableIds(true);
        fastAdapter.withSelectable(true);
        fastAdapter.withMultiSelect(true);
        fastAdapter.withSelectOnLongClick(true);

        actionModeHelper = new ActionModeHelper(fastAdapter, R.menu.menu_action_mode, new ActionModeCallBack())
                .withTitleProvider(new ActionModeHelper.ActionModeTitleProvider() {
                    @Override
                    public String getTitle(int selected) {
                        return selected + "/" + fastAdapter.getAdapterItemCount();
                    }
                });
        recyclerView.setAdapter(fastAdapter);

        progressBar = view.findViewById(R.id.progressBar);
        readAllApks();


        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!operationRunning) {
                    if (selectedApps.size() > 0) {
                        operationRunning = true;
                        if (!isPremium && mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        } else {
                            startFab();
                        }
                    } else {
                        Toast.makeText(getActivity(), "No apps selected\nLong click to start selection", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), "Another operation is running", Toast.LENGTH_SHORT).show();
                }
            }
        });
        change_directory = view.findViewById(R.id.change_directory_button);
        change_directory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction().replace(R.id.mainFrame, new Settings(), "settings").commit();
            }
        });

        fastAdapter.withOnClickListener(new OnClickListener<AppProperties>() {
            @Override
            public boolean onClick(@NonNull View v, @NonNull IAdapter<AppProperties> adapter, @NonNull AppProperties item, int position) {
                if (!actionModeHelper.isActive()) {
                    singleAppDetails(item);
                }
                return true;
            }
        });
        fastAdapter.withOnPreClickListener(new OnClickListener<AppProperties>() {
            @Override
            public boolean onClick(@NonNull View v, @NonNull IAdapter<AppProperties> adapter, @NonNull AppProperties item, int position) {
                Boolean res = actionModeHelper.onClick(item);
                return res != null ? res : false;
            }
        });
        fastAdapter.withOnPreLongClickListener(new OnLongClickListener<AppProperties>() {
            @Override
            public boolean onLongClick(@NonNull View v, @NonNull IAdapter<AppProperties> adapter, @NonNull AppProperties item, int position) {
                ActionMode actionMode = actionModeHelper.onLongClick((AppCompatActivity) getActivity(), position);
                if (actionMode != null) {
                    getActivity().findViewById(R.id.action_mode_bar).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                }
                return actionMode != null;
            }
        });
        fastAdapter.withSelectionListener(new ISelectionListener<AppProperties>() {
            @Override
            public void onSelectionChanged(@Nullable AppProperties item, boolean selected) {
                if (getActivity() != null) {
                    if (selected) {
                        selectedApps.add(item);
                    } else {
                        selectedApps.remove(item);
                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
//        progressBar.setVisibility(View.VISIBLE);
//        selectedApps.clear();
//        fastAdapter.clear();
//        appList.clear();
        if (actionModeHelper.isActive())
            actionModeHelper.reset();
//        readAllApks();
    }

    private void singleAppDetails(AppProperties item) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(item.getIcon());
        String message = "<b>Version</b><br>" + item.getVersionname() + "<br><br><b>Package Name</b><br>" + item.getPname() + "<br><br><b>Size</b><br>" + item.getApksize() + "<br><br><b>Path</b><br>" + item.getApkpath();
        builder.setTitle(item.getAppname())
                .setMessage(Html.fromHtml(message))
                .setPositiveButton(android.R.string.ok, null);
        builder.create();
        builder.show();
    }

    @AfterPermissionGranted(READ_PERMISSION_CALLBACK_CONSTANT)
    private void readAllApks() {
        if (EasyPermissions.hasPermissions(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Thread appThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        appList = getAllApks(prefManager.getStoragePref());
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!appList.isEmpty()) {
                                    fastAdapter.add(appList);
                                    Toast.makeText(getActivity(), "LongClick to enable Multi-Selection", Toast.LENGTH_LONG).show();
                                } else {
                                    change_directory.setVisibility(View.VISIBLE);
                                }
                                progressBar.setVisibility(View.GONE);
                                progressBar.setEnabled(false);
                            }
                        });
                    }
                }
            });
            appThread.start();
        } else {
            EasyPermissions.requestPermissions(this, "Give Read Permission",
                    READ_PERMISSION_CALLBACK_CONSTANT, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private List<AppProperties> getAllApks(String pathToScan) throws NullPointerException {
        File file = new File(pathToScan);
        File fileLists[] = file.listFiles();
        if (fileLists != null && fileLists.length > 0) {
            for (File filename : fileLists) {
                if (filename.isDirectory()) {
                    getAllApks(filename.getPath());
                } else {
                    if (filename.getName().endsWith(".apk")) {
                        pi = packageManager.getPackageArchiveInfo(filename.getAbsolutePath(), 0);
                        if (pi == null)
                            continue;
                        pi.applicationInfo.sourceDir = filename.getAbsolutePath();
                        pi.applicationInfo.publicSourceDir = filename.getAbsolutePath();

                        app = new AppProperties();
                        app.setAppname((String) pi.applicationInfo.loadLabel(packageManager));
                        app.setPname(pi.packageName);
                        app.setApkpath(filename.getAbsolutePath());
                        app.setIcon(pi.applicationInfo.loadIcon(packageManager));
                        app.setApksize((filename.length() / (1024 * 1024)) + "MB");
                        app.setVersionname(pi.versionName);
                        app.setVersionCode(pi.versionCode);
                        app.setInstaller(true);
                        if (null != packageManager.getLaunchIntentForPackage(pi.applicationInfo.packageName)) {
                            app.setAlready_installedorbackedup(true);
                        } else {
                            app.setAlready_installedorbackedup(false);
                        }
                        appList.add(app);
                    }
                }
            }
        }
        Collections.sort(appList, new Comparator<AppProperties>() {
            @Override
            public int compare(AppProperties v1, AppProperties v2) {
                int x = -1;
                try {
                    x = v1.getAppname().toLowerCase().compareTo(v2.getAppname().toLowerCase());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return x;
            }
        });
        return appList;
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        progressBar.setEnabled(false);
        progressBar.setVisibility(View.GONE);
        //button use can be changed to grant permission again
        change_directory.setVisibility(View.VISIBLE);
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            readAllApks();
        }
    }

    @AfterPermissionGranted(WRITE_PERMISSION_CALLBACK_CONSTANT)
    private void deleteFiles() {
        if (EasyPermissions.hasPermissions(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            boolean deletesuccess = false;
            for (AppProperties i : selectedApps) {
                deletesuccess = deleteApk(i);
            }
            if (deletesuccess) {
                fastAdapter.deleteAllSelectedItems();
                actionModeHelper.reset();
                selectedApps.clear();
                if (fastAdapter.getItemCount() == 0)
                    change_directory.setVisibility(View.VISIBLE);
            } else
                Toast.makeText(getActivity(), "Problem with deleting file", Toast.LENGTH_SHORT).show();
        } else {
            EasyPermissions.requestPermissions(this, "Give Write Permission",
                    WRITE_PERMISSION_CALLBACK_CONSTANT, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    boolean deleteApk(AppProperties temp) {
        boolean success;
        File file = new File(temp.getApkpath());
        if (temp.getApkpath().contains(Environment.getExternalStorageDirectory().toString())) {
            success = file.delete();
        } else {
            success = deleteUriApk(temp);
        }
        if (!success) {
            if (Shell.SU.available()) {
                String command = "rm " + "\"" + file + "\"";
                Shell.SU.run(command);
                success = true;
            }
        }
        return success;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        packageManager = null;
        pi = null;
        prefManager = null;
        fastAdapter = null;
        recyclerView = null;
        app = null;
        appList = null;
        progressBar = null;
        change_directory = null;
        actionModeHelper = null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    boolean deleteUriApk(AppProperties temp) {
        boolean success = false;
        String folder = prefManager.getStoragePref();
        folder = folder.substring(folder.lastIndexOf("/") + 1);
        String filename = temp.getApkpath().substring(temp.getApkpath().lastIndexOf("/") + 1);
        DocumentFile pickedDir;
        if (!prefManager.getTreeUri().equals(Uri.parse("nouri"))) {
            try {
                pickedDir = DocumentFile.fromTreeUri(getActivity(), prefManager.getTreeUri());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
        try {
            pickedDir = pickedDir.findFile(folder);
            pickedDir = pickedDir.findFile(filename);
            success = DocumentsContract.deleteDocument(getActivity().getContentResolver(), pickedDir.getUri());
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return success;
    }

    private void adsInitialise() {
        bannerAdRequest = new AdRequest.Builder()
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
        //interstitial
        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId(Constants.ads_interstitial_activity_processing_video);
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                startFab();
            }
        });
    }

    public void startFab() {
        if (MainActivity.phoneIsRooted) {
            InstallScreen.totalSize = selectedApps.size();
            startActivity(new Intent(getActivity(), InstallScreen.class));
        } else {
            startActivity(new Intent(getActivity(), NonSuInstallScreen.class));
        }
    }

    private void requestNewInterstitial() {
        AdRequest interstitialAdRequest = new AdRequest.Builder()
                .addTestDevice("D0ACF42C29771A79DA18B6D5E91A43E0")
                .build();
        mInterstitialAd.loadAd(interstitialAdRequest);
    }

    private class ActionModeCallBack implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    if (!operationRunning) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle("Delete apps?")
                                .setMessage("Are you sure you want to delete the selected apps?")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        deleteFiles();
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    } else {
                        Toast.makeText(getActivity(), "Another operation is running", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case R.id.action_select_all:
                    selectedApps.clear();
                    for (int i = 0; i < fastAdapter.getItemCount(); i++) {
                        fastAdapter.select(i);
                    }
                    mode.setTitle(String.valueOf(fastAdapter.getItemCount()));
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }
    }
}
