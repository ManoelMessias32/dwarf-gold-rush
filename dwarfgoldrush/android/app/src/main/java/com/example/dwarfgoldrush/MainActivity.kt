package com.example.dwarfgoldrush

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable remote debugging
        WebView.setWebContentsDebuggingEnabled(true)

        val myWebView = WebView(this)

        // Configure WebView settings
        myWebView.settings.javaScriptEnabled = true // Enable JavaScript
        myWebView.settings.domStorageEnabled = true    // Enable local storage
        myWebView.settings.allowFileAccess = true      // Allow access to local files
        myWebView.settings.allowContentAccess = true   // Allow content access
        myWebView.settings.allowFileAccessFromFileURLs = true // Allow access from file URLs
        myWebView.settings.allowUniversalAccessFromFileURLs = true // Allow universal access from file URLs
        myWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Set a WebViewClient to handle page navigation within the WebView
        myWebView.webViewClient = WebViewClient()

        // Load the game's index.html file from the root of the assets folder
        myWebView.loadUrl("file:///android_asset/frontend/index.html")

        setContentView(myWebView)
    }
}