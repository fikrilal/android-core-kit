package dev.fikril.androidcorekit.feature.auth.navigation

import dev.fikril.androidcorekit.core.navigation.Destination

data object LoginDestination : Destination {
    override val route: String = "login"
}
