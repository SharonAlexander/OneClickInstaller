package com.sharon.oneclickinstaller.welcome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nononsenseapps.filepicker.FilePickerActivity;
import com.sharon.oneclickinstaller.MainActivity;
import com.sharon.oneclickinstaller.PrefManager;
import com.sharon.oneclickinstaller.R;

public class IntroFour extends Fragment {

    PrefManager prefManager;
    CheckBox checkBoxEntire, checkBoxDefault;
    private Button folderSelector, btnStart;
    private TextView selectedPath, selectedPathHeading;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.welcome_slide4, container, false);
        folderSelector = view.findViewById(R.id.folder_selector);
        checkBoxEntire = view.findViewById(R.id.checkBoxEntire);
        checkBoxDefault = view.findViewById(R.id.checkBoxDefault);
        btnStart = view.findViewById(R.id.welcome_start);
        selectedPath = view.findViewById(R.id.selected_path);
        selectedPathHeading = view.findViewById(R.id.selected_path_heading);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        prefManager = new PrefManager(getActivity());
        checkBoxEntire.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkBoxDefault.setChecked(false);
                    folderSelector.setEnabled(false);
                    selectedPath.setText(Environment.getExternalStorageDirectory().getPath());
                    selectedPath.setVisibility(View.VISIBLE);
                    selectedPathHeading.setVisibility(View.VISIBLE);
                    btnStart.setVisibility(View.VISIBLE);
                    prefManager.putStoragePref(Environment.getExternalStorageDirectory().getPath());
                } else {
                    folderSelector.setEnabled(true);
                }
            }
        });
        checkBoxDefault.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String path = Environment.getExternalStorageDirectory().getPath() + "/" + getString(R.string.app_folder_name);
                if (isChecked) {
                    checkBoxEntire.setChecked(false);
                    folderSelector.setEnabled(false);
                    selectedPath.setText(path);
                    selectedPath.setVisibility(View.VISIBLE);
                    selectedPathHeading.setVisibility(View.VISIBLE);
                    btnStart.setVisibility(View.VISIBLE);
                    prefManager.putStoragePref(path);
                } else {
                    folderSelector.setEnabled(true);
                }
            }
        });
        folderSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), FilePickerActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
                startActivityForResult(i, 123);
            }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefManager prefManager = new PrefManager(getActivity());
                prefManager.setFirstTimeLaunch(false);
                startActivity(new Intent(getActivity(), MainActivity.class));
                getActivity().finish();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123 && resultCode == Activity.RESULT_OK) {
            String path = data.getData().toString();
            path = path.substring(path.lastIndexOf("root") + 4);
            selectedPath.setText(path);
            selectedPath.setVisibility(View.VISIBLE);
            selectedPathHeading.setVisibility(View.VISIBLE);
            btnStart.setVisibility(View.VISIBLE);
            prefManager.putStoragePref(path);
        }
    }
}
