package com.example.recentresourcefinder

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

data class RecentFileItem(val name: String, val type: String, val filePath: String)

@Service(Service.Level.APP)
class RecentFileTrackerService {
    private val recentFiles = mutableListOf<RecentFileItem>()
    private val maxSize = 20

    fun addFile(fileItem: RecentFileItem) {
        recentFiles.removeIf { it.filePath == fileItem.filePath }
        recentFiles.add(0, fileItem)
        if (recentFiles.size > maxSize) {
            recentFiles.removeLast()
        }
    }

    fun getRecentFiles(): List<RecentFileItem> = recentFiles.toList()

    fun getAllUniqueFileTypes(): List<String> {
        return recentFiles.map { it.type }.distinct().sorted()
    }

    companion object {
        fun getInstance(): RecentFileTrackerService {
            return ApplicationManager.getApplication().getService(RecentFileTrackerService::class.java)
        }
    }
}