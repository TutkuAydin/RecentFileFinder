package com.example.recentresourcefinder

import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.startup.StartupActivity
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiDirectory

class ResourceTrackerStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.messageBus.connect()
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    val tracker = ApplicationManager.getApplication().getService(ResourceTrackerService::class.java)
                    val psiFile = PsiManager.getInstance(source.project).findFile(file)

                    val resourceType: String
                    val resourceName: String

                    // PsiFile'ı kullanarak daha doğru kaynak türü belirleme
                    if (psiFile is XmlFile) {
                        when (file.parent?.name) {
                            "layout" -> {
                                resourceType = "layout"
                                resourceName = file.nameWithoutExtension
                            }

                            "drawable" -> {
                                resourceType = "drawable"
                                resourceName = file.nameWithoutExtension
                            }

                            "values" -> {
                                // values klasöründeki XML dosyaları için daha spesifik kontrol
                                val rootTag = psiFile.rootTag
                                resourceType = when (rootTag?.name) {
                                    "resources" -> {
                                        // resources tag'i içindeki child tag'lere bakarak string, color, dimen vb. ayırabiliriz
                                        // Şimdilik dosya adına göre basitleştirelim, istersen daha derine inebiliriz
                                        when {
                                            file.nameWithoutExtension.startsWith("strings") -> "string"
                                            file.nameWithoutExtension.startsWith("colors") -> "color"
                                            file.nameWithoutExtension.startsWith("dimens") -> "dimen"
                                            else -> "value"
                                        }
                                    }

                                    else -> "xml_value_file"
                                }
                                resourceName = file.nameWithoutExtension
                            }

                            else -> {
                                resourceType = "xml_file"
                                resourceName = file.nameWithoutExtension
                            }
                        }
                    } else {
                        // XML olmayan dosyalar için mevcut mantığı koru
                        resourceType = when {
                            file.extension == "png" || file.extension == "jpg" -> "image"
                            else -> "file"
                        }
                        resourceName = file.nameWithoutExtension
                    }

                    tracker.addResource(ResourceItem(resourceName, resourceType, file.path))
                }
            })
    }
}
/*class ResourceUsageListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val document = editor.document
        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return

        val tracker = ApplicationManager.getApplication().getService(ResourceTrackerService::class.java)

        /*// --- Kotlin için:
        val ktRefs = PsiTreeUtil.collectElementsOfType(
            psiFile,
            KtDotQualifiedExpression::class.java
        ) as Collection<KtDotQualifiedExpression>

        ktRefs.forEach { expr ->
            val text = expr.text // örn: R.drawable.logo
            if (text.startsWith("R.")) {
                val parts = text.split('.')
                if (parts.size == 3) {
                    val type = parts[1]
                    val name = parts[2]
                    tracker.addResource(ResourceItem(name, type, virtualFile.path))
                }
            }
        }

        // --- Java için:
        val javaRefs = PsiTreeUtil.collectElementsOfType(psiFile, PsiReferenceExpression::class.java)
        javaRefs.forEach { ref ->
            val text = ref.text // örn: R.drawable.logo
            if (text.startsWith("R.")) {
                val parts = text.split('.')
                if (parts.size == 3) {
                    val type = parts[1]
                    val name = parts[2]
                    tracker.addResource(ResourceItem(name, type, virtualFile.path))
                }
            }
        }*/
    }
}*/
