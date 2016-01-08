package com.houcg.floatingcpucontrol;

import android.content.Context;
import android.widget.Toast;


public class BackgroundRunClass extends Thread {
    public static boolean mHasRootPermission = false;
    public static boolean mShow_toast_flag = true;
    private String command;
    private boolean isRoot;
    private Context mContext;

    public BackgroundRunClass(String i_command, boolean i_isRoot, Context iContext) {
        command = i_command;
        isRoot = i_isRoot;
        mContext = iContext;
    }


    @Override
    public void run() {
        if (isRoot) {
            if (!mHasRootPermission) {
                if (ShellUtils.checkRootPermission()) {
                    if (mShow_toast_flag)
                        Toast.makeText(mContext, R.string.root_ok, Toast.LENGTH_SHORT).show();
                    mHasRootPermission = true;
                } else {
                    if (mShow_toast_flag)
                        Toast.makeText(mContext, R.string.root_wrong, Toast.LENGTH_SHORT).show();
                    mHasRootPermission = false;
                    return;
                }
            }
        }
        ShellUtils.execCommand(command, isRoot);
    }

}