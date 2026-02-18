package com.viktorholk.apipushnotifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.viktorholk.apipushnotifications.ui.AppTheme
import com.viktorholk.apipushnotifications.ui.ConfigViewModel
import com.viktorholk.apipushnotifications.ui.MainScreen

class MainActivity : ComponentActivity() {

    private val viewModel: ConfigViewModel by viewModels()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { viewModel.handleBroadcast(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Register receiver
        val filter = IntentFilter("serviceFragmentBroadcast")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
             registerReceiver(broadcastReceiver, filter)
        }

        setContent {
            AppTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }
}
