package com.example.recentresourcefinder

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBList
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.fileEditor.FileEditorManager

class ShowRecentResourcesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // ResourceTrackerService'in tekil örneğini alıyoruz.
        // Bu, ResourceTrackerStartupActivity'nin kaynak eklediği servis örneği olmalı.
        val tracker = ApplicationManager.getApplication().getService(ResourceTrackerService::class.java)

        val recentResources = tracker.getRecentResources()

        // Popup listesindeki öğeleri daha okunur hale getiriyoruz
        val listItems = recentResources.map { "${it.type}: ${it.name}" }
        val list = JBList(listItems)

        val popup = JBPopupFactory.getInstance()
            .createListPopupBuilder(list)
            .setTitle("Son Kullanılan Kaynaklar")
            .setItemChoosenCallback {
                val selectedIndex = list.selectedIndex
                val item = recentResources.getOrNull(selectedIndex) ?: return@setItemChoosenCallback

                val file = VirtualFileManager.getInstance().findFileByUrl("file://${item.filePath}")
                file?.let {
                    // Dosyayı ilgili projede aç
                    FileEditorManager.getInstance(project).openFile(it, true)
                }
            }
            .createPopup()
        popup.showInFocusCenter()
    }
}