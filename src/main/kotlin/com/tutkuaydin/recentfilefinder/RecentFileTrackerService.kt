package com.tutkuaydin.recentfilefinder

import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.components.*
import java.util.concurrent.CopyOnWriteArrayList
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.StoragePathMacros

@State(
    name = "RecentFileTrackerService",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
@Service(Service.Level.PROJECT)
class RecentFileTrackerService(private val project: Project) : PersistentStateComponent<RecentFileTrackerService.State>,
    FileDocumentManagerListener,
    FileEditorManagerListener {

    class State {
        var recentFiles = CopyOnWriteArrayList<RecentFileItem>()
    }

    private var myState = State()

    private val MAX_RECENT_FILES = 50

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(FileDocumentManagerListener.TOPIC, this)
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }


    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    fun getRecentFiles(): List<RecentFileItem> {
        return myState.recentFiles.toList()
    }

    fun getFavoriteFiles(): List<RecentFileItem> {
        return myState.recentFiles.filter { it.isFavorite }.toList()
    }

    fun clearRecentFiles() {
        val nonFavoriteFiles = myState.recentFiles.filter { !it.isFavorite }
        if (nonFavoriteFiles.isNotEmpty()) {
            myState.recentFiles.removeAll(nonFavoriteFiles.toSet()) // Performans için toSet() kullanabiliriz
        }
    }

    /**
     * Verilen dosyayı son kullanılanlar listesine ekler veya mevcutsa günceller (en başa taşır).
     */
    private fun addOrUpdateRecentFile(file: VirtualFile) {
        if (file.isDirectory || file.fileSystem.protocol != "file" || !file.isValid) return
        if (!file.path.startsWith(project.basePath ?: "")) {
            return
        }

        val filePath = file.path
        val fileName = file.nameWithoutExtension
        val fileExtension = file.extension ?: ""
        val fileType = file.fileType.name

        val existingItem = myState.recentFiles.find { it.filePath == filePath }
        val isCurrentlyFavorite = existingItem?.isFavorite ?: false

        // Mevcut dosyayı listeden kaldır
        myState.recentFiles.removeAll { it.filePath == filePath }

        // Yeni veya güncellenmiş dosyayı listenin başına ekle
        myState.recentFiles.add(0, RecentFileItem(filePath, fileName, fileExtension, fileType, isCurrentlyFavorite))

        // Listeyi maksimum boyutta tut
        if (myState.recentFiles.size > MAX_RECENT_FILES) {
            myState.recentFiles = CopyOnWriteArrayList(myState.recentFiles.take(MAX_RECENT_FILES))
        }
    }

    fun setFileFavoriteStatus(filePath: String, isFavorite: Boolean) {
        val index = myState.recentFiles.indexOfFirst { it.filePath == filePath }
        if (index != -1) {
            val oldItem = myState.recentFiles[index]
            if (oldItem.isFavorite != isFavorite) {
                myState.recentFiles[index] = oldItem.copy(isFavorite = isFavorite)
            }
        }
    }

    fun getAllUniqueFileTypes(): Set<String> {
        return myState.recentFiles.map { it.type }.toSet()
    }

    // --- FileDocumentManagerListener Metotları (belge kaydedildiğinde) ---

    override fun beforeDocumentSaving(document: com.intellij.openapi.editor.Document) {
        val file = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(document)
        if (file != null) {
            addOrUpdateRecentFile(file)
        }
    }

    // --- FileEditorManagerListener Metotları (dosya açıldığında veya sekmeler arası geçişte) ---

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        addOrUpdateRecentFile(file)
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
    }

    override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
        val newFile = event.newFile
        if (newFile != null) {
            addOrUpdateRecentFile(newFile)
        }
    }

    companion object {
        fun getInstance(project: Project): RecentFileTrackerService =
            project.getService(RecentFileTrackerService::class.java)
    }
}
