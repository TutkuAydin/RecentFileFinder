package com.example.recentresourcefinder

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBList
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.text.StringUtil // Gerekirse dosya yolunu kısaltmak için

class ShowRecentFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val tracker = RecentFileTrackerService.getInstance()

        val recentFiles = tracker.getRecentFiles()

        val listItems =
            recentFiles.map { "${it.name}.${it.filePath.substringAfterLast('.') ?: ""} (${it.type})" }
                .map { StringUtil.shortenPathWithEllipsis(it, 50) }

        val list = JBList(listItems)

        val popupBuilder = JBPopupFactory.getInstance()
            .createListPopupBuilder(list)
            .setTitle("Son Kullanılan Dosyalar")
            .setItemChoosenCallback {
                val selectedIndex = list.selectedIndex
                val item = recentFiles.getOrNull(selectedIndex) ?: return@setItemChoosenCallback

                val file = VirtualFileManager.getInstance().findFileByUrl("file://${item.filePath}")
                file?.let {
                    FileEditorManager.getInstance(project).openFile(it, true)
                }
            }

        val popup = popupBuilder.createPopup()
        popup.showInFocusCenter()
    }
}
