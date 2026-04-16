package com.autoledger.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.autoledger.ui.*

data class BottomNavItem(
    val label : String,
    val icon  : ImageVector,
    val route : String
)

val bottomNavItems = listOf(
    BottomNavItem("Home",         Icons.Rounded.Home,     NavRoutes.Dashboard.route),
    BottomNavItem("Transactions", Icons.Rounded.List,     NavRoutes.Transactions.route),
    BottomNavItem("Analytics",    Icons.Rounded.BarChart, NavRoutes.Analytics.route),
    BottomNavItem("Profile",      Icons.Rounded.Person,   NavRoutes.Profile.route),
)

@Composable
fun AutoledgerBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide on login, permission and detail screens
    if (currentRoute == NavRoutes.Login.route        ||
        currentRoute == NavRoutes.Permission.route   ||
        currentRoute?.startsWith("detail/") == true) return

    // ── Outer container — true black, fixed height ────────────────────
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)                          // ← fixed compact height
            .background(TrueBlack)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavItem(
                item       = item,
                isSelected = isSelected,
                onClick    = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(NavRoutes.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun NavItem(
    item       : BottomNavItem,
    isSelected : Boolean,
    onClick    : () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Icon
        Icon(
            imageVector        = item.icon,
            contentDescription = item.label,
            tint               = if (isSelected) SafaricomGreen else TextMuted,
            modifier           = Modifier.size(17.dp)
        )

        // Label — always horizontal, never vertical
        Text(
            text       = item.label,
            fontSize   = 8.5.sp,
            maxLines   = 1,                         // ← force single line
            overflow   = TextOverflow.Clip,         // ← never wrap or rotate
            color      = if (isSelected) SafaricomGreen else TextMuted,
            fontFamily = InterFontFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )

        // Active indicator dot
        Box(
            modifier = Modifier
                .size(if (isSelected) 3.dp else 0.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) SafaricomGreen else Color.Transparent
                )
        )
    }
}