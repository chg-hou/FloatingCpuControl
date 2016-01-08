package com.houcg.floatingcpucontrol;

import android.content.Context;

import java.util.Arrays;
import java.util.List;


public class GpuInfo {

    public static boolean isGPUsupport = checkGpuSupported();
    public int gpu_block;
    public int gpu_idle;
    public int gpu_loading;
    public int gpu_freq;
    public boolean gpu_dvfs_enable;
    public List<Integer> GPU_DVFS_FREQ = Arrays.asList(253500, 299000, 396500, 455000, 494000);
    public int GPU_DVFS_FREQ_no = GPU_DVFS_FREQ.size();

    public int gpu_freq_idx;

    private Context mContext;

    public GpuInfo(Context iContext) {
        mContext = iContext;
    }

    public static boolean checkGpuSupported() {
        String mCommandOnline = "[ -d /sys/module/pvrsrvkm/parameters/ ] && echo '1' || echo '0'";
        String msg = ShellUtils.execCommand(mCommandOnline, false).successMsg;
        return msg.equals("1");

    }

    public void updataAll() {
        if (isGPUsupport) {
            updateGPULoad();
            updateGPUFreq();
            updateDVFSstate();
        }
    }

    public void updateGPULoad() {
        String mCommandOnline = "cat /sys/module/pvrsrvkm/parameters/gpu_loading";
        String msg = ShellUtils.execCommand(mCommandOnline, false).successMsg;
        gpu_loading = Integer.valueOf(msg);

        mCommandOnline = "cat /sys/module/pvrsrvkm/parameters/gpu_block";
        msg = ShellUtils.execCommand(mCommandOnline, false).successMsg;
        gpu_block = Integer.valueOf(msg);

        gpu_idle = Math.max(0, 100 - gpu_loading - gpu_block);
        //Log.d("GPUInfo", "gpu load:\n" + gpu_loading + gpu_block + gpu_idle);
    }

    public void updateGPUFreq() {
        String mCommandOnline = "cat /sys/module/pvrsrvkm/parameters/gpu_freq";
        String msg = ShellUtils.execCommand(mCommandOnline, false).successMsg;
        gpu_freq = Integer.valueOf(msg);
        if (GPU_DVFS_FREQ.contains(gpu_freq)) {
            gpu_freq_idx = GPU_DVFS_FREQ.indexOf(gpu_freq) + 1;
        } else {
            gpu_freq_idx = 0;
        }
        //Log.d("GPUInfo", "gpu freq:\n" + gpu_freq);
    }

    public void updateDVFSstate() {
        String mCommandOnline = "cat /sys/module/pvrsrvkm/parameters/gpu_dvfs_enable";
        String msg = ShellUtils.execCommand(mCommandOnline, false).successMsg;
        gpu_dvfs_enable = msg.equals("1");

        //Log.d("GPUInfo", "gpu_dvfs_enable:\n" + gpu_dvfs_enable);
    }

    public void setDVFSstate(boolean enable) {
        String command = "echo ";
        if (enable) {
            command += " 1 ";
        } else {
            command += " 0 ";
        }
        command += " > /sys/module/pvrsrvkm/parameters/gpu_dvfs_enable";
        (new BackgroundRunClass(command, true, mContext)).run();
    }

}
