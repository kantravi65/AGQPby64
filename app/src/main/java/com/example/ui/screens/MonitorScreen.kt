package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.HttpAuthHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.WebServerState

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MonitorScreen(onBack: () -> Unit) {
    val serverUrl by WebServerState.url.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Audio/Video Monitor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (serverUrl != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.domStorageEnabled = true
                        
                        webChromeClient = object : WebChromeClient() {
                            // Automatically grant permissions
                            override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
                                request.grant(request.resources)
                            }
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedHttpAuthRequest(
                                view: WebView,
                                handler: HttpAuthHandler,
                                host: String,
                                realm: String
                            ) {
                                handler.proceed("admin", "1234")
                            }
                            
                            // Bypass SSL error for local self-signed cert
                            @SuppressLint("WebViewClientOnReceivedSslError")
                            override fun onReceivedSslError(
                                view: WebView,
                                handler: android.webkit.SslErrorHandler,
                                error: android.net.http.SslError
                            ) {
                                handler.proceed()
                            }
                        }
                        
                        loadUrl("$serverUrl/admin")
                    }
                }
            )
        } else {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Server is not running. Please start it from Archives.")
            }
        }
    }
}
