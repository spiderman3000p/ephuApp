package com.tau.ephuapp.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.AppIntroPageTransformerType
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.main.MainActivity
import com.tau.ephuapp.classes.Utilities

class IntroActivity : AppIntro() {
    private val TAG = "INTRO_ACTIVITY"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_intro)
        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        isColorTransitionsEnabled = true
        isSkipButtonEnabled = false
        addSlide(AppIntroFragment.newInstance(
            title = "Bienvenido!",
            description = "Antes de iniciar, es necesario otorgar ciertos permisos a la app",
            backgroundColor = resources.getColor(R.color.design_default_color_primary)
        ))
        addSlide(AppIntroFragment.newInstance(
            title = "Empecemos...",
            description = "Necesitamos permisos para usar la camara y acceder al estado del dispositivo",
            backgroundColor = resources.getColor(R.color.design_default_color_primary_dark)
        ))
        askForPermissions(
            permissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                //Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            slideNumber = 2,
            required = true)
        setTransformer(AppIntroPageTransformerType.Zoom)
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on "Skip"
        Utilities.showAlert(this, getString(R.string.error), getString(R.string.skiped_intro_permissions))
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"
        saveIntroDone()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    fun saveIntroDone() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        with(sharedPref.edit()) {
            putBoolean("firstRun", true)
            commit()
        }
    }

    override fun onUserDeniedPermission(permissionName: String) {
        // User pressed "Deny" on the permission dialog
        Log.i(TAG, "permiso desactivado: $permissionName")
        Utilities.showAlert(this, getString(R.string.warning), getString(R.string.user_denied_permission_msg))
    }
    override fun onUserDisabledPermission(permissionName: String) {
        // User pressed "Deny" + "Don't ask again" on the permission dialog
        Log.i(TAG, "permiso desactivado: $permissionName")
        Utilities.showAlert(this, getString(R.string.warning), getString(R.string.user_disabled_permission_msg))
    }
}