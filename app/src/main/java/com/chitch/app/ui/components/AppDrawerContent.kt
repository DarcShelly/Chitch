package com.chitch.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chitch.app.ui.nav.Destinations

@Composable
fun AppDrawerContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.systemBarsPadding()) {
        Text(
            text = "Chitch",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(24.dp)
        )
        HorizontalDivider()

        NavigationDrawerItem(
            label = { Text("Home") },
            icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            selected = currentRoute == Destinations.HOME,
            onClick = { onNavigate(Destinations.HOME); onCloseDrawer() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        NavigationDrawerItem(
            label = { Text("Routines") },
            icon = { Icon(Icons.Outlined.Repeat, contentDescription = null) },
            selected = currentRoute == Destinations.ROUTINES_LIST,
            onClick = { onNavigate(Destinations.ROUTINES_LIST); onCloseDrawer() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        NavigationDrawerItem(
            label = { Text("Habits") },
            icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
            selected = currentRoute == Destinations.HABITS,
            onClick = { onNavigate(Destinations.HABITS); onCloseDrawer() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        NavigationDrawerItem(
            label = { Text("Tasks") },
            icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            selected = currentRoute == Destinations.TASKS_LIST,
            onClick = { onNavigate(Destinations.TASKS_LIST); onCloseDrawer() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text("Daily Review") },
            icon = { Icon(Icons.Outlined.RateReview, contentDescription = null) },
            selected = currentRoute == Destinations.DAILY_REVIEW,
            onClick = { onNavigate(Destinations.DAILY_REVIEW); onCloseDrawer() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        NavigationDrawerItem(
            label = { Text("Summary") },
            icon = { Icon(Icons.Outlined.Insights, contentDescription = null) },
            selected = currentRoute == Destinations.SUMMARY,
            onClick = { onNavigate(Destinations.SUMMARY); onCloseDrawer() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        NavigationDrawerItem(
            label = { Text("Stats") },
            icon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
            selected = currentRoute == Destinations.STATS,
            onClick = { onNavigate(Destinations.STATS); onCloseDrawer() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        NavigationDrawerItem(
            label = { Text("Settings") },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            selected = currentRoute == Destinations.SETTINGS,
            onClick = { onNavigate(Destinations.SETTINGS); onCloseDrawer() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
