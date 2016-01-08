package com.houcg.floatingcpucontrol;

/**
 * https://github.com/afarber/android-newbie/blob/q19/MyPrefs/src/de/afarber/myprefs/SeekBarPreference.java
 */

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {
    private SeekBar mSeekBar;
    private TextView mTextView = null;
    private int mProgress = 100;

    public SeekBarPreference(Context context) {
        super(context);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.preference_seekbar, parent, false);
        mSeekBar = (SeekBar) view.findViewById(R.id.seekbar_in_preference);

        mSeekBar.setProgress(mProgress);
        mTextView = (TextView) view.findViewById(R.id.TextView_in_preference);
        mTextView.setText(mProgress + "%");
        mSeekBar.setOnSeekBarChangeListener(this);
        super.onCreateView(parent);
        return view;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //Log.v("log", "onProgressChanged");
        mTextView.setText(progress + "%");
        if (!fromUser)
            return;

        setValue(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // not used
        //Log.v("log", "onStartTrackingTouch");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // not used
        //Log.v("log", "onStopTrackingTouch");
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mProgress) : (Integer) defaultValue);
    }

    public void setValue(int value) {
        if (shouldPersist()) {
            persistInt(value);
        }

        if (value != mProgress) {
            mProgress = value;
            if (mTextView != null)
                mTextView.setText(mProgress + "%");
            notifyChanged();
        }
    }
}