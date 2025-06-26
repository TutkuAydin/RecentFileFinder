package com.tutkuaydin.recentfilefinder

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag

@Tag("RecentFileItem")
data class RecentFileItem(
    @Attribute("filePath") val filePath: String, // Dosyanın tam yolu
    @Attribute("name") val name: String,         // Dosyanın uzantısız adı (nameWithoutExtension)
    @Attribute("extension") val extension: String, // Dosyanın uzantısı
    @Attribute("type") val type: String,        // Dosyanın IntelliJ tarafından belirlenen türü (örneğin "Kotlin", "Java", "Text")
    @Attribute("isFavorite") var isFavorite: Boolean = false
) {

    @Suppress("unused")
    private constructor() : this("", "", "", "", false)
}
