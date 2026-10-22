package com.rabit.clonadora

import android.app.Activity
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val accion = "com.rabit.clonadora.INSTALAR_RESULTADO"

    private val receptor = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val estado = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            when (estado) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    leerIntent(intent)?.let { startActivity(it) }
                }
                PackageInstaller.STATUS_SUCCESS -> {
                    Toast.makeText(context, "App instalada", Toast.LENGTH_LONG).show()
                }
                else -> {
                    val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Toast.makeText(context, "No se pudo instalar: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun leerIntent(i: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= 33) {
            i.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            i.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        }

    private fun elegirApk() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(i, 1)
    }

    private fun crearEspacio() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)) {
            Toast.makeText(this, "Este teléfono no soporta perfiles de trabajo", Toast.LENGTH_LONG).show()
            return
        }
        if (!dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
            Toast.makeText(
                this,
                "Android no permite crear el espacio ahora (quizá ya existe uno)",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val admin = ComponentName(this, AdminReceiver::class.java)
        val i = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
            .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin)
        try {
            startActivityForResult(i, 2)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            data?.data?.let { instalarApk(it) }
        }
        if (requestCode == 2) {
            val msg = if (resultCode == RESULT_OK) {
                "Espacio creado. Busca la pestaña Trabajo en tus apps"
            } else {
                "Creación cancelada o fallida"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun instalarApk(uri: android.net.Uri) {
        try {
            val installer = packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                contentResolver.openInputStream(uri)!!.use { input ->
                    session.openWrite("apk", 0, -1).use { out ->
                        input.copyTo(out)
                        session.fsync(out)
                    }
                }
                val intent = Intent(accion).setPackage(packageName)
                val pi = PendingIntent.getBroadcast(
                    this, id, intent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                session.commit(pi.intentSender)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filtro = IntentFilter(accion)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receptor, filtro, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receptor, filtro)
        }

        val dpm = getSystemService(DevicePolicyManager::class.java)
        val dentroDelEspacio = dpm.isProfileOwnerApp(packageName)

        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }
        val nombres = apps.map { pm.getApplicationLabel(it).toString() }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(32, 160, 32, 32)
        }

        val botonApk = Button(this).apply {
            text = "Instalar un APK"
            setOnClickListener { elegirApk() }
        }
        layout.addView(botonApk)

        if (dentroDelEspacio) {
            layout.addView(TextView(this).apply {
                text = "Estás dentro del espacio de clones"
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(0, 24, 0, 0)
            })
        } else {
            layout.addView(Button(this).apply {
                text = "Crear espacio de clones"
                setOnClickListener { crearEspacio() }
            })
        }

        val titulo = TextView(this).apply {
            text = "Apps instaladas (${apps.size})"
            textSize = 22f
            setTextColor(Color.BLACK)
            setPadding(0, 24, 0, 24)
        }

        val lista = ListView(this)
        lista.adapter = object : ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1, nombres
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(Color.BLACK)
                return v
            }
        }
        lista.setOnItemClickListener { _, _, pos, _ ->
            Toast.makeText(this, apps[pos].packageName, Toast.LENGTH_LONG).show()
        }

        layout.addView(titulo)
        layout.addView(
            lista,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        setContentView(layout)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receptor)
    }
}
