package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WebServerState {
    private val _url = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = _url.asStateFlow()

    private val _httpUrl = MutableStateFlow<String?>(null)
    val httpUrl: StateFlow<String?> = _httpUrl.asStateFlow()

    private val _publicUrl = MutableStateFlow<String?>(null)
    val publicUrl: StateFlow<String?> = _publicUrl.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun setError(err: String?) {
        _error.value = err
    }

    private val _mode = MutableStateFlow<String>("admin")
    val mode: StateFlow<String> = _mode.asStateFlow()

    fun setUrl(url: String?, mode: String = "admin", httpUrl: String? = null, publicUrl: String? = null) {
        _url.value = url
        _httpUrl.value = httpUrl
        _publicUrl.value = publicUrl
        if (url != null) {
            _mode.value = mode
        }
    }

    fun setPublicUrl(publicUrl: String?) {
        _publicUrl.value = publicUrl?.trim()?.trimEnd('/')
    }
}
