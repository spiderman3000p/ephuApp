package com.tau.ephuapp.activities.main.ui.settings

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.tau.ephuapp.R
import com.tau.ephuapp.services.MySettings

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    val TAG = "SETTINGS_FRAGMENT"
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.i(TAG, "la configuracion ha cambiado...")
        val value = sharedPreferences?.getString(key, "")
        Log.i(TAG, "nuevo valor para $key: $value")
        MySettings.resetValues(requireContext())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.i(TAG, "onSaveInstanceState")
        MySettings.resetValues(requireContext())
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause")
        MySettings.resetValues(requireContext())
    }
}