package com.example.recentresourcefinder

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBList
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

class ShowRecentFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val tracker = RecentFileTrackerService.getInstance()

        val allRecentFiles = tracker.getRecentFiles()

        val list = JBList<RecentFileItem>()
        list.cellRenderer = RecentFileItemRenderer()

        val scrollPane = JBScrollPane(list)
        val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())

        val fileTypes = mutableListOf("Tüm Dosyalar").apply {
            addAll(tracker.getAllUniqueFileTypes())
        }

        val comboBoxModel = DefaultComboBoxModel(fileTypes.toTypedArray())
        val typeFilterComboBox = JComboBox(comboBoxModel)
        typeFilterComboBox.preferredSize = Dimension(150, typeFilterComboBox.preferredSize.height)
        val comboBoxPanel = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(5), 0))
        comboBoxPanel.add(typeFilterComboBox)

        mainPanel.add(comboBoxPanel, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        val updateFileList = { selectedType: String ->
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

        typeFilterComboBox.addActionListener {
            val selectedType = typeFilterComboBox.selectedItem as String
            updateFileList(selectedType)
        }

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

        val popup = popupBuilder.createPopup()
        // Listeye MouseListener ekleyerek çift tıklama olayını yakalıyoruz
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(mouseEvent: MouseEvent) {
                if (mouseEvent.clickCount == 2) { // Çift tıklama kontrolü
                    val item = list.selectedValue // Doğrudan RecentFileItem alıyoruz
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
        // Klavyeden Enter tuşuna basıldığında da dosyanın açılmasını sağlayalım
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
        popup.showInFocusCenter()
    }

    private class RecentFileItemRenderer : JLabel(), ListCellRenderer<RecentFileItem> {
        init {
            isOpaque = true
            border = EmptyBorder(1, 1, 1, 1)
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
