package com.example.recentresourcefinder

import com.intellij.icons.AllIcons
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
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.LayeredIcon
import com.intellij.util.PlatformIcons
import com.intellij.ui.components.JBTabbedPane
import javax.swing.*

class ShowRecentFilesAction : AnAction() {
    private lateinit var recentFilesList: JBList<RecentFileItem>
    private lateinit var favoriteFilesList: JBList<RecentFileItem>
    private lateinit var project: com.intellij.openapi.project.Project
    private lateinit var popup: com.intellij.openapi.ui.popup.JBPopup
    private lateinit var typeFilterComboBox: ComboBox<String>

    override fun actionPerformed(e: AnActionEvent) {
        project = e.project ?: return

        val tracker = RecentFileTrackerService.getInstance(project)

        recentFilesList = JBList<RecentFileItem>()
        recentFilesList.cellRenderer = RecentFileItemRenderer()

        favoriteFilesList = JBList<RecentFileItem>()
        favoriteFilesList.cellRenderer = RecentFileItemRenderer()

        typeFilterComboBox = createTypeFilterComboBox(tracker)

        val tabbedPane = JBTabbedPane()
        tabbedPane.addTab("Recent Files", JBScrollPane(recentFilesList))
        tabbedPane.addTab("Favorites", JBScrollPane(favoriteFilesList))

        val mainPanel = createMainPanel(typeFilterComboBox, tabbedPane)

        updateLists(tracker)

        val preferredSize = Dimension(450, 400)

        val popupBuilder = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mainPanel, recentFilesList)
            .setTitle("Son Kullanılan Dosyalar & Favoriler")
            .setMovable(true)
            .setResizable(true)
            .setDimensionServiceKey(project, "RecentFilesPopup", true)
            .setFocusable(true)
            .setRequestFocus(true)
            .setCancelOnWindowDeactivation(true)
            .setBelongsToGlobalPopupStack(true)
            .setMinSize(preferredSize)

        popup = popupBuilder.createPopup()

        addListListeners(recentFilesList)
        addListListeners(favoriteFilesList)

        popup.showInFocusCenter()
    }

    private fun createTypeFilterComboBox(tracker: RecentFileTrackerService): ComboBox<String> {
        val fileTypes = mutableListOf("All Files").apply {
            addAll(tracker.getAllUniqueFileTypes())
        }
        val comboBoxModel = DefaultComboBoxModel(fileTypes.toTypedArray())
        val typeFilterComboBox = ComboBox(comboBoxModel)

        typeFilterComboBox.preferredSize = Dimension(150, typeFilterComboBox.preferredSize.height)

        typeFilterComboBox.addActionListener {
            updateLists(tracker)
            val selectedList =
                if ((popup.content as JBTabbedPane).selectedComponent == (recentFilesList.parent as JBScrollPane)) recentFilesList else favoriteFilesList
            selectedList.requestFocusInWindow()
            if (selectedList.model.size > 0) {
                selectedList.selectedIndex = 0
            }
        }
        return typeFilterComboBox
    }

    private fun createMainPanel(typeFilterComboBox: ComboBox<String>, tabbedPane: JBTabbedPane): JBPanel<*> {
        val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())

        val topPanel = JPanel(BorderLayout())
        val comboBoxPanel = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(5), 0))
        comboBoxPanel.add(typeFilterComboBox)
        topPanel.add(comboBoxPanel, BorderLayout.EAST)

        mainPanel.add(topPanel, BorderLayout.NORTH)
        mainPanel.add(tabbedPane, BorderLayout.CENTER)
        return mainPanel
    }

    private fun updateLists(tracker: RecentFileTrackerService) {
        val selectedType = typeFilterComboBox.selectedItem as String

        val filteredRecentFiles = if (selectedType == "All Files") {
            tracker.getRecentFiles()
        } else {
            tracker.getRecentFiles().filter { !it.isFavorite && it.type == selectedType }
        }
        recentFilesList.setListData(filteredRecentFiles.toTypedArray())

        val filteredFavoriteFiles = if (selectedType == "All Files") {
            tracker.getFavoriteFiles()
        } else {
            tracker.getFavoriteFiles().filter { it.type == selectedType }
        }
        favoriteFilesList.setListData(filteredFavoriteFiles.toTypedArray())

        if (recentFilesList.model.size > 0) recentFilesList.selectedIndex = 0
        else if (favoriteFilesList.model.size > 0) favoriteFilesList.selectedIndex = 0
    }

    private fun addListListeners(listToListen: JBList<RecentFileItem>) {
        listToListen.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(mouseEvent: MouseEvent) {
                if (mouseEvent.clickCount == 2) {
                    openSelectedFile(listToListen)
                }
            }

            override fun mousePressed(e: MouseEvent) {
                showPopupMenu(e, listToListen)
            }

            override fun mouseReleased(e: MouseEvent) {
                showPopupMenu(e, listToListen)
            }
        })
        listToListen.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                    openSelectedFile(listToListen)
                }
            }
        })
    }

    private fun openSelectedFile(list: JBList<RecentFileItem>) {
        val item = list.selectedValue
        if (item != null) {
            popup.closeOk(null)
            val file = VirtualFileManager.getInstance().findFileByUrl("file://${item.filePath}")
            file?.let {
                FileEditorManager.getInstance(project).openFile(it, true)
            }
        }
    }

    private fun showPopupMenu(e: MouseEvent, list: JBList<RecentFileItem>) {
        if (e.isPopupTrigger) {
            val index = list.locationToIndex(e.point)
            if (index != -1) {
                list.selectedIndex = index
                val selectedItem = list.selectedValue ?: return

                val popupMenu = JPopupMenu()
                val tracker = RecentFileTrackerService.getInstance(project)

                val favoriteMenuItem =
                    JMenuItem(if (selectedItem.isFavorite) "Remove from Favorites" else "Add to Favorites")
                favoriteMenuItem.addActionListener {
                    tracker.setFileFavoriteStatus(selectedItem.filePath, !selectedItem.isFavorite)
                    updateLists(tracker)
                }
                popupMenu.add(favoriteMenuItem)

                popupMenu.show(list, e.x, e.y)
            }
        }
    }

    private class RecentFileItemRenderer : JLabel(), ListCellRenderer<RecentFileItem> {
        private val FAVORITE_OVERLAY_ICON = AllIcons.Nodes.Favorite

        init {
            isOpaque = true
            border = JBUI.Borders.empty(JBUI.scale(1), JBUI.scale(5), JBUI.scale(1), JBUI.scale(1))
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
                icon = null
                return this
            }

            text = "${value.name}.${value.extension}"
            val fileType = FileTypeManager.getInstance().getFileTypeByExtension(value.extension)
            val mainIcon: Icon = fileType.icon ?: PlatformIcons.FILE_ICON

            if (value.isFavorite) {
                val layeredIcon = LayeredIcon(2)
                layeredIcon.setIcon(mainIcon, 0)
                layeredIcon.setIcon(FAVORITE_OVERLAY_ICON, 1, 8, 8)
                icon = layeredIcon
            } else {
                icon = mainIcon
            }


            if (isSelected) {
                background = list.selectionBackground
                foreground = list.selectionForeground
            } else {
                background = list.background
                foreground = list.foreground
            }

            font = if (value.isFavorite) {
                font.deriveFont(java.awt.Font.BOLD)
            } else {
                font.deriveFont(java.awt.Font.PLAIN)
            }
            return this
        }
    }
}