package com.surroundsound.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Recorder : Screen("recorder")
    object SoundDna : Screen("sound_dna/{sourceId}") {
        fun createRoute(sourceId: String) = "sound_dna/$sourceId"
    }
    object ModeSelection : Screen("modes/{sourceId}") {
        fun createRoute(sourceId: String) = "modes/$sourceId"
    }
    object BeatEditor : Screen("editor/{projectId}") {
        fun createRoute(projectId: String) = "editor/$projectId"
    }
    object ProjectLibrary : Screen("library")
    object Export : Screen("export/{projectId}") {
        fun createRoute(projectId: String) = "export/$projectId"
    }
}
