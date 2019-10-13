package com.sharon.oneclickinstaller.welcome;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import com.sharon.oneclickinstaller.PrefManager;
import com.sharon.oneclickinstaller.R;

import java.io.File;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class IntroThree extends Fragment implements EasyPermissions.PermissionCallbacks {

    private static final int WRITE_PERMISSION_CALLBACK_CONSTANT = 101;
    private static final int DOCUMENT_TREE_CONSTANT = 22;
    public TextView txtPermissions, headingText;
    Button btnCheckPermissions,btnSDPermission;
    private View view;
    PrefManager prefManager;

    public IntroThree() {
    }

    public static boolean hasRemovableSdCard(Context context) {
        return ContextCompat.getExternalFilesDirs(context, null).length >= 2;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.welcome_slide3, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (null != view) {
            txtPermissions = view.findViewById(R.id.permissions_info);
            headingText = view.findViewById(R.id.text_slide3_heading);
            btnCheckPermissions = view.findViewById(R.id.grant_permissions);
            btnSDPermission = view.findViewById(R.id.grant_sd_permissions);
            btnCheckPermissions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getReadPermission();
                }
            });
            btnSDPermission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getSDCardPermission();
                }
            });
        }
    }

    private void getSDCardPermission() {
        if (hasRemovableSdCard(getActivity())) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity()).
                            setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @SuppressLint("InlinedApi")
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), DOCUMENT_TREE_CONSTANT);
                                    } catch (ActivityNotFoundException e) {
                                        e.printStackTrace();
                                        createDefaultDirectoryInInternal();
                                    }

                                    dialog.dismiss();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    btnSDPermission.setVisibility(View.VISIBLE);
                                }
                            })
                            .setView(R.layout.sd_card_access_dialog);
            builder.create().show();
        }
    }

    @AfterPermissionGranted(WRITE_PERMISSION_CALLBACK_CONSTANT)
    private void getReadPermission() {
        if (EasyPermissions.hasPermissions(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            proceedAfterReadPermission();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_storage),
                    WRITE_PERMISSION_CALLBACK_CONSTANT, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void proceedAfterReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSDCardPermission();
        } else {
            createDefaultDirectoryInInternal();
        }
        btnCheckPermissions.setVisibility(View.GONE);
        if (hasRemovableSdCard(getActivity())) {
            txtPermissions.setText(R.string.one_more_permission);
        } else {
            txtPermissions.setText(R.string.permission_granted);
        }
        txtPermissions.setVisibility(View.VISIBLE);
    }

    private void createDefaultDirectoryInInternal() {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + getString(R.string.app_folder_name));
        file.mkdir();
        btnCheckPermissions.setVisibility(View.GONE);
        txtPermissions.setText(R.string.permission_granted);
        txtPermissions.setVisibility(View.VISIBLE);
    }

    @TargetApi(19)
    private void getStorageAccessFramework(Intent data) {
        Uri treeUri = data.getData();
        DocumentFile pickedDir = DocumentFile.fromTreeUri(getActivity(), treeUri);
        getActivity().grantUriPermission(getActivity().getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getActivity().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        prefManager = new PrefManager(getActivity());
        prefManager.putTreeUri(treeUri);
        if (pickedDir.findFile(getString(R.string.app_folder_name)) == null) {
            pickedDir.createDirectory(getString(R.string.app_folder_name));
        }
        txtPermissions.setVisibility(View.GONE);
        headingText.setText(getString(R.string.permission_granted));
        startAction();
    }

    private void startAction() {
        String scanPath = Environment.getExternalStorageDirectory().getPath();
        prefManager.putScanPref(scanPath);
        prefManager.putStoragePref(scanPath);
        prefManager.setFirstTimeLaunch(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DOCUMENT_TREE_CONSTANT) {
            if (resultCode == RESULT_OK) {
                getStorageAccessFramework(data);
                btnSDPermission.setVisibility(View.GONE);
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getActivity(), R.string.sd_card_permission_error, Toast.LENGTH_SHORT).show();
                createDefaultDirectoryInInternal();
                btnSDPermission.setVisibility(View.VISIBLE);
            }
        }
    }
}
