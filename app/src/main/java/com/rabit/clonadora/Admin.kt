package com.rabit.clonadora

import android.app.Activity
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle

class AdminReceiver : DeviceAdminReceiver() {
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        activarPerfil(context)
    }
}

fun activarPerfil(context: Context) {
    try {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(context, AdminReceiver::class.java)
        dpm.setProfileName(admin, "Clones")
        dpm.setProfileEnabled(admin)
    } catch (e: Exception) {
        // si falla, el sistema ya lo habrá activado
    }
}

class ProvisioningModeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resultado = Intent().putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_MODE,
            DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
        )
        setResult(RESULT_OK, resultado)
        finish()
    }
}

class PolicyComplianceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activarPerfil(this)
        setResult(RESULT_OK)
        finish()
    }
}
