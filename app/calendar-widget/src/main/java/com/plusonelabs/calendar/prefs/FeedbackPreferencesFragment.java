package com.plusonelabs.calendar.prefs;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.plusonelabs.calendar.R;
import com.plusonelabs.calendar.calendar.CalendarQueryStoredResults;

public class FeedbackPreferencesFragment extends PreferenceFragment {
    private static final String KEY_SHARE_EVENTS_FOR_DEBUGGING = "share_events_for_debugging";

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		switch (preference.getKey()) {
            case KEY_SHARE_EVENTS_FOR_DEBUGGING:
                CalendarQueryStoredResults.shareEventsForDebugging(getActivity());
			default:
				break;
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_feedback);
	}
}