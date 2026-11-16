package com.contoh.modkeyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class SetupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        col.addView(TextView(this).apply {
            text = "Mod Keyboard\n\nLangkah:\n1. Hidupkan Mod Keyboard dalam senarai.\n2. Pilih Mod Keyboard sebagai papan kekunci aktif.\n3. Uji menaip di kotak bawah."
            textSize = 16f
        })
        col.addView(Button(this).apply {
            text = "1. Hidupkan Mod Keyboard"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        })
        col.addView(Button(this).apply {
            text = "2. Pilih Mod Keyboard"
            setOnClickListener {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showInputMethodPicker()
            }
        })
        col.addView(EditText(this).apply { hint = "Uji menaip di sini" })
        setContentView(col)
    }
}
