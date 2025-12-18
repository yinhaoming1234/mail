package com.yhm.mymail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yhm.mymail.ui.AppNavigation
import com.yhm.mymail.ui.theme.MymailTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MymailTheme {
                AppNavigation()
            }
        }
    }
}