package dev.fikril.androidcorekit.core.navigation

sealed interface AppDestinations : Destination {
    data object UnauthenticatedRoot : AppDestinations {
        override val route: String = "unauthenticated_root"
    }

    data object AuthenticatedRoot : AppDestinations {
        override val route: String = "authenticated_root"
    }
}
