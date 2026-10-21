package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomSheet(
    userProfile: UserProfile?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onMyProfileClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onReferEarnClick: () -> Unit,
    onAccountConnectClick: () -> Unit,
    onTaskHistoryClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Profile Summary Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                if (!userProfile?.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userProfile?.photoUrl,
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(DarkCardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = GoldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userProfile?.displayName?.ifBlank { "Quest Gamer" } ?: "Quest Gamer",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = userProfile?.email?.ifBlank { "Signed in with Google" } ?: "",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val uidShort = if ((userProfile?.uid?.length ?: 0) > 12) {
                            "${userProfile?.uid?.take(8)}...${userProfile?.uid?.takeLast(4)}"
                        } else {
                            userProfile?.uid ?: ""
                        }
                        Text(
                            text = "UID: $uidShort",
                            fontSize = 11.sp,
                            color = GoldPrimary
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("UID", userProfile?.uid ?: "")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "UID copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy UID",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = DividerDefaults.color.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Menu Items
            ProfileMenuItem(
                icon = Icons.Default.AccountCircle,
                title = "My Profile",
                tag = "menu_my_profile",
                onClick = onMyProfileClick
            )
            ProfileMenuItem(
                icon = Icons.Default.Share,
                title = "Refer and Earn",
                subtitle = "Earn 100 Coins + 10 Gems per friend",
                tag = "menu_refer_earn",
                onClick = onReferEarnClick
            )
            ProfileMenuItem(
                icon = Icons.Default.History,
                title = "Task & Reward History",
                tag = "menu_task_history",
                onClick = onTaskHistoryClick
            )
            ProfileMenuItem(
                icon = Icons.Default.Link,
                title = "Account Connect",
                tag = "menu_account_connect",
                onClick = onAccountConnectClick
            )
            ProfileMenuItem(
                icon = Icons.Default.Notifications,
                title = "Notification Settings",
                tag = "menu_notifications",
                onClick = onNotificationSettingsClick
            )
            ProfileMenuItem(
                icon = Icons.Default.Policy,
                title = "Privacy Policy",
                tag = "menu_privacy_policy",
                onClick = onPrivacyPolicyClick
            )

            HorizontalDivider(
                color = DividerDefaults.color.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Logout
            ProfileMenuItem(
                icon = Icons.Default.ExitToApp,
                title = "Logout",
                titleColor = Color(0xFFEF4444),
                iconColor = Color(0xFFEF4444),
                tag = "menu_logout",
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = TextPrimaryDark,
    iconColor: Color = GoldPrimary,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextSecondaryDark.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}
