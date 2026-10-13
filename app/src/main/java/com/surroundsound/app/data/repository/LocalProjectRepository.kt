package com.surroundsound.app.data.repository

import android.content.Context
import com.surroundsound.app.domain.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LocalProjectRepository(private val context: Context) : ProjectRepository {

    private val projectsFile: File
        get() = File(context.filesDir, "projects_index.json")

    private val _projects = MutableStateFlow<List<Project>>(emptyList())

    init {
        loadProjectsFromDisk()
    }

    override fun getAllProjects(): Flow<List<Project>> = _projects.asStateFlow()

    override suspend fun getProjectById(id: String): Project? {
        return _projects.value.find { it.id == id }
    }

    override suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val current = _projects.value.toMutableList()
        val index = current.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            current[index] = project.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(0, project)
        }
        _projects.value = current
        saveProjectsToDisk(current)
    }

    override suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        val updated = _projects.value.filterNot { it.id == id }
        _projects.value = updated
        saveProjectsToDisk(updated)
    }

    private fun loadProjectsFromDisk() {
        if (!projectsFile.exists()) return
        try {
            val jsonStr = projectsFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Project>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val secondariesArray = obj.optJSONArray("secondarySourceIds")
                val secondaryIds = mutableListOf<String>()
                if (secondariesArray != null) {
                    for (j in 0 until secondariesArray.length()) {
                        secondaryIds.add(secondariesArray.getString(j))
                    }
                }
                list.add(
                    Project(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        primarySourceId = obj.getString("primarySourceId"),
                        secondarySourceIds = secondaryIds,
                        bpm = obj.optInt("bpm", 120),
                        stepCount = obj.optInt("stepCount", 16),
                        swingPercent = obj.optInt("swingPercent", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            _projects.value = list
        } catch (_: Exception) {
            _projects.value = emptyList()
        }
    }

    private fun saveProjectsToDisk(projects: List<Project>) {
        try {
            val array = JSONArray()
            for (p in projects) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("primarySourceId", p.primarySourceId)
                    val secondaries = JSONArray()
                    p.secondarySourceIds.forEach { secondaries.put(it) }
                    put("secondarySourceIds", secondaries)
                    put("creativeMode", p.creativeMode.name)
                    put("bpm", p.bpm)
                    put("stepCount", p.stepCount)
                    put("swingPercent", p.swingPercent)
                    put("createdAt", p.createdAt)
                    put("updatedAt", p.updatedAt)
                }
                array.put(obj)
            }
            projectsFile.writeText(array.toString(2))
        } catch (_: Exception) {}
    }
}
