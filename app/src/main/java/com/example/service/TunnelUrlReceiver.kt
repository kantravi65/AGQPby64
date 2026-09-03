package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.util.SettingsManager
import com.example.util.WebServerState

class TunnelUrlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.SET_PUBLIC_TUNNEL_URL") {
            val url = intent.getStringExtra("url")?.trim() ?: ""
            if (url.isNotBlank()) {
                val sm = SettingsManager(context)
                sm.publicTunnelUrl = url
                WebServerState.setPublicUrl(url)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "🚀 Public Internet Portal Linked!\n$url",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
