package com.sharon.oneclickinstaller.welcome;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import com.sharon.oneclickinstaller.R;

import eu.chainfire.libsuperuser.Shell;

public class IntroTwo extends Fragment implements View.OnClickListener {

    RadioButton radio_not_rooted, radio_rooted, radio_exit;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.welcome_slide2, container, false);
        radio_not_rooted = rootView.findViewById(R.id.radio_not_rooted);
        radio_rooted = rootView.findViewById(R.id.radio_rooted);
        radio_exit = rootView.findViewById(R.id.radio_exit);

        radio_not_rooted.setOnClickListener(this);
        radio_rooted.setOnClickListener(this);
        radio_exit.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        boolean checked = ((RadioButton) v).isChecked();
        switch (v.getId()) {
            case R.id.radio_not_rooted:
                Toast.makeText(getActivity(), R.string.intro2_no_root, Toast.LENGTH_LONG).show();
                break;
            case R.id.radio_rooted:
                if (checked) {
                    Thread rootCheck = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (Shell.SU.available()) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getActivity(), R.string.intro2_root_granted_info, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } else {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            radio_not_rooted.setChecked(true);
                                            radio_rooted.setChecked(false);
                                            Toast.makeText(getActivity(), R.string.intro2_no_root_info, Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        }
                    });
                    rootCheck.start();
                }
                break;
            case R.id.radio_exit:
                System.exit(0);
                break;
        }
    }
}
