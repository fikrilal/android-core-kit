package dev.fikril.androidcorekit.core.navigation

sealed interface AppDestination : Destination {
    data object UnauthenticatedRoot : AppDestination {
        override val route: String = "unauthenticated_root"
    }

    data object AuthenticatedRoot : AppDestination {
        override val route: String = "authenticated_root"
    }
}

