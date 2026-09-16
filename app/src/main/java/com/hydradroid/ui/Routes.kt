package com.hydradroid.ui

object Routes {
    const val HOME = "home"
    const val CATALOGUE = "catalogue"
    const val LIBRARY = "library"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val GAME = "game/{shop}/{objectId}"
    const val PROFILE = "profile/{userId}"
    const val ACHIEVEMENTS = "achievements?objectId={objectId}&shop={shop}"
    const val NOTIFICATIONS = "notifications"
    const val AUTH_SOON = "auth-soon"

    fun game(shop: String, objectId: String) = "game/$shop/$objectId"
    fun profile(userId: String) = "profile/$userId"
}
