package com.rabit.clonadora

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(48, 200, 48, 48)
        }
        val texto = TextView(this).apply {
            text = "Clonadora funciona"
            textSize = 28f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        layout.addView(texto)
        setContentView(layout)
    }
}
