package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.local.ReelFlowDatabase
import com.example.data.model.VideoSourceType
import com.example.data.prefs.PlayerPreferencesRepository
import com.example.data.repository.VideoRepository
import com.example.player.PlayerViewModel
import com.example.ui.navigation.LibraryRoute
import com.example.ui.navigation.PlayerRoute
import com.example.ui.navigation.PlaylistsRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ReelFlowTheme
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.PlaylistsViewModel
import com.example.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private var pendingExternalUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractViewUri(intent)?.let { pendingExternalUri = it }

        setContent {
            val app = application
            val database = remember { ReelFlowDatabase.getInstance(app) }
            val videoRepository = remember {
                VideoRepository(
                    context = app,
                    videoDao = database.videoDao(),
                    playlistDao = database.playlistDao(),
                )
            }
            val preferencesRepository = remember { PlayerPreferencesRepository(app) }

            val libraryViewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.Factory(videoRepository),
            )
            val playlistsViewModel: PlaylistsViewModel = viewModel(
                factory = PlaylistsViewModel.Factory(videoRepository),
            )
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(preferencesRepository, videoRepository),
            )
            val playerViewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.Factory(app, videoRepository, preferencesRepository),
            )

            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            ReelFlowTheme(themeMode = settingsState.preferences.themeMode) {
                ReelFlowAppShell(
                    pendingExternalUri = pendingExternalUri,
                    onConsumedExternalUri = { pendingExternalUri = null },
                    libraryViewModel = libraryViewModel,
                    playlistsViewModel = playlistsViewModel,
                    settingsViewModel = settingsViewModel,
                    playerViewModel = playerViewModel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractViewUri(intent)?.let { pendingExternalUri = it }
    }

    private fun extractViewUri(intent: Intent?): Uri? {
        return if (intent?.action == Intent.ACTION_VIEW) intent.data else null
    }
}

private data class TopLevelNavItem(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
    val testTag: String,
)

@Composable
private fun ReelFlowAppShell(
    pendingExternalUri: Uri?,
    onConsumedExternalUri: () -> Unit,
    libraryViewModel: LibraryViewModel,
    playlistsViewModel: PlaylistsViewModel,
    settingsViewModel: SettingsViewModel,
    playerViewModel: PlayerViewModel,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playlistsState by playlistsViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val isPlayerRoute = currentDestination?.hasRoute<PlayerRoute>() == true

    LaunchedEffect(pendingExternalUri) {
        val uri = pendingExternalUri ?: return@LaunchedEffect
        libraryViewModel.importLocalVideos(listOf(uri), VideoSourceType.LOCAL_FILE) { importedId ->
            navController.navigate(PlayerRoute(videoId = importedId, playlistId = null))
        }
        onConsumedExternalUri()
    }

    val navItems = remember {
        listOf(
            TopLevelNavItem(
                labelRes = R.string.nav_library,
                selectedIcon = Icons.Filled.VideoLibrary,
                unselectedIcon = Icons.Outlined.VideoLibrary,
                route = LibraryRoute,
                testTag = "nav_library_tab",
            ),
            TopLevelNavItem(
                labelRes = R.string.nav_playlists,
                selectedIcon = Icons.AutoMirrored.Filled.PlaylistPlay,
                unselectedIcon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                route = PlaylistsRoute,
                testTag = "nav_playlists_tab",
            ),
            TopLevelNavItem(
                labelRes = R.string.nav_settings,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                route = SettingsRoute,
                testTag = "nav_settings_tab",
            ),
        )
    }

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isPlayerRoute) {
            AtmosphericBackdrop()
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = if (isPlayerRoute) {
                    WindowInsets(0, 0, 0, 0)
                } else {
                    WindowInsets.statusBars
                },
                bottomBar = {
                    if (!isPlayerRoute && !useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            windowInsets = WindowInsets.navigationBars,
                        ) {
                            navItems.forEach { item ->
                                val selected = when (item.route) {
                                    is LibraryRoute -> currentDestination?.hasRoute<LibraryRoute>() == true
                                    is PlaylistsRoute -> currentDestination?.hasRoute<PlaylistsRoute>() == true
                                    is SettingsRoute -> currentDestination?.hasRoute<SettingsRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes),
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    modifier = Modifier.testTag(item.testTag),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (!isPlayerRoute && useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                        ) {
                            navItems.forEach { item ->
                                val selected = when (item.route) {
                                    is LibraryRoute -> currentDestination?.hasRoute<LibraryRoute>() == true
                                    is PlaylistsRoute -> currentDestination?.hasRoute<PlaylistsRoute>() == true
                                    is SettingsRoute -> currentDestination?.hasRoute<SettingsRoute>() == true
                                    else -> false
                                }
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes),
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    modifier = Modifier.testTag(item.testTag),
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = LibraryRoute,
                        modifier = Modifier.weight(1f),
                    ) {
                        composable<LibraryRoute> {
                            LibraryScreen(
                                uiState = libraryState,
                                libraryViewModel = libraryViewModel,
                                playlistsViewModel = playlistsViewModel,
                                onPlayVideo = { videoId ->
                                    navController.navigate(PlayerRoute(videoId = videoId, playlistId = null))
                                },
                            )
                        }
                        composable<PlaylistsRoute> {
                            PlaylistsScreen(
                                uiState = playlistsState,
                                viewModel = playlistsViewModel,
                                onPlayFromPlaylist = { videoId, playlistId ->
                                    navController.navigate(PlayerRoute(videoId = videoId, playlistId = playlistId))
                                },
                            )
                        }
                        composable<SettingsRoute> {
                            SettingsScreen(
                                uiState = settingsState,
                                viewModel = settingsViewModel,
                            )
                        }
                        composable<PlayerRoute> { entry ->
                            val route = entry.toRoute<PlayerRoute>()
                            PlayerScreen(
                                videoId = route.videoId,
                                playlistId = route.playlistId,
                                doubleTapSeekSeconds = settingsState.preferences.doubleTapSeekSeconds,
                                playerViewModel = playerViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AtmosphericBackdrop() {
    val bg = MaterialTheme.colorScheme.background
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
    val tertiaryGlow = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = bg)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryGlow, Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.08f),
                radius = size.maxDimension * 0.55f,
            ),
            center = Offset(size.width * 0.15f, size.height * 0.08f),
            radius = size.maxDimension * 0.55f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryGlow, Color.Transparent),
                center = Offset(size.width * 0.9f, size.height * 0.72f),
                radius = size.maxDimension * 0.5f,
            ),
            center = Offset(size.width * 0.9f, size.height * 0.72f),
            radius = size.maxDimension * 0.5f,
        )
    }
}
