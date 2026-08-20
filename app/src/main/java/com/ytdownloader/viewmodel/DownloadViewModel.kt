package com.ytdownloader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ytdownloader.data.model.DownloadStatus
import com.ytdownloader.data.model.DownloadTask
import com.ytdownloader.data.model.FormatOption
import com.ytdownloader.data.model.VideoInfo
import com.ytdownloader.data.repository.DownloadRepository
import com.ytdownloader.domain.engine.DownloadEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadRepo = DownloadRepository(application)
    private var downloadEngine: DownloadEngine? = null

    private val _downloadState = MutableStateFlow<DownloadTask?>(null)
    val downloadState: StateFlow<DownloadTask?> = _downloadState.asStateFlow()

    private val _history = MutableStateFlow<List<com.ytdownloader.data.model.DownloadHistory>>(emptyList())
    val history: StateFlow<List<com.ytdownloader.data.model.DownloadHistory>> = _history.asStateFlow()

    fun startDownload(
        videoInfo: VideoInfo,
        videoFormat: FormatOption,
        audioFormat: FormatOption
    ) {
        val downloadDir = downloadRepo.getDownloadDir()
        downloadEngine = DownloadEngine(downloadDir)

        viewModelScope.launch {
            downloadEngine?.downloadState?.collect { task ->
                _downloadState.value = task

                if (task?.status == DownloadStatus.COMPLETED) {
                    downloadRepo.saveDownloadHistory(task)
                    loadHistory()
                }
            }
        }

        viewModelScope.launch {
            downloadEngine?.startDownload(videoInfo, videoFormat, audioFormat)
        }
    }

    fun cancelDownload() {
        downloadEngine?.cancelDownload()
        _downloadState.value = null
    }

    fun resetDownload() {
        _downloadState.value = null
    }

    fun loadHistory() {
        viewModelScope.launch {
            _history.value = downloadRepo.getDownloadHistory()
        }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            downloadRepo.deleteHistoryItem(id)
            loadHistory()
        }
    }

    fun getDownloadDir() = downloadRepo.getDownloadDir()

    fun setCustomDownloadDir(uri: android.net.Uri) {
        downloadRepo.setCustomDownloadDir(uri)
    }
}
