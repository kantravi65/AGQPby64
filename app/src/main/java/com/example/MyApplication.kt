package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.json.JSONObject

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase is now initialized automatically via the google-services plugin
        /*
        try {
            val inputStream = assets.open("firebase-applet-config.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            
            val options = FirebaseOptions.Builder()
                .setProjectId(jsonObject.getString("projectId"))
                .setApplicationId(jsonObject.getString("appId"))
                .setApiKey(jsonObject.getString("apiKey"))
                .setStorageBucket(jsonObject.getString("storageBucket"))
                .build()
            
            FirebaseApp.initializeApp(this, options)
            Log.d("MyApplication", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "Failed to initialize Firebase", e)
        }
        */
    }
}
