package com.rabit.clonadora

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

data class AppItem(
    val nombre: String,
    val paquete: String,
    val icono: Drawable,
    val enEspacio: Boolean
)

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

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

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
                "Espacio creado. Abre una vez la Clonadora de la pestaña Trabajo"
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

    private fun paquetesEnOtrosPerfiles(): Set<String> {
        val res = mutableSetOf<String>()
        try {
            val la = getSystemService(LauncherApps::class.java)
            val yo = Process.myUserHandle()
            for (p in la.profiles) {
                if (p != yo) {
                    la.getActivityList(null, p).forEach {
                        res.add(it.applicationInfo.packageName)
                    }
                }
            }
        } catch (e: Exception) {
        }
        return res
    }

    private fun preguntarClon(app: AppItem) {
        val extra = if (app.enEspacio) "\n\n(Ya hay una copia en el espacio de trabajo.)" else ""
        AlertDialog.Builder(this)
            .setTitle(app.nombre)
            .setIcon(app.icono)
            .setMessage("¿Quieres crear un clon de esta app?$extra")
            .setPositiveButton("Sí, clonar") { _, _ -> enviarClon(app) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarClon(app: AppItem) {
        val i = Intent(ACCION_CLONAR)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .putExtra("paquete", app.paquete)
        try {
            startActivity(i)
            Handler(Looper.getMainLooper()).postDelayed({ construirPantalla() }, 2500)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "El espacio no está listo. Abre una vez la Clonadora de la pestaña Trabajo y vuelve a intentar.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun construirPantalla() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val dentroDelEspacio = dpm.isProfileOwnerApp(packageName)
        val hayEspacio = getSystemService(UserManager::class.java).userProfiles.size > 1

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(32, 160, 32, 32)
        }

        layout.addView(Button(this).apply {
            text = "Instalar un APK"
            setOnClickListener { elegirApk() }
        })

        if (dentroDelEspacio) {
            prepararEspacio(this)
            layout.addView(TextView(this).apply {
                text = "Espacio de clones listo.\n\nAbre la Clonadora normal (la de fuera del maletín) para elegir qué apps clonar."
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(0, 32, 0, 0)
            })
            setContentView(layout)
            return
        }

        if (!hayEspacio) {
            layout.addView(Button(this).apply {
                text = "Crear espacio de clones"
                setOnClickListener { crearEspacio() }
            })
        }

        val pm = packageManager
        val enEspacio = paquetesEnOtrosPerfiles()
        val items = pm.getInstalledApplications(0)
            .filter { it.packageName != packageName && pm.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                AppItem(
                    pm.getApplicationLabel(it).toString(),
                    it.packageName,
                    pm.getApplicationIcon(it),
                    enEspacio.contains(it.packageName)
                )
            }
            .sortedBy { it.nombre.lowercase() }

        layout.addView(TextView(this).apply {
            text = "Toca una app para clonarla"
            textSize = 20f
            setTextColor(Color.BLACK)
            setPadding(0, 24, 0, 16)
        })

        val lista = ListView(this)
        lista.adapter = object : BaseAdapter() {
            override fun getCount() = items.size
            override fun getItem(position: Int) = items[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = items[position]
                val fila = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(8), dp(10), dp(8), dp(10))
                }
                val img = ImageView(this@MainActivity).apply {
                    setImageDrawable(item.icono)
                    layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                }
                val txt = TextView(this@MainActivity).apply {
                    text = item.nombre + if (item.enEspacio) "   (ya en el espacio)" else ""
                    textSize = 17f
                    setTextColor(Color.BLACK)
                    setPadding(dp(16), 0, 0, 0)
                }
                fila.addView(img)
                fila.addView(txt)
                return fila
            }
        }
        lista.setOnItemClickListener { _, _, pos, _ -> preguntarClon(items[pos]) }

        layout.addView(
            lista,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        setContentView(layout)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filtro = IntentFilter(accion)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receptor, filtro, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receptor, filtro)
        }
    }

    override fun onResume() {
        super.onResume()
        construirPantalla()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receptor)
    }
}
