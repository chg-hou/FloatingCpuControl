package com.houcg.floatingcpucontrol;

//ref: https://github.com/mikewang0326/FloatingViewDemo

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class FloatingView extends LinearLayout implements OnClickListener, View.OnLongClickListener,
        AdapterView.OnItemSelectedListener, RadioGroup.OnCheckedChangeListener,
        RatingBar.OnRatingBarChangeListener, CompoundButton.OnCheckedChangeListener,
        OnSharedPreferenceChangeListener {
    private static final String TAG = "FloatingView";

    private static final int MOVE_DISTANCE_MIN = 10;
    private static int minfoFloatLayout_width = -1;
    private static int minfoFloatLayout_height = -1;
    private final Object mRefresh_UI_lock = new Object();
    //...................
    public CpuInfo mCpuInfo = new CpuInfo(getContext());
    public GpuInfo mGpuInfo = new GpuInfo(getContext());
    public int scale_ratio;
    private float mTouchStartX;
    private float mTouchStartY;
    private float mRawX;
    private float mRawY;
    private float mRawStartX = 0;
    private float mRawStartY = 0;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowLayoutParams;
    private int mLastWindowHeight = 0;
    private View mContentView = null;
    private boolean mIsShowing = false;
    private int mScreenHeight;
    private int mStatusBarHeight;
    private RunningStateUpdaterTask mUpdateTask = null;
    private FloatingActivity mMainActivity = null;
    private SharedPreferences mSettings = null;
    private volatile int mUpdatePeriod = 500;
    private boolean mShow_toast_flag = true;
    //..................
    private boolean isFloatLayoutShown = true;
    Runnable updateUIrunable = new Runnable() {
        @Override
        public void run() {
            if (isFloatLayoutShown) {
                for (int i = 0; i < CpuInfo.getCoreNum(); i++) {
                    RatingBar tmpRatingBar = (RatingBar) mContentView.findViewById(100 * i + 2 + 1000);
                    TextView tmpTextFreq = (TextView) mContentView.findViewById(100 * i + 3 + 1000);
                    //ToggleButton tmpToggleButton =  (ToggleButton) mContentView.findViewById(100 * i + 11 + 1000);
                    CheckBox tempCheckbox = (CheckBox) mContentView.findViewById(100 * i + 1 + 1000);

                    //TextView tmpTextFreq2 = (TextView) mContentView.findViewById(100 * i + 4 + 1000);

                    if (CpuInfo.isHPSsupport) {
                        RadioButton radioButtonAuto = (RadioButton) findViewById(R.id.radioButtonAuto);
                        RadioButton radioButtonManual = (RadioButton) findViewById(R.id.radioButtonManual);
                        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.governorRadioGroup);
                        if (CpuInfo.HPS_enable) {
                            if (radioButtonManual.isChecked()) {
                                radioGroup.setTag(R.id.radioButtonAuto);
                                radioButtonAuto.setChecked(true);
                            }
                            Spinner core_spinner_little = (Spinner) findViewById(R.id.core_spinner_little);
                            Spinner core_spinner_big = (Spinner) findViewById(R.id.core_spinner_big);
                            core_spinner_little.setTag(CpuInfo.BaseCoreLittle);
                            core_spinner_little.setSelection(CpuInfo.BaseCoreLittle, true);
                            core_spinner_big.setTag(CpuInfo.BaseCoreBig);
                            core_spinner_big.setSelection(CpuInfo.BaseCoreBig, true);
                        } else {
                            if (radioButtonAuto.isChecked()) {
                                radioGroup.setTag(R.id.radioButtonManual);
                                radioButtonManual.setChecked(true);
                            }
                        }
                    } else {
                        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.governorRadioGroup);
                        radioGroup.setVisibility(RadioGroup.INVISIBLE);
                        LinearLayout manualGovernorLayout = (LinearLayout) mContentView.findViewById(R.id.ManualGovernorLayout);
                        manualGovernorLayout.setVisibility(LinearLayout.GONE);

                    }
                    if (mCpuInfo.CoresArraylist.get(i).online) {
                        //tmpToggleButton.setChecked(true);
                        tempCheckbox.setTag(true);
                        tempCheckbox.setChecked(true);

                        //Log.d(TAG, "Freq Idx = " + mCpuInfo.CoresArraylist.get(i).getCurFreqIdxStr());
                        tmpRatingBar.setNumStars(mCpuInfo.CoresArraylist.get(i).scaling_available_frequencies.size());
                        tmpRatingBar.setStepSize(1f);
                        tmpRatingBar.setRating(mCpuInfo.CoresArraylist.get(i).getCurFreqIdx() + 1f);

                        //Log.d(TAG, "======================freq num:\n" + mCpuInfo.CoresArraylist.get(i).getCurFreqIdx());
                        tmpTextFreq.setText(mCpuInfo.CoresArraylist.get(i).scaling_cur_freq / 1000 + " MHz");
                    } else {
                        //tmpToggleButton.setChecked(false);
                        tempCheckbox.setTag(false);
                        tempCheckbox.setChecked(false);
                        tmpRatingBar.setRating((float) 0.0);
                        tmpTextFreq.setText(getResources().getString(R.string.float_windows_cpu_offline));
                        //tmpTextFreq2.setText("");
                    }
                }
                TextView cpuLoadText = (TextView) mContentView.findViewById(R.id.cpuLoadText);
                cpuLoadText.setText(CpuInfo.CpuUsage_user + "/" + (100 - CpuInfo.CpuUsage_idle - CpuInfo.CpuUsage_user) + "/" + CpuInfo.CpuUsage_idle);

                ProgressBar cpuLoadBar = (ProgressBar) mContentView.findViewById(R.id.cpuLoadBar);
                cpuLoadBar.setProgress(CpuInfo.CpuUsage_user);
                cpuLoadBar.setSecondaryProgress(100 - CpuInfo.CpuUsage_idle);

                LinearLayout gpuLoadLayout = (LinearLayout) mContentView.findViewById(R.id.gpuLoadLayout);
                if (GpuInfo.isGPUsupport) {
                    gpuLoadLayout.setVisibility(LinearLayout.VISIBLE);

                    TextView gpuLoadText = (TextView) mContentView.findViewById(R.id.gpuLoadText);
                    gpuLoadText.setText(mGpuInfo.gpu_loading + "/" + mGpuInfo.gpu_block + "/" + mGpuInfo.gpu_idle);

                    ProgressBar gpuLoadBar = (ProgressBar) mContentView.findViewById(R.id.gpuLoadBar);
                    gpuLoadBar.setProgress(mGpuInfo.gpu_loading);
                    gpuLoadBar.setSecondaryProgress(mGpuInfo.gpu_loading + mGpuInfo.gpu_block);

                    CheckBox gpuDVFScheckBox = (CheckBox) mContentView.findViewById(R.id.gpuDVFScheckBox);
                    gpuDVFScheckBox.setTag(mGpuInfo.gpu_dvfs_enable);
                    gpuDVFScheckBox.setChecked(mGpuInfo.gpu_dvfs_enable);

                    RatingBar gpuFreqRatingBar = (RatingBar) mContentView.findViewById(R.id.gpuFreqRatingBar);
                    gpuFreqRatingBar.setNumStars(mGpuInfo.GPU_DVFS_FREQ_no);
                    gpuFreqRatingBar.setRating(mGpuInfo.gpu_freq_idx);

                    TextView gpuFreqText = (TextView) mContentView.findViewById(R.id.gpuFreqText);
                    gpuFreqText.setText(mGpuInfo.gpu_freq / 1000 + " MHz");
                } else {
                    gpuLoadLayout.setVisibility(LinearLayout.GONE);
                }
            }
            refreshLayout();
        }
    };

    public FloatingView(Context context) {
        super(context);
        init();
        mMainActivity = (FloatingActivity) context;
    }

    public FloatingView(FloatingActivity activity) {
        super(activity);
        init();
        mMainActivity = activity;
    }

    public FloatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void RescaleLayout(LinearLayout iLayout, float scale) {
        if (scale < 0.05 || scale > 1) {
            scale = 1.0f;
        }
        LayoutParams params = (LayoutParams) (iLayout.getLayoutParams());
        minfoFloatLayout_width = iLayout.getMeasuredWidth();
        minfoFloatLayout_height = iLayout.getMeasuredHeight();

        int width = minfoFloatLayout_width;
        int height = minfoFloatLayout_height;
        iLayout.setPivotX(0f);
        iLayout.setPivotY(0f);
        iLayout.setScaleX(scale);
        iLayout.setScaleY(scale);
        width = (int) (width * (scale - 1));
        height = (int) (height * (scale - 1));
        params.setMargins(0, 0, width, height);
        //params.width = width;       params.height = height;
        iLayout.setLayoutParams(params);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //Log.i("Preference", key);
        switch (key) {
            case "clickable":
                SettingChanged_Clickable(true);
                break;
            case "show_gpu":
                SettingChanged_Show_gpu(true);
                break;
            case "show_toast":
                SettingChanged_Show_toast();
                break;
            case "show_when_locked":
                SettingChanged_Show_when_locked(true);
                break;
            case "Transparence":
                SettingChanged_Alpha(true);
                break;
            case "scale_ratio":
                SettingChanged_Scale_ratio(true);
                break;
            case "refresh_rate":
                SettingChanged_Refresh_rate(true);
                break;

        }
    }

    public void Setting_load() {
        SettingChanged_Clickable(false);
        SettingChanged_Show_when_locked(false);
        SettingChanged_Alpha(false);
        SettingChanged_Show_gpu(false);
        SettingChanged_Show_toast();
        SettingChanged_Refresh_rate(false);
        SettingChanged_Position(false);

        SettingChanged_Scale_ratio(false);
    }

    public void SettingChanged_Clickable(boolean update_flag) {
        //mWindowLayoutParams = ((FloatingApplication) getContext().getApplicationContext()).getWindowManagerLayoutParams();
        boolean clickable = mSettings.getBoolean("clickable", true);
        if (clickable) {
            mWindowLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        } else {
            mWindowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        if (update_flag)
            mWindowManager.updateViewLayout(this, mWindowLayoutParams);
    }

    public void SettingChanged_Show_gpu(boolean update_flag) {
        //mWindowLayoutParams = ((FloatingApplication) getContext().getApplicationContext()).getWindowManagerLayoutParams();
        boolean show_gpu = mSettings.getBoolean("show_gpu", true);
        if (show_gpu) {
            mGpuInfo.isGPUsupport = mGpuInfo.checkGpuSupported();
        } else {
            mGpuInfo.isGPUsupport = false;
        }
        if (update_flag)
            refreshUI_unlock();

    }

    public void SettingChanged_Show_toast() {
        //mWindowLayoutParams = ((FloatingApplication) getContext().getApplicationContext()).getWindowManagerLayoutParams();
        mShow_toast_flag = mSettings.getBoolean("show_toast", true);
        BackgroundRunClass.mShow_toast_flag = mShow_toast_flag;
    }

    public void SettingChanged_Show_when_locked(boolean update_flag) {

//        boolean clickable = mSettings.getBoolean("show_when_locked", true);
//        if (clickable) {
//            mWindowLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
//        } else {
//            mWindowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
//        }
//        mWindowManager.updateViewLayout(this, mWindowLayoutParams);
    }

    public void SettingChanged_Alpha(boolean update_flag) {
        int alpha = mSettings.getInt("Transparence", 80);
        mWindowLayoutParams.alpha = alpha / 100.0f;
        if (update_flag)
            mWindowManager.updateViewLayout(this, mWindowLayoutParams);
    }

    public void SettingChanged_Scale_ratio(boolean update_flag) {
        scale_ratio = mSettings.getInt("scale_ratio", 100);
        if (update_flag)
            RescaleLayout((LinearLayout) findViewById(R.id.infoFloatLayout), scale_ratio / 100.0f);
    }

    public void SettingChanged_Refresh_rate(boolean update_flag) {
        mUpdatePeriod = Integer.valueOf(mSettings.getString("refresh_rate", "1000"));
        if (update_flag)
            refreshUI_unlock();
    }

    public void SettingChanged_Position(boolean update_flag) {
        //mWindowLayoutParams = ((FloatingApplication) getContext() .getApplicationContext()).getWindowManagerLayoutParams();
        mWindowLayoutParams.x = mSettings.getInt("position_x", 0);
        mWindowLayoutParams.y = mSettings.getInt("position_y", 0);
        if (update_flag)
            mWindowManager.updateViewLayout(this, mWindowLayoutParams);
    }

    private void init() {
        mSettings = PreferenceManager.getDefaultSharedPreferences(getContext());

        initScreenWidthHeihgt();
        prepareForAddView();


        Spinner core_spinner_little = (Spinner) findViewById(R.id.core_spinner_little);
        Spinner core_spinner_big = (Spinner) findViewById(R.id.core_spinner_big);

        core_spinner_little.setSelection(1, true);
        core_spinner_little.setOnItemSelectedListener(this);

        core_spinner_big.setSelection(1, true);
        core_spinner_big.setOnItemSelectedListener(this);


        RadioGroup governorRadioGroup = (RadioGroup) findViewById(R.id.governorRadioGroup);
        governorRadioGroup.check(R.id.radioButtonAuto);
        governorRadioGroup.setOnCheckedChangeListener(this);


        for (int i = 0; i < CpuInfo.getCoreNum(); i++) {
            CheckBox tempCheckbox = (CheckBox) mContentView.findViewById(100 * i + 1 + 1000);
            tempCheckbox.setOnCheckedChangeListener(this);
            tempCheckbox.setOnClickListener(this);
            RatingBar tmpRatingBar = (RatingBar) mContentView.findViewById(100 * i + 2 + 1000);
            tmpRatingBar.setOnRatingBarChangeListener(this);
        }
        CheckBox gpuDVFScheckBox = (CheckBox) mContentView.findViewById(R.id.gpuDVFScheckBox);
        gpuDVFScheckBox.setOnCheckedChangeListener(this);


        Setting_load();
        mSettings.registerOnSharedPreferenceChangeListener(this);
        mUpdateTask = new RunningStateUpdaterTask();
        mUpdateTask.execute();

    }

    /**
     *
     */
    @SuppressWarnings("deprecation")
    private void prepareForAddView() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mContentView = inflater.inflate(R.layout.float_window, null);
        mContentView.setOnClickListener(this);


        ImageButton mImgBtnMinimize = (ImageButton) mContentView.findViewById(R.id.button_minimize);
        ImageButton mImgBtnClose = (ImageButton) mContentView.findViewById(R.id.button_close);


        LinearLayout infoFloatLayout = (LinearLayout) mContentView.findViewById(R.id.infoFloatLayout);
        LinearLayout cpuInfoLayout = (LinearLayout) mContentView.findViewById(R.id.cpuInfoLayout);


        //-------------------------

        for (int i = 0; i < CpuInfo.getCoreNum(); i++) {

            LinearLayout tmpLinearLayout = new LinearLayout(getContext());
            tmpLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            tmpLinearLayout.setLayoutParams((new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)));
            tmpLinearLayout.setId(100 * i + 0 + 1000);

            CheckBox tempCheckbox = new CheckBox(getContext());
            tempCheckbox.setText("CPU " + String.valueOf(i));
            tempCheckbox.setId(100 * i + 1 + 1000);

            RatingBar tmpRatingBar = new RatingBar(new ContextThemeWrapper(getContext(), R.style.myRatingBar), null, 0);
            tmpRatingBar.setNumStars(1);
            tmpRatingBar.setStepSize(1f);
            tmpRatingBar.setRating((float) 0.0);
            tmpRatingBar.setId(100 * i + 2 + 1000);

            TextView tmpTextFreq = new TextView(getContext());
            tmpTextFreq.setText(" MHz");
            tmpTextFreq.setId(100 * i + 3 + 1000);

            RelativeLayout tmpRelativeLayout = new RelativeLayout(getContext());
            tmpRelativeLayout.addView(tmpRatingBar);
            tmpRelativeLayout.addView(tmpTextFreq);

            LayoutParams tmpParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            tmpParams.gravity = Gravity.CENTER;
            tmpRelativeLayout.setLayoutParams(tmpParams);

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) tmpTextFreq.getLayoutParams();
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            tmpTextFreq.setLayoutParams(layoutParams);


            tmpLinearLayout.addView(tempCheckbox);
            tmpLinearLayout.addView(tmpRelativeLayout);


            cpuInfoLayout.addView(tmpLinearLayout);
        }
        //------------------------
        if (!GpuInfo.isGPUsupport) {
            LinearLayout gpuLoadLayout = (LinearLayout) mContentView.findViewById(R.id.gpuLoadLayout);
            gpuLoadLayout.setVisibility(LinearLayout.GONE);
        }


        mImgBtnMinimize.setOnClickListener(this);
        mImgBtnClose.setOnClickListener(this);

        infoFloatLayout.setOnLongClickListener(this);

        addView(mContentView, LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);

        initWindowLayoutParams();

        mWindowManager.addView(this, mWindowLayoutParams);

    }

    private void initScreenWidthHeihgt() {
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager = (WindowManager) getContext().getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenHeight = metrics.heightPixels;
        Activity activity = (Activity) getContext();
        Rect rect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        mStatusBarHeight = rect.top;

        //Log.d(TAG, "initScreenWidthHeihgt mScreenHeight = " + mScreenHeight);
        //Log.d(TAG, "initScreenWidthHeihgt mStatusBarHeight = " + mStatusBarHeight);
    }

    @SuppressWarnings("deprecation")
    private void initWindowLayoutParams() {
        mWindowLayoutParams = ((FloatingApplication) getContext()
                .getApplicationContext()).getWindowManagerLayoutParams();

        mWindowLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mWindowLayoutParams.format = PixelFormat.RGBA_8888;
        mWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;


        mWindowLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;

        mWindowLayoutParams.x = 0;
        mWindowLayoutParams.y = 0;

        mWindowLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        mLastWindowHeight = mWindowLayoutParams.height;
    }

    public void show() {
        if (null != mContentView && !mIsShowing) {
            //Log.d(TAG, "show()");
            mWindowLayoutParams.height = mLastWindowHeight;
            mWindowManager.updateViewLayout(this, mWindowLayoutParams);
            mContentView.setVisibility(View.VISIBLE);
            mIsShowing = true;
        }
    }

    public void release() {

        if (null != mWindowManager && null != mContentView) {
            //Log.d(TAG, "release()");
            mWindowManager.removeView(this);
            mIsShowing = false;
        }

        mUpdateTask.stopTask();
        mUpdateTask = null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        mRawX = event.getRawX();
        mRawY = event.getRawY() - mStatusBarHeight; // remove the height of notification bar
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchStartX = event.getX();
                mTouchStartY = event.getY();
                mRawStartX = mRawX;
                mRawStartY = mRawY;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isNeedUpdateViewPosition()) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isNeedUpdateViewPosition()) {
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    private boolean isNeedUpdateViewPosition() {
        return Math.abs(mRawX - mRawStartX) > MOVE_DISTANCE_MIN || Math.abs(mRawY - mRawStartY) > MOVE_DISTANCE_MIN;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Log.d(TAG, "onTouchEvent() ");
        mRawX = event.getRawX();
        mRawY = event.getRawY() - mStatusBarHeight;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                updateViewPosition();
                break;
            case MotionEvent.ACTION_UP:
                updateViewPosition();
                SharedPreferences.Editor prefEditor = mSettings.edit();
                prefEditor.putInt("position_x", mWindowLayoutParams.x);
                prefEditor.putInt("position_y", mWindowLayoutParams.y);
                prefEditor.apply();
                break;
        }
        return true;
    }

    private void updateViewPosition() {
        mWindowLayoutParams.x = (int) (mRawX - mTouchStartX);
        mWindowLayoutParams.y = (int) (mRawY - mTouchStartY);
        mWindowManager.updateViewLayout(this, mWindowLayoutParams);
    }

    public void refreshLayout() {
        LinearLayout iLayout = (LinearLayout) findViewById(R.id.infoFloatLayout);
        if (!((iLayout.getMeasuredWidth() == minfoFloatLayout_width) &&
                (iLayout.getMeasuredHeight() == minfoFloatLayout_height))) {
            RescaleLayout(iLayout, scale_ratio / 100.0f);
        }
        //mWindowManager.updateViewLayout(this, mWindowLayoutParams);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_close:
                mMainActivity.finish();
                System.exit(0);
                return;
            case R.id.button_minimize:
                LinearLayout controlFloatLayout = (LinearLayout) findViewById(R.id.controlFloatLayout);
                ImageButton mImgBtnMinimize = (ImageButton) mContentView.findViewById(R.id.button_minimize);
                ImageButton mImgBtnClose = (ImageButton) mContentView.findViewById(R.id.button_close);
                LinearLayout iLayout = (LinearLayout) findViewById(R.id.infoFloatLayout);
                if (!isFloatLayoutShown) {
                    iLayout.setBackgroundResource(R.drawable.shape);

                    controlFloatLayout.setVisibility(LinearLayout.VISIBLE);
                    mImgBtnClose.setVisibility(ImageButton.VISIBLE);
                    mImgBtnMinimize.setBackgroundResource(R.drawable.expander_maximized);
                    isFloatLayoutShown = true;
                    refreshUI_unlock();
                } else {
                    isFloatLayoutShown = false;
                    iLayout.setBackgroundResource(R.drawable.shape_blank);
                    mImgBtnMinimize.setBackgroundResource(R.drawable.expander_minimized);
                    controlFloatLayout.setVisibility(LinearLayout.GONE);
                    mImgBtnClose.setVisibility(ImageButton.GONE);
                    RescaleLayout(iLayout, scale_ratio / 100.0f);
                    refreshUI_unlock();

                }

        }

    }


    @Override
    public boolean onLongClick(View v) {
        //Toast.makeText(getContext(), "Long click!", Toast.LENGTH_SHORT).show();
        //Log.d("VISIALBE", " " + FloatingActivity.activityVisible);

        if (FloatingActivity.activityVisible) {
            mMainActivity.moveTaskToBack(true);
        } else {
            ActivityManager am = (ActivityManager) mMainActivity.getSystemService(Context.ACTIVITY_SERVICE);
            am.moveTaskToFront(mMainActivity.getTaskId(), 0);
        }
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        //Spinner
        Spinner core_spinner = (Spinner) findViewById(parent.getId());

        if (core_spinner.getTag() != pos) {
            core_spinner.setTag(pos);
            Spinner core_spinner_little = (Spinner) findViewById(R.id.core_spinner_little);
            Spinner core_spinner_big = (Spinner) findViewById(R.id.core_spinner_big);

            mCpuInfo.setCoreLittleBig(core_spinner_little.getSelectedItemPosition(),
                    core_spinner_big.getSelectedItemPosition());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //Toast.makeText(getContext(), "Long onNothingSelected!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        //Toast.makeText(getContext(), " Radiogroup id " + group.getCheckedRadioButtonId() + " checkedId " + checkedId , Toast.LENGTH_SHORT).show();
        LinearLayout manualGovernorLayout = (LinearLayout) mContentView.findViewById(R.id.ManualGovernorLayout);
        switch (checkedId) {
            case R.id.radioButtonAuto:
                manualGovernorLayout.setVisibility(LinearLayout.VISIBLE);
                break;
            case R.id.radioButtonManual:
                manualGovernorLayout.setVisibility(LinearLayout.GONE);
                break;
        }
        if (checkedId != group.getTag()) {
            switch (checkedId) {
                case R.id.radioButtonAuto:
                    mCpuInfo.setHPS(true);
                    break;
                case R.id.radioButtonManual:
                    mCpuInfo.setHPS(false);
                    break;
            }
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int checkboxID = buttonView.getId();
        CheckBox tempCheckbox = (CheckBox) mContentView.findViewById(checkboxID);
        //Toast.makeText(getContext(), " CheckBox id " + buttonView.getId() + " checked " + isChecked , 500).show();

        if (tempCheckbox.getTag() != tempCheckbox.isChecked()) {
            boolean checkedstate = tempCheckbox.isChecked();
            String toast_Text;
            if (checkedstate) {
                toast_Text = "enabled";
            } else {
                toast_Text = "disabled";
            }
            switch (checkboxID) {
                case R.id.gpuDVFScheckBox:
                    if (mShow_toast_flag)
                        Toast.makeText(getContext(), " GPU DVFS " + toast_Text + ".", Toast.LENGTH_SHORT).show();
                    mGpuInfo.setDVFSstate(checkedstate);
                    break;
                default:
                    int coreID = (checkboxID - 1 - 1000) / 100;
                    if (mShow_toast_flag)
                        Toast.makeText(getContext(), " Core " + coreID + " " + toast_Text + ".", Toast.LENGTH_SHORT).show();
                    mCpuInfo.setCoreOnline(coreID, checkedstate);
            }

        }
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        if (fromUser) {
            //tmpRatingBar.setId(100 * i + 2 + 1000);
            int coreID = (ratingBar.getId() - 2 - 1000) / 100;

            try {
                if (mShow_toast_flag) {
                    Toast.makeText(getContext(), getResources().getString(R.string.freq_set_to) + mCpuInfo.CoresArraylist.get(coreID).scaling_available_frequencies.get((int) rating - 1) / 1000
                            + " MHz", Toast.LENGTH_SHORT).show();
                }
                mCpuInfo.setCoreMaxfreq(coreID, (int) rating - 1);
                refreshUI_unlock();
            } catch (Throwable e) {
            }

        }
    }


    private void refreshUI_unlock() {
        synchronized (mRefresh_UI_lock) {
            mRefresh_UI_lock.notify();
        }
    }

    class RunningStateUpdaterTask extends AsyncTask<Object, Integer, Boolean> {
        private boolean isStoped = false;

        public RunningStateUpdaterTask() {
            isStoped = false;
        }

        public void updateUI() {
            mCpuInfo.updateAll();
            mGpuInfo.updataAll();
            mMainActivity.runOnUiThread(updateUIrunable);
        }

        private void stopTask() {
            isStoped = true;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            while (!isStoped) {

                updateUI();

                synchronized (mRefresh_UI_lock) {
                    try {
                        mRefresh_UI_lock.wait(mUpdatePeriod);
                    } catch (InterruptedException e) {

                    }
                }
            }
            return true;
        }


    }
}
