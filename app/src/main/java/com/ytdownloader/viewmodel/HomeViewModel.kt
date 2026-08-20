package com.ytdownloader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ytdownloader.data.model.FormatOption
import com.ytdownloader.data.model.VideoInfo
import com.ytdownloader.data.repository.YtDlpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = YtDlpRepository()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    fun updateUrl(newUrl: String) {
        _url.value = newUrl
    }

    fun fetchVideo() {
        val currentUrl = _url.value.trim()
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            try {
                val videoInfo = repository.fetchVideoInfo(currentUrl)
                val (videoFormats, audioFormats) = repository.fetchFormats(currentUrl)

                _uiState.value = HomeUiState.Success(
                    videoInfo = videoInfo,
                    videoFormats = videoFormats,
                    audioFormats = audioFormats
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    message = e.message ?: "Failed to fetch video"
                )
            }
        }
    }

    fun reset() {
        _uiState.value = HomeUiState.Idle
        _url.value = ""
    }
}

sealed class HomeUiState {
    data object Idle : HomeUiState()
    data object Loading : HomeUiState()
    data class Success(
        val videoInfo: VideoInfo,
        val videoFormats: List<FormatOption>,
        val audioFormats: List<FormatOption>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
