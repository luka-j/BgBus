package rs.luka.android.bgbus.ui;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import rs.luka.android.bgbus.R;

/**
 * Fragment displaying preferences, as given in {@link rs.luka.android.bgbus.R.xml.preferences}
 * Created by luka on 11.11.15.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
    }

}
