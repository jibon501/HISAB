package com.hisab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hisab.app.ui.BackupStore
import com.hisab.app.ui.HisabApp
import com.hisab.app.ui.HisabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Documents/আমার হিসাব ফোল্ডার তৈরি (ব্যাকআপ রাখার জায়গা)
        BackupStore.ensureFolder(this)
        setContent {
            HisabTheme {
                HisabApp()
            }
        }
    }
}
