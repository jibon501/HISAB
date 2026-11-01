package com.hisab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hisab.app.ui.HisabApp
import com.hisab.app.ui.HisabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HisabTheme {
                HisabApp()
            }
        }
    }
}
