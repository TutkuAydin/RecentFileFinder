package com.example.recentresourcefinder

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBList
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import com.intellij.openapi.ui.ComboBox
import javax.swing.*

class ShowRecentFilesAction : AnAction() {
    private lateinit var allRecentFiles: List<RecentFileItem>
    private lateinit var list: JBList<RecentFileItem>
    private lateinit var project: com.intellij.openapi.project.Project
    private lateinit var popup: com.intellij.openapi.ui.popup.JBPopup

    override fun actionPerformed(e: AnActionEvent) {
        project = e.project ?: return

        val tracker = RecentFileTrackerService.getInstance()
        allRecentFiles = tracker.getRecentFiles()

        list = JBList<RecentFileItem>()
        list.cellRenderer = RecentFileItemRenderer()

        val typeFilterComboBox = createTypeFilterComboBox(tracker, list)

        val mainPanel = createMainPanel(typeFilterComboBox, JBScrollPane(list))

        updateFileList("Tüm Dosyalar")

        val preferredSize = Dimension(450, 400)

        val popupBuilder = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mainPanel, list)
            .setTitle("Son Kullanılan Dosyalar")
            .setMovable(true)
            .setResizable(true)
            .setDimensionServiceKey(project, "RecentFilesPopup", true) // Popup boyutunu hatırla
            .setFocusable(true) // Liste odağı alır
            .setRequestFocus(true) // Odağı otomatik iste
            .setCancelOnWindowDeactivation(true) // Pencere odağı kaybedince kapat
            .setBelongsToGlobalPopupStack(true) // Global popup yığınına ait olsun
            .setMinSize(preferredSize) // Minimum boyut belirle

        popup = popupBuilder.createPopup()

        addListListeners()

        popup.showInFocusCenter()
    }

    private fun createTypeFilterComboBox(
        tracker: RecentFileTrackerService,
        listToFocus: JBList<RecentFileItem>
    ): ComboBox<String> {
        val fileTypes = mutableListOf("Tüm Dosyalar").apply {
            addAll(tracker.getAllUniqueFileTypes())
        }
        val comboBoxModel = DefaultComboBoxModel(fileTypes.toTypedArray())
        val typeFilterComboBox = ComboBox(comboBoxModel)

        typeFilterComboBox.preferredSize = Dimension(150, typeFilterComboBox.preferredSize.height)

        typeFilterComboBox.addActionListener {
            val selectedType = typeFilterComboBox.selectedItem as String
            updateFileList(selectedType)

            listToFocus.requestFocusInWindow()
            if (listToFocus.model.size > 0) {
                listToFocus.selectedIndex = 0
            }
        }
        return typeFilterComboBox
    }

    private fun createMainPanel(typeFilterComboBox: ComboBox<String>, scrollPane: JBScrollPane): JBPanel<*> {
        val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())

        val comboBoxPanel = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(5), 0))
        comboBoxPanel.add(typeFilterComboBox)

        mainPanel.add(comboBoxPanel, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        return mainPanel
    }

    private fun updateFileList(selectedType: String) {
        val filteredFiles = if (selectedType == "Tüm Dosyalar") {
            allRecentFiles
        } else {
            allRecentFiles.filter { it.type == selectedType }
        }
        list.setListData(filteredFiles.toTypedArray())
        if (filteredFiles.isNotEmpty()) {
            list.selectedIndex = 0
        }
    }

    private fun addListListeners() {
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(mouseEvent: MouseEvent) {
                if (mouseEvent.clickCount == 2) {
                    val item = list.selectedValue
                    if (item != null) {
                        popup.closeOk(mouseEvent)
                        val file = VirtualFileManager.getInstance().findFileByUrl("file://${item.filePath}")
                        file?.let {
                            FileEditorManager.getInstance(project).openFile(it, true)
                        }
                    }
                }
            }
        })
        list.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                    val item = list.selectedValue
                    if (item != null) {
                        popup.closeOk(e)
                        val file = VirtualFileManager.getInstance().findFileByUrl("file://${item.filePath}")
                        file?.let {
                            FileEditorManager.getInstance(project).openFile(it, true)
                        }
                    }
                }
            }
        })
    }

    private class RecentFileItemRenderer : JLabel(), ListCellRenderer<RecentFileItem> {
        init {
            isOpaque = true
            border = JBUI.Borders.empty(JBUI.scale(1), JBUI.scale(1), JBUI.scale(1), JBUI.scale(1))
        }

        override fun getListCellRendererComponent(
            list: JList<out RecentFileItem>,
            value: RecentFileItem?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            if (value == null) {
                text = ""
                return this
            }

            text = "${value.name}.${value.filePath.substringAfterLast('.')}"

            // TODO: İsterseniz buraya dosya türüne göre ikon ekleyebilirsiniz
            // val icon = com.intellij.ide.FileIconProvider.forFile(file)?.getIcon(file, FLAGS)
            // setIcon(icon)

            if (isSelected) {
                background = list.selectionBackground
                foreground = list.selectionForeground
            } else {
                background = list.background
                foreground = list.foreground
            }
            return this
        }
    }
}
