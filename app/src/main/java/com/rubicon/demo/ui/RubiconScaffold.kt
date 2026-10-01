package com.rubicon.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rubicon.demo.R
import com.rubicon.demo.demo
import com.rubicon.demo.sdk.Calls
import com.rubicon.demo.ui.components.StatusPill
import com.rubicon.demo.ui.screens.CaptureScreen
import com.rubicon.demo.ui.screens.EventsScreen
import com.rubicon.demo.ui.screens.IdentityScreen
import com.rubicon.demo.ui.screens.LogsScreen
import com.rubicon.demo.ui.screens.ServerScreen
import com.rubicon.demo.ui.theme.Danger
import com.rubicon.demo.ui.theme.Success

enum class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Events("events", "Events", Icons.AutoMirrored.Filled.Send),
    Identity("identity", "Identity", Icons.Filled.Person),
    Capture("capture", "Capture", Icons.Filled.TouchApp),
    Server("server", "Server", Icons.Filled.Cloud),
    Logs("logs", "Logs", Icons.AutoMirrored.Filled.List),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RubiconScaffold(onOpenSettings: () -> Unit) {
    val app = LocalContext.current.demo
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val counts by app.feed.counts.collectAsState()
    val person by app.person.collectAsState()

    LaunchedEffect(Unit) {
        Calls.messages.collect {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(it)
        }
    }
    // A single-activity app sends its screens itself, with fixed keys (Lifecycle 3).
    LaunchedEffect(currentRoute) {
        if (app.settings.tabScreens) Tab.entries.firstOrNull { it.route == currentRoute }?.let { app.analytics.screen("tab_${it.route}") }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rubicon", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (app.startupError != null) {
                                StatusPill("SDK disabled", Danger)
                            } else {
                                StatusPill("${counts.queued} queued · ${counts.sent} sent · ${counts.droppedTotal} dropped", Success)
                            }
                            Text(person.userId ?: "anonymous", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    Icon(
                        painterResource(R.drawable.ic_rubicon_mark),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = { IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Tab.entries.forEach { tab ->
                    val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Tab.Events.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Events.route) { EventsScreen() }
            composable(Tab.Identity.route) { IdentityScreen() }
            composable(Tab.Capture.route) { CaptureScreen() }
            composable(Tab.Server.route) { ServerScreen() }
            composable(Tab.Logs.route) { LogsScreen() }
        }
    }
}
