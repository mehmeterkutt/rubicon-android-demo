package com.rubicon.demo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rubicon.demo.R
import com.rubicon.demo.analytics.RubiconAnalytics
import com.rubicon.demo.ui.screens.DiagnosticsScreen
import com.rubicon.demo.ui.screens.EventsScreen
import com.rubicon.demo.ui.screens.IdentityScreen

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Events("events", "Events", Icons.AutoMirrored.Filled.Send),
    Identity("identity", "Identity", Icons.Filled.Person),
    Diagnostics("diagnostics", "Diagnostics", Icons.Filled.Info),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RubiconScaffold(analytics: RubiconAnalytics, onOpenSettings: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Every navigation destination is a screen view; this is where the SDK's screen() call belongs
    // in an app that does not use Activities per screen.
    LaunchedEffect(currentRoute) {
        Tab.entries.firstOrNull { it.route == currentRoute }?.let { analytics.screen(it.label) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rubicon") },
                navigationIcon = {
                    Icon(painterResource(R.drawable.ic_rubicon_mark), contentDescription = null, modifier = Modifier.padding(start = 16.dp), tint = MaterialTheme.colorScheme.primary)
                },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar {
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
            composable(Tab.Events.route) { EventsScreen(analytics) }
            composable(Tab.Identity.route) { IdentityScreen(analytics) }
            composable(Tab.Diagnostics.route) { DiagnosticsScreen(analytics) }
        }
    }
}
