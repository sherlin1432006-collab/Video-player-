package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object LibraryRoute

@Serializable
object PlaylistsRoute

@Serializable
object SettingsRoute

@Serializable
data class PlayerRoute(
    val videoId: Long,
    val playlistId: Long? = null,
)
