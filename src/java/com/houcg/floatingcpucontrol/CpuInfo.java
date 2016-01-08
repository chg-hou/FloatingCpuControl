package com.houcg.floatingcpucontrol;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


// sudo watch -n1 cat /sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_cur_freq
// watch -n1 "cat /proc/cpuinfo | grep MHz"

public class CpuInfo {

    public static boolean isHPSsupport = checkHPS();
    public static boolean HPS_enable;
    public static int BaseCoreLittle;
    public static int BaseCoreBig;
    public static int mCorenumber = -1;
    public static int CpuUsage_user, CpuUsage_idle;
    private static Long CpuTime_user_1 = 0L;
    private static Long CpuTime_idle_1 = 0L;
    private static Long CpuTime_total_1 = 0L;
    private static Long CpuTime_user_2 = 0L;
    private static Long CpuTime_idle_2 = 0L;
    private static Long CpuTime_total_2 = 0L;
    public ArrayList<CoresInfo> CoresArraylist = new ArrayList<CoresInfo>();
    private Context mContext;


    public CpuInfo(Context iContext) {
        updateCoreNum();
        mContext = iContext;
    }

    public static boolean checkHPS() {
        String mCommandOnline = "[ -d /proc/hps/ ] && echo '1' || echo '0'";
        String msg = ShellUtils.execCommand(mCommandOnline, false).successMsg;
        return msg.equals("1");
    }

    public static void updateCpuTime() {
        CpuTime_user_1 = CpuTime_user_2;
        CpuTime_idle_1 = CpuTime_idle_2;
        CpuTime_total_1 = CpuTime_total_2;
        String[] cpuInfos = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        } catch (Throwable e) {
        }
        CpuTime_total_2 = Long.parseLong(cpuInfos[2])
                + Long.parseLong(cpuInfos[3]) + Long.parseLong(cpuInfos[4])
                + Long.parseLong(cpuInfos[6]) + Long.parseLong(cpuInfos[5])
                + Long.parseLong(cpuInfos[7]) + Long.parseLong(cpuInfos[8]);
        CpuTime_user_2 = Long.parseLong(cpuInfos[2]);
        CpuTime_idle_2 = Long.parseLong(cpuInfos[5]);

        CpuUsage_user = (int) (100L * (CpuTime_user_2 - CpuTime_user_1) / (CpuTime_total_2 - CpuTime_total_1));
        CpuUsage_idle = (int) (100L * (CpuTime_idle_2 - CpuTime_idle_1) / (CpuTime_total_2 - CpuTime_total_1));

    }

    public static int getCoreNum() {
        if (mCorenumber < 0) {
            updateCoreNum();
        }
        return mCorenumber;
    }

    public static void updateCoreNum() {
        String msg = ShellUtils.execCommand("cat /sys/devices/system/cpu/cpu*/online", false).successMsg;
        //Log.d("CpuInfo", "core number: " + msg.split("\n").length);
        mCorenumber = msg.split("\n").length;
    }

    public static int getCoreNum2() {
        String msg = ShellUtils.execCommand("cat /sys/devices/system/cpu/possible", false).successMsg;
        String[] sArray = msg.split("-");
        String corenum = sArray[sArray.length - 1];
        //Log.d("CpuInfo", "cpu info: " + corenum);
        return Integer.valueOf(corenum) + 1;
    }

    private void balanceCoresArraylist(int length) {
        while (CoresArraylist.size() > length) {
            CoresArraylist.remove(CoresArraylist.size() - 1);
        }
        while (CoresArraylist.size() < length) {
            CoresArraylist.add(new CoresInfo());
        }
    }

    public void updateAll() {
        updateState_Online();
        updateState_Cur_freq();
        updateState_Freq_list();
        updateHPS();

        updateCpuTime();

    }

    public void updateState_Online() {
        String mCommandOnline = "cat /sys/devices/system/cpu/cpu*/online";
        String msg = ShellUtils.execCommand(mCommandOnline, false).successMsg;

        String[] sArray = msg.split("\\r?\\n");
        balanceCoresArraylist(sArray.length);

        for (int i = 0; i < sArray.length; i++) {
            CoresArraylist.get(i).online = sArray[i].equals("1");
        }
        //Log.d("CpuInfo", "cpu info (updateState_Online):\n" + msg);
    }

    public void updateState_Cur_freq() {
        String mCommandOnline = "cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq";
        String msg = ShellUtils.execCommand(mCommandOnline, false).successMsg;
        String[] sArray = msg.split("\\r?\\n");
        //Log.d("CpuInfo", "cpu info (updateState_Cur_freq):\n" + msg);
        for (int i = 0; i < sArray.length; i++) {
            CoresArraylist.get(i).scaling_cur_freq = Integer.valueOf(sArray[i]);
        }
    }


    public void updateState_Freq_list() {
        String mCommandOnline = "cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_available_frequencies";
        String msg = ShellUtils.execCommand(mCommandOnline, false).successMsg;
        //Log.d("CpuInfo", "cpu info (updateState_Freq_list):\n" + msg);
        String[] sArray = msg.split("\\r?\\n");


        for (int i = 0; i < sArray.length; i++) {
            String[] sArray_2 = sArray[i].split(" ");
            CoresArraylist.get(i).scaling_available_frequencies = getIntegerArray(Arrays.asList(sArray_2));
            Collections.sort(CoresArraylist.get(i).scaling_available_frequencies);
        }

    }

    public void updateHPS() {
        if (isHPSsupport) {
            String mCommandOnline = "cat /proc/hps/num_base_perf_serv";
            ShellUtils.CommandResult cResult = ShellUtils.execCommand(mCommandOnline, true);

            if (cResult.result != 0) {
                HPS_enable = false;
                return;
            }
            try {
                String msg = cResult.successMsg;
                String[] CoreState = msg.split(" ");
                BaseCoreLittle = Integer.valueOf(CoreState[0]);
                BaseCoreBig = Integer.valueOf(CoreState[1]);

                mCommandOnline = "cat /proc/hps/enabled";
                msg = ShellUtils.execCommand(mCommandOnline, false).successMsg;
                HPS_enable = msg.equals("1");
            } catch (Throwable e) {
                HPS_enable = false;
            }
        }
    }

    private ArrayList<Integer> getIntegerArray(List<String> stringArray) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (String stringValue : stringArray) {
            try {
                result.add(Integer.valueOf(stringValue));
            } catch (NumberFormatException nfe) {
                //System.out.println("Could not parse " + nfe);
                //Log.w("NumberFormat", "Parsing failed! " + stringValue + " can not be an integer");
            }
        }
        return result;
    }


    public ArrayList<Boolean> getCoreState() {
        String msg = ShellUtils.execCommand("cat /sys/devices/system/cpu/cpu*/online", false).successMsg;
        //Log.d("CpuInfo", "cpu online: " + msg);
        ArrayList<Boolean> bArrayList = new ArrayList<Boolean>();
        for (String line : msg.split("\n")) {
            bArrayList.add(line.equalsIgnoreCase("1"));
        }
        return bArrayList;
    }

    /*
        https://www.kernel.org/doc/Documentation/cpu-freq/pcc-cpufreq.txt
            cpuinfo_cur_freq:
        ---------------------
        A) Often cpuinfo_cur_freq will show a value different than what is declared
        in the scaling_available_frequencies or scaling_cur_freq, or scaling_max_freq.
        This is due to "turbo boost" available on recent Intel processors. If certain
        conditions are met the BIOS can achieve a slightly higher speed than requested
        by OSPM. An example:

        scaling_cur_freq	: 2933000
        cpuinfo_cur_freq	: 3196000

        B) There is a round-off error associated with the cpuinfo_cur_freq value.
        Since the driver obtains the current frequency as a "percentage" (%) of the
        nominal frequency from the BIOS, sometimes, the values displayed by
        scaling_cur_freq and cpuinfo_cur_freq may not match. An example:

        scaling_cur_freq	: 1600000
        cpuinfo_cur_freq	: 1583000
     */


    public String readFileData(String fileName) {
        StringBuilder result = new StringBuilder();
        try {
            File file = new File(fileName);
            FileInputStream fis = new FileInputStream(fileName);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String str = new String(data, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }


    public void setCoreOnline(int core, boolean setOnline) {
        String command = "echo";
        if (setOnline) {
            command += " 1 ";
        } else {
            command += " 0 ";
        }
        command += " > /sys/devices/system/cpu/cpu" + core + "/online";
        (new BackgroundRunClass(command, true, mContext)).run();
    }

    public void setCoreMaxfreq(int core, int freqidx) {
        //echo [freq] > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
        String command = "echo ";
        if (freqidx >= 0 && freqidx < CoresArraylist.get(core).scaling_available_frequencies.size()) {
            command += CoresArraylist.get(core).scaling_available_frequencies.get(freqidx);
            command += " > /sys/devices/system/cpu/cpu" + core + "/cpufreq/scaling_max_freq";
            (new BackgroundRunClass(command, true, mContext)).run();
        }
    }

    public void setHPS(boolean enable) {

        String command = "echo ";
        if (enable) {
            command += " 1 ";
        } else {
            command += " 0 ";
        }
        command += " > /proc/hps/enabled ";
        (new BackgroundRunClass(command, true, mContext)).run();
    }

    public void setCoreLittleBig(int little, int big) {
        //echo [# of little] [# of bigs]  > /proc/hps/num_base_perf_serv
        String command = "echo ";
        command += little + " " + big;
        command += " > /proc/hps/num_base_perf_serv";
        (new BackgroundRunClass(command, true, mContext)).run();
    }


    public class CoresInfo {
        //https://www.kernel.org/doc/Documentation/cpu-freq/user-guide.txt
        public Boolean online;
        public Integer scaling_cur_freq;
        public Integer cpuinfo_cur_freq;
        public ArrayList<Integer> scaling_available_frequencies;
        public int scaling_no;

        public CoresInfo() {
            online = false;
            scaling_cur_freq = -1;
            scaling_available_frequencies = new ArrayList<Integer>();
            scaling_no = -1;
        }

        public int getCurFreqIdx() {
            scaling_no = scaling_available_frequencies.indexOf(scaling_cur_freq);
            return scaling_no;
        }
    }

}
