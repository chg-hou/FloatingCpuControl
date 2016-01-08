package com.houcg.floatingcpucontrol;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;

import xml.SettingsFragment;

//extends Activity
//extends PreferenceActivity
public class FloatingActivity extends PreferenceActivity {

    public static boolean activityVisible;
    private FloatingView mFloatingView = null;


    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFloatingView = new FloatingView(this);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

        mFloatingView.show();
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        if (null != mFloatingView) {
            mFloatingView.release();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityPaused();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            this.moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}