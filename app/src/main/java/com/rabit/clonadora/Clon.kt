package com.rabit.clonadora

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast

const val ACCION_CLONAR = "com.rabit.clonadora.CLONAR"

fun prepararEspacio(context: Context) {
    try {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        if (!dpm.isProfileOwnerApp(context.packageName)) return
        val admin = ComponentName(context, AdminReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, CloneRequestActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        dpm.clearCrossProfileIntentFilters(admin)
        val filtro = IntentFilter(ACCION_CLONAR).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        dpm.addCrossProfileIntentFilter(
            admin, filtro, DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED
        )
    } catch (e: Exception) {
        Toast.makeText(context, "Error preparando el espacio: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

class CloneRequestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val paquete = intent.getStringExtra("paquete")
        val mensaje = try {
            val dpm = getSystemService(DevicePolicyManager::class.java)
            val admin = ComponentName(this, AdminReceiver::class.java)
            if (paquete == null) {
                "Falta el nombre de la app"
            } else if (!dpm.isProfileOwnerApp(packageName)) {
                "Esta copia no administra el espacio"
            } else if (Build.VERSION.SDK_INT >= 28 && dpm.installExistingPackage(admin, paquete)) {
                "Clon creado en el espacio de trabajo"
            } else {
                "No se pudo crear el clon"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
        Toast.makeText(applicationContext, mensaje, Toast.LENGTH_LONG).show()
        finish()
    }
}
