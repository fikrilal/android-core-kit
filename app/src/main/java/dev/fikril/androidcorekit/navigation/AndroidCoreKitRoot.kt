package dev.fikril.androidcorekit.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.fikril.androidcorekit.feature.home.HomeScreen
import dev.fikril.androidcorekit.feature.home.navigation.HomeDestination
import dev.fikril.androidcorekit.feature.profile.ProfileScreen
import dev.fikril.androidcorekit.feature.profile.navigation.ProfileDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidCoreKitRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val topLevelDestinations = listOf(homeTopLevelDestination, profileTopLevelDestination)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTopLevel =
        topLevelDestinations.firstOrNull { destination -> destination.route == currentRoute }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = currentTopLevel?.label ?: "AndroidCoreKit") })
        },
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(text = destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(route = HomeDestination.route) {
                HomeScreen(onGoToProfile = { navController.navigate(ProfileDestination.route) })
            }
            composable(route = ProfileDestination.route) {
                ProfileScreen(onGoToHome = { navController.navigate(HomeDestination.route) })
            }
        }
    }
}
