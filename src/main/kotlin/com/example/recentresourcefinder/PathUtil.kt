package com.example.recentresourcefinder

import java.io.File

object PathUtil {

    /**
     * Verilen dosya yolunu belirli bir maksimum uzunluğa göre kısaltır.
     * Dosya adını öncelikli olarak görünür tutar ve dizin yolunu ortadan kısaltır.
     *
     * Örnek: "/path/to/my/project/src/main/kotlin/MyClass.kt" -> ".../src/main/kotlin/MyClass.kt"
     *
     * @param path Kısaltılacak dosya yolu.
     * @param maxLength Kısaltılmış yolun maksimum karakter uzunluğu.
     * @return Kısaltılmış dosya yolu.
     */
    fun abbreviatePath(path: String, maxLength: Int): String {
        if (path.length <= maxLength) {
            return path
        }

        val separator = File.separatorChar
        val parts = path.split(separator)

        if (parts.size <= 1) {
            return path
        }

        val fileName = parts.last()
        val directoryParts = parts.dropLast(1)

        var currentLength = fileName.length + (if (directoryParts.isNotEmpty()) 1 else 0)

        val abbreviatedDirParts = mutableListOf<String>()
        val totalDirLength = directoryParts.sumOf { it.length + 1 }

        if (totalDirLength + fileName.length <= maxLength) {
            return path
        }

        val ellipsis = "..."
        val availableLengthForDir = maxLength - fileName.length - ellipsis.length - 1 // -1 son ayırıcı için

        if (availableLengthForDir <= 0) {
            if (fileName.length <= maxLength) return fileName
            return fileName.take(maxLength - 3) + ellipsis // dosya adını da kısalt
        }

        for (i in directoryParts.indices.reversed()) {
            val part = directoryParts[i]
            if (currentLength + part.length + 1 <= maxLength - ellipsis.length) {
                abbreviatedDirParts.add(0, part)
                currentLength += part.length + 1
            } else {
                abbreviatedDirParts.add(0, ellipsis)
                break
            }
        }

        if (abbreviatedDirParts.isEmpty() && directoryParts.isNotEmpty()) {
            abbreviatedDirParts.add(ellipsis)
        }

        return "${abbreviatedDirParts.joinToString(separator.toString())}${separator}${fileName}"
    }
}