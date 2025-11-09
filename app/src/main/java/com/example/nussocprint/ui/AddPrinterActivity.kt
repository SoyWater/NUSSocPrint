package com.example.nussocprint.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity


class AddPrinterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, "Add Printer UI (simulated)", Toast.LENGTH_LONG).show()
        finish() // Close immediately (demo)
    }
}