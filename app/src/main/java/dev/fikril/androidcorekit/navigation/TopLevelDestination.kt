package dev.fikril.androidcorekit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import dev.fikril.androidcorekit.core.navigation.Destination
import dev.fikril.androidcorekit.feature.home.navigation.HomeDestination
import dev.fikril.androidcorekit.feature.profile.navigation.ProfileDestination

data class TopLevelDestination(
    val destination: Destination,
    val label: String,
    val icon: ImageVector,
) {
    val route: String = destination.route
}

val homeTopLevelDestination =
    TopLevelDestination(
        destination = HomeDestination,
        label = "Home",
        icon = Icons.Outlined.Home,
    )

val profileTopLevelDestination =
    TopLevelDestination(
        destination = ProfileDestination,
        label = "Profile",
        icon = Icons.Outlined.Person,
    )
