package com.akvo.externalodk.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.akvo.externalodk.ui.screen.DownloadCompleteScreen
import com.akvo.externalodk.ui.screen.HomeDashboardScreen
import com.akvo.externalodk.ui.screen.LoadingScreen
import com.akvo.externalodk.ui.screen.LoginScreen
import com.akvo.externalodk.ui.screen.SyncCompleteScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Login,
        modifier = modifier
    ) {
        composable<Login> {
            LoginScreen(
                onDownloadStart = {
                    navController.navigate(Loading(LoadingType.DOWNLOAD))
                }
            )
        }

        composable<Loading> { backStackEntry ->
            val route = backStackEntry.toRoute<Loading>()
            val message = when (route.type) {
                LoadingType.DOWNLOAD -> "Downloading data..."
                LoadingType.RESYNC -> "Syncing data..."
            }
            LoadingScreen(
                message = message,
                onLoadingComplete = {
                    when (route.type) {
                        LoadingType.DOWNLOAD -> {
                            // Mock data for download complete
                            navController.navigate(
                                DownloadComplete(
                                    totalEntries = 42,
                                    latestSubmissionDate = "2026-01-21 09:30"
                                )
                            ) {
                                popUpTo(Login) { inclusive = true }
                            }
                        }
                        LoadingType.RESYNC -> {
                            // Mock data for sync complete
                            navController.navigate(
                                SyncComplete(
                                    addedRecords = 5,
                                    updatedRecords = 3,
                                    latestRecordTimestamp = "2026-01-21 10:45"
                                )
                            ) {
                                popUpTo(Home)
                            }
                        }
                    }
                }
            )
        }

        composable<DownloadComplete> { backStackEntry ->
            val route = backStackEntry.toRoute<DownloadComplete>()
            DownloadCompleteScreen(
                totalEntries = route.totalEntries,
                latestSubmissionDate = route.latestSubmissionDate,
                onViewData = {
                    navController.navigate(Home) {
                        popUpTo<DownloadComplete> { inclusive = true }
                    }
                },
                onResyncData = {
                    navController.navigate(Loading(LoadingType.RESYNC))
                }
            )
        }

        composable<Home> {
            HomeDashboardScreen(
                onResyncClick = {
                    navController.navigate(Loading(LoadingType.RESYNC))
                },
                onLogout = {
                    navController.navigate(Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<SyncComplete> { backStackEntry ->
            val route = backStackEntry.toRoute<SyncComplete>()
            SyncCompleteScreen(
                addedRecords = route.addedRecords,
                updatedRecords = route.updatedRecords,
                latestRecordTimestamp = route.latestRecordTimestamp,
                onReturnToDashboard = {
                    navController.navigate(Home) {
                        popUpTo(Home) { inclusive = true }
                    }
                }
            )
        }
    }
}
