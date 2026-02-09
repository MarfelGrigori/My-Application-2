package com.example.myapplication2

import android.app.Application
import dagger.hilt.android.HiltAndroidApp



@HiltAndroidApp
class CryptoWalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}