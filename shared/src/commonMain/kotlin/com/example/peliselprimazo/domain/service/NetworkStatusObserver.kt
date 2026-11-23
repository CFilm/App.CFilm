package com.example.peliselprimazo.domain.service

import kotlinx.coroutines.flow.StateFlow

interface NetworkStatusObserver {
    val isConnected: StateFlow<Boolean>
}

class AlwaysConnectedNetworkStatusObserver : NetworkStatusObserver {
    override val isConnected: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(true)
}
