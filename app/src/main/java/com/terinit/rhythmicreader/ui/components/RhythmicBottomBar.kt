package com.terinit.rhythmicreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terinit.rhythmicreader.app.Screen
import com.terinit.rhythmicreader.ui.theme.CharcoalMuted
import com.terinit.rhythmicreader.ui.theme.CharcoalPrimary
import com.terinit.rhythmicreader.ui.theme.SageGreenContainer
import com.terinit.rhythmicreader.ui.theme.SageGreenPrimary
import com.terinit.rhythmicreader.ui.theme.WarmOutline
import com.terinit.rhythmicreader.ui.theme.WarmSurface

@Composable
fun RhythmicBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0x1F000000))
                .clip(RoundedCornerShape(32.dp))
                .background(WarmSurface)
                .border(1.dp, WarmOutline, RoundedCornerShape(32.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "Library",
                isSelected = currentScreen is Screen.Library,
                onClick = { onNavigate(Screen.Library) }
            )

            BottomNavItem(
                icon = Icons.Default.Explore,
                label = "Focus",
                isSelected = currentScreen is Screen.Focus,
                onClick = { onNavigate(Screen.Focus) }
            )

            BottomNavItem(
                icon = Icons.Default.Person,
                label = "You",
                isSelected = currentScreen is Screen.Settings,
                onClick = { onNavigate(Screen.Settings) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = ripple(bounded = true, color = SageGreenPrimary)

    val backgroundColor = if (isSelected) SageGreenContainer.copy(alpha = 0.6f) else Color.Transparent
    val contentColor = if (isSelected) SageGreenPrimary else CharcoalMuted

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) CharcoalPrimary else CharcoalMuted
            )
        }
    }
}
