package com.surroundsound.app.domain.model

import com.surroundsound.app.domain.mode.CreativeMode
import java.util.UUID

/**
 * Base project schema representing a user's Sound-to-Beat session.
 */
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val primarySourceId: String,
    val secondarySourceIds: List<String> = emptyList(),
    val creativeMode: CreativeMode = CreativeMode.RHYTHMIC,
    val bpm: Int = 120,
    val stepCount: Int = 16,
    val swingPercent: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val allSourceIds: List<String>
        get() = listOf(primarySourceId) + secondarySourceIds
}
