package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WebServerState {
    private val _url = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = _url.asStateFlow()

    private val _mode = MutableStateFlow<String>("admin")
    val mode: StateFlow<String> = _mode.asStateFlow()

    fun setUrl(url: String?, mode: String = "admin") {
        _url.value = url
        if (url != null) {
            _mode.value = mode
        }
    }
}
