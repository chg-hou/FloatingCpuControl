package xml;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.houcg.floatingcpucontrol.R;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
//        getPreferenceManager().setSharedPreferencesName("app_setting");
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        //Log.d("d", "============================================onSharedPreferenceChanged");
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {

        //Log.v("log", "onPreferenceTreeClick");
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}