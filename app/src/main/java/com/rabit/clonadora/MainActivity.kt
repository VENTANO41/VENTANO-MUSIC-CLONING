package com.rabit.clonadora

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        val titulo = TextView(this).apply {
            text = "Apps instaladas (${apps.size})"
            textSize = 22f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 24)
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
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )
        setContentView(layout)
    }
}
