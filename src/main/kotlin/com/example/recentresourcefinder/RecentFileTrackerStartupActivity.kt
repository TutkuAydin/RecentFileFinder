package com.example.recentresourcefinder

import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiFile
import com.intellij.lang.Language

class RecentFileTrackerStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.messageBus.connect()
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {

                    val tracker = RecentFileTrackerService.getInstance()
                    val psiFile = PsiManager.getInstance(source.project).findFile(file)

                    val fileName = file.nameWithoutExtension
                    val fileExtension = file.extension ?: ""
                    val fileType: String

                    if (psiFile != null) {
                        // PsiFile varsa, dilini veya dosya türünü kullan
                        fileType = psiFile.language.displayName // Kotlin, Java, XML, Plain Text vb.
                    } else {
                        // PsiFile yoksa, uzantıya göre genel bir tür belirle
                        fileType = when (fileExtension.toLowerCase()) {
                            "kt" -> "Kotlin File"
                            "java" -> "Java File"
                            "xml" -> "XML File"
                            "gradle" -> "Gradle Script"
                            "json" -> "JSON File"
                            "md" -> "Markdown File"
                            "png", "jpg", "jpeg", "gif" -> "Image File"
                            else -> fileExtension.toUpperCase() + " File"
                        }
                    }

                    tracker.addFile(RecentFileItem(fileName, fileType, file.path))
                }
            })
    }
}