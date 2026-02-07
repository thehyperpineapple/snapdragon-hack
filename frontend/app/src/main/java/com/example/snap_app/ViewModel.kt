package com.example.snap_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    private val _completionPercentage = MutableStateFlow(0)
    val completionPercentage: StateFlow<Int> = _completionPercentage

    fun updateCompletionPercentage(percentage: Int) {
        viewModelScope.launch {
            _completionPercentage.value = percentage
        }
    }
}