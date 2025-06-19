package com.example.recentresourcefinder

data class ResourceItem(val name: String, val type: String, val filePath: String)

class ResourceTrackerService {
    private val recentResources = mutableListOf<ResourceItem>()
    private val maxSize = 10

    fun addResource(resource: ResourceItem) {
        recentResources.removeIf { it.name == resource.name && it.type == resource.type }
        recentResources.add(0, resource)
        if (recentResources.size > maxSize) recentResources.removeLast()
    }

    fun getRecentResources(): List<ResourceItem> = recentResources.toList()
}