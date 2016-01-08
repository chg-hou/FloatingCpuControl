package com.houcg.floatingcpucontrol;

import android.app.Application;
import android.view.WindowManager;

public class FloatingApplication extends Application {

    private WindowManager.LayoutParams mWindowManagerLayoutParams = new WindowManager.LayoutParams();

    public WindowManager.LayoutParams getWindowManagerLayoutParams() {
        return mWindowManagerLayoutParams;
    }
}
