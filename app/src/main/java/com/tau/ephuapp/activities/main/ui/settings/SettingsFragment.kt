package com.tau.ephuapp.activities.main.ui.settings

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.tau.ephuapp.R
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.services.MySettings
import org.jetbrains.anko.defaultSharedPreferences

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    val TAG = "SETTINGS_FRAGMENT"
    var mRootKey: String? = ""
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        /*AskPasswordDialog.display(parentFragmentManager, { password: String? ->
            checkPassword(password, rootKey)
        },null)*/
        mRootKey = rootKey
        showPrompt()
    }

    private fun checkPassword(password: String?) {
        Log.i(TAG, "password recibida: $password")
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        Log.i(TAG, "hay clave en shared pref?: ${sharedPref?.contains("password")}")
        Log.i(TAG, "clave en shared pref: ${sharedPref?.getString("password", "")}")
        if(sharedPref?.contains("password") == true && sharedPref.getString("password", "") == password) {
            setPreferencesFromResource(R.xml.preferences, mRootKey)
        } else {
            Utilities.showAlert(requireContext(), getString(R.string.error), getString(R.string.wrong_password_error_msg), {
                showPrompt()
            }, null, null, null, true, false)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    fun setLayout(rootKey: String?){
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.i(TAG, "la configuracion ha cambiado...")
        val value = sharedPreferences?.getString(key, "")
        Log.i(TAG, "nuevo valor para $key: $value")
        //MySettings.resetValues(requireContext())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.i(TAG, "onSaveInstanceState")
        //MySettings.resetValues(requireContext())
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause")
        MySettings.resetValues(requireContext())
    }

    fun showPrompt(){
        activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val dView = inflater.inflate(R.layout.fragment_askpassword_dialog, null)
            builder.setView(dView)
                .setTitle(getString(R.string.restricted_area))
                // Add action buttons
                .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { dialog, id ->
                    val password = dView.findViewById<EditText>(R.id.password)?.text.toString()
                    checkPassword(password)
                })
                .setNegativeButton(R.string.cancel) { dialog, id ->
                    dialog?.cancel()
                }
            builder.show()
        }
    }
}