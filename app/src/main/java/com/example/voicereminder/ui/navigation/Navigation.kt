package com.example.voicereminder.ui.navigation

enum class VoiceReminderScreen {
    Home,
    AddEdit,
    Details,
    Settings
}

sealed class NavigationDestination(val route: String) {
    object Home : NavigationDestination("home")
    object AddEdit : NavigationDestination("add_edit?reminderId={reminderId}") {
        fun createRoute(reminderId: Long? = null) = if (reminderId != null) "add_edit?reminderId=$reminderId" else "add_edit"
    }
    object Details : NavigationDestination("details/{reminderId}?autoplay={autoplay}") {
        fun createRoute(reminderId: Long, autoplay: Boolean = false) = "details/$reminderId?autoplay=$autoplay"
    }
    object Settings : NavigationDestination("settings")
}
