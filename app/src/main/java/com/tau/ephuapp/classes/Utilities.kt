package com.tau.ephuapp.classes

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.tau.ephuapp.BuildConfig
import com.tau.ephuapp.R
import com.tau.ephuapp.models.TaskState
import org.jetbrains.anko.runOnUiThread
import java.lang.Exception
import java.util.*

class Utilities {
    companion object{
        val TAG = "UTILITIES"
        fun getStateColor(taskState: TaskState?): Int{
            return when(taskState){
                TaskState.Paused -> R.color.ps_state
                TaskState.WorkInProgress -> R.color.wp_state
                TaskState.Complete -> R.color.cp_state
                TaskState.Pending -> R.color.pe_state
                TaskState.Active -> R.color.pe_active
                TaskState.Cancelled -> R.color.pe_cancelled
                else -> R.color.unknown
            }
        }

        fun getAndroidId(context: Context): String{
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }

        fun getVersionName(): String? {
            return BuildConfig.VERSION_NAME
        }

        fun getResumedState(taskState: TaskState?): CharSequence? {
            return when(taskState){
                TaskState.Paused -> "PS"
                TaskState.WorkInProgress -> "WP"
                TaskState.Complete -> "CP"
                TaskState.Pending -> "PE"
                TaskState.Active -> "AC"
                TaskState.Cancelled -> "CC"
                else -> ""
            }
        }

        fun showAlert(context: Context, title: String, message: String) {
            Log.i(TAG, "showing alert...")
            context.runOnUiThread {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(title)
                builder.setMessage(message)
                builder.setPositiveButton("Aceptar", null)
                val dialog: AlertDialog = builder.create()
                if (context is Activity && !context.isFinishing) {
                    dialog.show()
                }
            }
        }

        fun showAlert(
            context: Context,
            title: String,
            message: String,
            positiveCallback: (() -> Unit)? = null,
            negativeCallback: (() -> Unit)? = null,
            positiveText: String? = null,
            negativeText: String? = null,
            showPositiveBtn: Boolean = true,
            showNegativeBtn: Boolean = true
        ) {
            context.runOnUiThread {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(title)
                builder.setMessage(message)
                if(showPositiveBtn) {
                    builder.setPositiveButton(
                        (positiveText
                            ?: getString(R.string.ok)).capitalize(Locale.getDefault()),
                        DialogInterface.OnClickListener { dialog, id ->
                            positiveCallback?.let {
                                it()
                            }
                            dialog.dismiss()
                        })
                }
                if(showNegativeBtn) {
                    builder.setNegativeButton(
                        (negativeText
                            ?: getString(R.string.cancel)).capitalize(Locale.getDefault()),
                        DialogInterface.OnClickListener { dialog, id ->
                            negativeCallback?.let {
                                it()
                            }
                            dialog.dismiss()
                        })
                }
                builder.setCancelable(false)
                if (context is Activity && !context.isFinishing) {
                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
            }
        }

        fun showToast(context: Context, message: String){
            context.runOnUiThread {
                try {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }catch (e: Exception){
                    e.printStackTrace()
                    Log.e(TAG, "Excepcion al mostrar toast", e)
                }
            }
        }
    }
}