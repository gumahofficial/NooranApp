package com.gumah.nooran.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gumah.nooran.ui.theme.SoftGold

enum class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    EXPLORE("استكشف", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore"),
    CREATE("إنشاء", Icons.Filled.Add, Icons.Outlined.Add, "nav_create"),
    NOTIFICATIONS("الإشعارات", Icons.Filled.Notifications, Icons.Outlined.Notifications, "nav_notifications"),
    PROFILE("حسابي", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun NooranBottomNav(
    selectedItem: NavItem,
    unreadNotifsCount: Int = 0,
    onItemSelected: (NavItem) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nooran_bottom_nav"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            NavItem.values().forEach { item ->
                val isSelected = item == selectedItem
                val isCreate = item == NavItem.CREATE

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onItemSelected(item) },
                    modifier = Modifier.testTag(item.tag),
                    icon = {
                        if (isCreate) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "إنشاء محتوى",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            BadgedBox(
                                badge = {
                                    if (item == NavItem.NOTIFICATIONS && unreadNotifsCount > 0) {
                                        Badge(
                                            containerColor = SoftGold,
                                            contentColor = MaterialTheme.colorScheme.surface
                                        ) {
                                            Text(unreadNotifsCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}
