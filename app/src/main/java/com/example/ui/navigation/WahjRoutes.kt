package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface WahjRoute {
    @Serializable data object Splash : WahjRoute
    @Serializable data object Welcome : WahjRoute
    @Serializable data object RoleSelection : WahjRoute
    @Serializable data object Login : WahjRoute
    @Serializable data object Register : WahjRoute
    @Serializable data object ChildDashboard : WahjRoute
    @Serializable data object ChildQrPairing : WahjRoute
    @Serializable data object ParentQrScanner : WahjRoute
    @Serializable data class PairingSuccess(val childId: String, val isChildFlow: Boolean) : WahjRoute
    @Serializable data object ParentDashboard : WahjRoute
    @Serializable data class ChildDetail(val childId: String) : WahjRoute
    @Serializable data class AppManagement(val childId: String) : WahjRoute
    @Serializable data class ScreenTime(val childId: String) : WahjRoute
    @Serializable data class Messages(val childId: String) : WahjRoute
    @Serializable data class Location(val childId: String) : WahjRoute
    @Serializable data class Reports(val childId: String) : WahjRoute
    @Serializable data object Alerts : WahjRoute
    @Serializable data object Settings : WahjRoute
    @Serializable data object Subscriptions : WahjRoute
    @Serializable data object AccountsDevices : WahjRoute
}
