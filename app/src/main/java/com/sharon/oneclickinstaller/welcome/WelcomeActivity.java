package com.sharon.oneclickinstaller.welcome;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sharon.oneclickinstaller.MainActivity;
import com.sharon.oneclickinstaller.PrefManager;
import com.sharon.oneclickinstaller.R;


public class WelcomeActivity extends AppCompatActivity {

    public TextView[] dots;
    public LinearLayout dotsLayout;
    public Button btnNext;
    ViewPager.OnPageChangeListener vOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);

            if (position == 3) {
                btnNext.setVisibility(View.GONE);
            } else {
                btnNext.setText(getString(R.string.next));
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
    private ViewPager mViewPager;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_main);

        prefManager = new PrefManager(this);
        if (!prefManager.isFirstTimeLaunch()) {
            launchHomeScreen();
            finish();
        }

        if (Build.VERSION.SDK_INT > 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        dotsLayout = findViewById(R.id.layoutDots);
        btnNext = findViewById(R.id.btn_next);

        changeStatusBarColor();
        addBottomDots(0);

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.view_pager);
        mViewPager.addOnPageChangeListener(vOnPageChangeListener);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int current = getItem(+1);
                if (current < 4) {
                    // move to next screen
                    mViewPager.setCurrentItem(current);
                } else {
                    launchHomeScreen();
                }
            }
        });
    }

    public void launchHomeScreen() {
        prefManager.setFirstTimeLaunch(false);
        startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
        finish();
    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private int getItem(int i) {
        return mViewPager.getCurrentItem() + i;
    }

    private void addBottomDots(int currentPage) {
        dots = new TextView[4];

        int[] colorsActive = getResources().getIntArray(R.array.array_dot_active);
        int[] colorsInActive = getResources().getIntArray(R.array.array_dot_inactive);

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorsInActive[currentPage]);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(colorsActive[currentPage]);
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    // Top Rated fragment activity
                    return new IntroOne();
                case 1:
                    // Games fragment activity
                    return new IntroTwo();
                case 2:
                    // Movies fragment activity
                    return new IntroThree();
                case 3:
                    // Movies fragment activity
                    return new IntroFour();
            }

            return null;
        }

        @Override
        public int getCount() {
            return 4;
        }
    }
}
