package com.example.nussocprint.ui

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tv = TextView(this)
        tv.text = "My Print Service Settings\n\nThis is a demo service."
        tv.setPadding(64, 64, 64, 64)
        tv.textSize = 18f
        setContentView(tv)
    }
}

