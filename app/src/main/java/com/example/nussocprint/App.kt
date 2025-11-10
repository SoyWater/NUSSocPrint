package com.example.nussocprint

import android.app.Application
import com.example.nussocprint.util.EncryptedDataStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        EncryptedDataStore.init(this)
    }
}