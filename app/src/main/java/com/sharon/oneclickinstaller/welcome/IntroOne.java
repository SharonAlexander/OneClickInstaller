package com.sharon.oneclickinstaller.welcome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sharon.oneclickinstaller.R;

public class IntroOne extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.welcome_slide1, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        CheckPurchase.checkpurchases(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        CheckPurchase.dispose();
    }
}
