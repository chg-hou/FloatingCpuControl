# FloatingCpuControl
Android app with floating window to control cpu


This app requires ROOT permissions to change settings.
By now, this app is only tested under Meizu MX4. It MAY work in other devices.

## What settings can it change?


Active cores manually
> - /proc/hps/enabled   
> - /sys/devices/system/cpu/cpu*/online   


Minimum numbers of online big and little  cores
> - /proc/hps/num_base_perf_serv 


 Maximum scaling frequency 
> - /sys/devices/system/cpu/cpu*/cpufreq/scaling_max_freq


Disable GPU dvfs

>  - /sys/module/pvrsrvkm/parameters/gpu_dvfs_enable



**Refs:** 

 - [https://lists.launchpad.net/ubuntu-phone/msg14982.html](https://lists.launchpad.net/ubuntu-phone/msg14982.html)
 - [https://www.kernel.org/doc/Documentation/cpu-freq/pcc-cpufreq.txt](https://www.kernel.org/doc/Documentation/cpu-freq/pcc-cpufreq.txt)

## How to use

1. Use radio buttons to switch between *Auto* and  *Manual* modes.
2. Use spinners to set the minimum numbers of online big and little  cores.
3. Use square bars to limit the maximum scaling frequency. 
4. Use checkboxes to enable or disable cores. (in  *Manual* mode)
5. Click to close this app.
6. Click to minimize the floating window.
7. Touch and hold on the blank area to open/close the preference screen. Touch and drag to move the window.


> **CPU load bar:**    user / system(including IO) / idle
> **GPU load bar:**    loading / block / idle


