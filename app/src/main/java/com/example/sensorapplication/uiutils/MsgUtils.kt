package com.example.sensorapplication.uiutils


import android.widget.Toast

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context

object MsgUtils {
    // short message
    fun showToast(msg: String?, context: Context?) {
        val toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT)
        toast.show()
    }

    // alert message
    fun createDialog(title: String?, msg: String?, context: Context?): Dialog {
        val builder = AlertDialog.Builder(
            context!!
        )
        builder.setTitle(title)
        builder.setMessage(msg)
        builder.setPositiveButton(" Ok") { dialog, id ->
            // do nothing, just close the alert
        }
        return builder.create()
    }
}