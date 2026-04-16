package com.autoledger.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.autoledger.ui.*
import com.autoledger.ui.viewmodels.AuthViewModel
import com.autoledger.ui.viewmodels.TransactionViewModel

private const val APP_VERSION      = "1.0.0"
private const val DEVELOPER_NAME   = "Waweru"
private const val DEVELOPER_EMAIL  = "wawerua239@gmail.com"
private const val DEVELOPER_GITHUB = "github.com/2025wech"

@Composable
fun ProfileScreen(
    viewModel     : TransactionViewModel = hiltViewModel(),
    authViewModel : AuthViewModel        = hiltViewModel(),
    onThemeChange : (AppTheme) -> Unit   = {},
    onLogout      : () -> Unit           = {}
) {
    val context       = LocalContext.current
    val transactions  by viewModel.transactions.collectAsState()
    val totalSent     by viewModel.totalSent.collectAsState()
    val totalReceived by viewModel.totalReceived.collectAsState()
    val userPrefs     by authViewModel.userPreferences.collectAsState()
    val uiState       by viewModel.uiState.collectAsState()

    val displayName   = userPrefs.userName.ifBlank { "User" }
    val displayLetter = displayName.firstOrNull()
        ?.uppercaseChar()?.toString() ?: "A"

    // ── Settings state ────────────────────────────────────────────────
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoSyncEnabled      by remember { mutableStateOf(true) }
    var selectedTheme        by remember { mutableStateOf("OLED Black") }

    // ── Dialog states ─────────────────────────────────────────────────
    var showThemeDialog     by remember { mutableStateOf(false) }
    var showPrivacyDialog   by remember { mutableStateOf(false) }
    var showVersionDialog   by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showLogoutDialog    by remember { mutableStateOf(false) }
    var showReimportDialog  by remember { mutableStateOf(false) }

    // ── Photo picker ──────────────────────────────────────────────────
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // ── Persist permission so URI survives app restart ────────────
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                android.util.Log.w("PROFILE_PHOTO",
                    "Could not persist URI permission: ${e.message}")
            }
            authViewModel.savePhotoUri(it.toString())
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────
    if (showThemeDialog) {
        ThemeDialog(
            currentTheme  = selectedTheme,
            onSelect      = { selectedTheme = it },
            onThemeChange = onThemeChange,
            onDismiss     = { showThemeDialog = false }
        )
    }
    if (showPrivacyDialog) {
        PrivacyDialog(onDismiss = { showPrivacyDialog = false })
    }
    if (showVersionDialog) {
        VersionDialog(onDismiss = { showVersionDialog = false })
    }
    if (showDeveloperDialog) {
        DeveloperDialog(
            context   = context,
            onDismiss = { showDeveloperDialog = false }
        )
    }
    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = { onLogout() },
            onDismiss = { showLogoutDialog = false }
        )
    }
    if (showReimportDialog) {
        AlertDialog(
            onDismissRequest = { showReimportDialog = false },
            containerColor   = CardBlack,
            title = {
                Text(
                    "Re-import SMS?",
                    color      = TextPrimary,
                    fontFamily = InterFontFamily,
                    fontSize   = 14.sp
                )
            },
            text = {
                Text(
                    "This will clear all transactions and re-read " +
                            "your SMS inbox. Use this after a parser fix.",
                    color      = TextSecondary,
                    fontFamily = InterFontFamily,
                    fontSize   = 12.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showReimportDialog = false
                    viewModel.clearAndReimport()
                }) {
                    Text(
                        "Re-import",
                        color      = SafaricomGreen,
                        fontFamily = InterFontFamily,
                        fontSize   = 12.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showReimportDialog = false }) {
                    Text(
                        "Cancel",
                        color      = TextSecondary,
                        fontFamily = InterFontFamily,
                        fontSize   = 12.sp
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        // ── Top bar ───────────────────────────────────────────────────
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Profile",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )
        }

        // ── Avatar + name ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tappable avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SafaricomGreen.copy(alpha = 0.15f))
                    .border(2.dp, SafaricomGreen.copy(alpha = 0.4f), CircleShape)
                    .clickable { photoPickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (userPrefs.photoUri.isNotEmpty()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(android.net.Uri.parse(userPrefs.photoUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile photo",
                        modifier           = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale       = ContentScale.Crop,
                        onError            = {
                            // URI expired — clear it so initial shows instead
                            android.util.Log.w("PROFILE_PHOTO", "Failed to load URI")
                        }
                    )
                } else {
                    Text(
                        displayLetter,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = SafaricomGreen,
                        fontFamily = InterFontFamily
                    )
                }
                // Camera badge
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(SafaricomGreen)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.CameraAlt,
                        contentDescription = "Change photo",
                        tint               = TrueBlack,
                        modifier           = Modifier.size(12.dp)
                    )
                }
            }

            Text(
                displayName,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )
            Text(
                "DohLog user",
                fontSize   = 11.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Stats row ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileStatCard(
                label    = "Transactions",
                value    = "${transactions.size}",
                modifier = Modifier.weight(1f)
            )
            ProfileStatCard(
                label    = "Total in",
                value    = "KES ${"%,.0f".format(totalReceived)}",
                color    = SafaricomGreen,
                modifier = Modifier.weight(1f)
            )
            ProfileStatCard(
                label    = "Total out",
                value    = "KES ${"%,.0f".format(totalSent)}",
                color    = ErrorRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Settings ──────────────────────────────────────────────────
        SectionLabel("Settings")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceBlack)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
        ) {
            ToggleMenuItem(
                icon     = Icons.Rounded.Notifications,
                label    = "Notifications",
                subtitle = "New transaction alerts",
                checked  = notificationsEnabled,
                onToggle = { notificationsEnabled = it }
            )
            MenuDivider()
            ToggleMenuItem(
                icon     = Icons.Rounded.Sync,
                label    = "Auto-sync SMS",
                subtitle = "Capture transactions automatically",
                checked  = autoSyncEnabled,
                onToggle = { autoSyncEnabled = it }
            )
            MenuDivider()
            ActionMenuItem(
                icon     = Icons.Rounded.Security,
                label    = "Privacy",
                subtitle = "Data usage & permissions",
                value    = "",
                onClick  = { showPrivacyDialog = true }
            )
            MenuDivider()
            ActionMenuItem(
                icon     = Icons.Rounded.DarkMode,
                label    = "Theme",
                subtitle = "App appearance",
                value    = selectedTheme,
                onClick  = { showThemeDialog = true }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── About ─────────────────────────────────────────────────────
        SectionLabel("About")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceBlack)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
        ) {
            ActionMenuItem(
                icon     = Icons.Rounded.Info,
                label    = "Version",
                subtitle = "What's new in this release",
                value    = APP_VERSION,
                onClick  = { showVersionDialog = true }
            )
            MenuDivider()
            ActionMenuItem(
                icon     = Icons.Rounded.Person,
                label    = "Developer",
                subtitle = DEVELOPER_NAME,
                value    = "",
                onClick  = { showDeveloperDialog = true }
            )
            MenuDivider()
            ActionMenuItem(
                icon     = Icons.Rounded.Phone,
                label    = "Supported providers",
                subtitle = "M-Pesa & Airtel Money",
                value    = "2",
                onClick  = {}
            )
            MenuDivider()
            ActionMenuItem(
                icon    = Icons.Rounded.Star,
                label   = "Rate DohLog",
                subtitle = "Enjoying the app? Leave a review",
                value   = "",
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.autoledger")
                    )
                    context.startActivity(intent)
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Logout button ─────────────────────────────────────────────
        Button(
            onClick  = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(44.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ErrorRed.copy(alpha = 0.12f),
                contentColor   = ErrorRed
            )
        ) {
            Icon(
                Icons.Rounded.ExitToApp,
                contentDescription = null,
                modifier           = Modifier.size(15.dp),
                tint               = ErrorRed
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Log out",
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color      = ErrorRed
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Re-import button ──────────────────────────────────────────
        TextButton(
            onClick  = { showReimportDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Re-import SMS",
                fontSize   = 11.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Footer ────────────────────────────────────────────────────
        Text(
            text       = "DohLog v$APP_VERSION • Made in Kenya 🇰🇪",
            fontSize   = 10.sp,
            color      = TextMuted,
            fontFamily = InterFontFamily,
            textAlign  = TextAlign.Center,
            modifier   = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}

// ── Section label ─────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        fontSize   = 11.sp,
        color      = TextSecondary,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        modifier   = Modifier.padding(
            start = 20.dp, end = 20.dp,
            top   = 4.dp,  bottom = 8.dp
        )
    )
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        color    = BorderSubtle,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

// ── Toggle menu item ──────────────────────────────────────────────────────
@Composable
private fun ToggleMenuItem(
    icon     : ImageVector,
    label    : String,
    subtitle : String,
    checked  : Boolean,
    onToggle : (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MenuIcon(icon)
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                label,
                fontSize   = 12.sp,
                color      = TextPrimary,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                fontSize   = 10.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily
            )
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            modifier        = Modifier.height(24.dp),
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = TrueBlack,
                checkedTrackColor   = SafaricomGreen,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = BorderSubtle
            )
        )
    }
}

// ── Action menu item ──────────────────────────────────────────────────────
@Composable
private fun ActionMenuItem(
    icon     : ImageVector,
    label    : String,
    subtitle : String,
    value    : String,
    onClick  : () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MenuIcon(icon)
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                label,
                fontSize   = 12.sp,
                color      = TextPrimary,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                fontSize   = 10.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily
            )
        }
        if (value.isNotEmpty()) {
            Text(
                value,
                fontSize   = 10.sp,
                color      = SafaricomGreen,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(4.dp))
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint               = TextMuted,
            modifier           = Modifier.size(14.dp)
        )
    }
}

// ── Menu icon ─────────────────────────────────────────────────────────────
@Composable
private fun MenuIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SafaricomGreen.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint               = SafaricomGreen,
            modifier           = Modifier.size(15.dp)
        )
    }
}

// ── Stat card ─────────────────────────────────────────────────────────────
@Composable
private fun ProfileStatCard(
    label    : String,
    value    : String,
    color    : Color    = TextPrimary,
    modifier : Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceBlack)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            value,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = color,
            fontFamily = InterFontFamily,
            maxLines   = 1
        )
        Text(
            label,
            fontSize   = 9.sp,
            color      = TextSecondary,
            fontFamily = InterFontFamily
        )
    }
}

// ── Theme dialog — 2 themes only ──────────────────────────────────────────
@Composable
private fun ThemeDialog(
    currentTheme  : String,
    onSelect      : (String) -> Unit,
    onThemeChange : (AppTheme) -> Unit,
    onDismiss     : () -> Unit
) {
    val themes = listOf(
        Triple("OLED Black", "Pure black — perfect for AMOLED screens",
            AppTheme.OLED_BLACK),
        Triple("White",      "Clean white — great for daytime use",
            AppTheme.WHITE)
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBlack)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Choose theme",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )
            Text(
                "Select your preferred appearance",
                fontSize   = 11.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily
            )

            Spacer(Modifier.height(4.dp))

            themes.forEach { (name, description, appTheme) ->
                val isSelected = currentTheme == name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected)
                                SafaricomGreen.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected)
                                SafaricomGreen.copy(alpha = 0.3f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            onSelect(name)
                            onThemeChange(appTheme)
                            onDismiss()
                        }
                        .padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Preview circle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (appTheme == AppTheme.OLED_BLACK)
                                    Color(0xFF000000)
                                else
                                    Color(0xFFFFFFFF)
                            )
                            .border(
                                1.dp,
                                SafaricomGreen.copy(alpha = 0.4f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint     = SafaricomGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(SafaricomGreen)
                            )
                        }
                    }

                    Column(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            name,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = if (isSelected) SafaricomGreen
                            else TextPrimary,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            description,
                            fontSize   = 10.sp,
                            color      = TextSecondary,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }

            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    "Cancel",
                    fontSize   = 12.sp,
                    color      = TextSecondary,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

// ── Privacy dialog ────────────────────────────────────────────────────────
@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBlack)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Privacy & data",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )
            PrivacyItem(
                "SMS access",
                "DohLog reads only M-Pesa and Airtel Money SMS. " +
                        "No other messages are accessed or stored."
            )
            PrivacyItem(
                "Local storage only",
                "All your transaction data is stored locally on your " +
                        "device. Nothing is sent to any server."
            )
            PrivacyItem(
                "No ads, no tracking",
                "DohLog contains no advertising, no analytics " +
                        "SDKs, and no third-party data collection."
            )
            PrivacyItem(
                "Biometric security",
                "Your biometric data never leaves your device. " +
                        "We use Android's secure biometric API only."
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = SafaricomGreen,
                    contentColor   = TrueBlack
                )
            ) {
                Text(
                    "Got it",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun PrivacyItem(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = SafaricomGreen,
            fontFamily = InterFontFamily
        )
        Text(
            body,
            fontSize   = 11.sp,
            color      = TextSecondary,
            fontFamily = InterFontFamily,
            lineHeight = 16.sp
        )
    }
}

// ── Version dialog ────────────────────────────────────────────────────────
@Composable
private fun VersionDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBlack)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SafaricomGreen.copy(alpha = 0.12f))
                    .border(1.dp, SafaricomGreen.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "DL",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = SafaricomGreen,
                    fontFamily = InterFontFamily
                )
            }
            Text(
                "DohLog",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )
            Text(
                "Version $APP_VERSION",
                fontSize   = 12.sp,
                color      = SafaricomGreen,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
            HorizontalDivider(color = BorderSubtle)
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "What's new",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    fontFamily = InterFontFamily
                )
                ChangelogItem("Real-time M-Pesa & Airtel SMS parsing")
                ChangelogItem("Biometric login with fingerprint & PIN")
                ChangelogItem("Daily, weekly & monthly analytics")
                ChangelogItem("OLED true black theme")
                ChangelogItem("Separate M-Pesa & Airtel balances")
                ChangelogItem("Profile photo support")
            }
            HorizontalDivider(color = BorderSubtle)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Android ${Build.VERSION.RELEASE}",
                    fontSize   = 10.sp,
                    color      = TextMuted,
                    fontFamily = InterFontFamily
                )
                Text(
                    "Build $APP_VERSION",
                    fontSize   = 10.sp,
                    color      = TextMuted,
                    fontFamily = InterFontFamily
                )
            }
            Button(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = SafaricomGreen,
                    contentColor   = TrueBlack
                )
            ) {
                Text(
                    "Close",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun ChangelogItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(SafaricomGreen)
        )
        Text(
            text,
            fontSize   = 11.sp,
            color      = TextSecondary,
            fontFamily = InterFontFamily,
            lineHeight = 16.sp
        )
    }
}

// ── Developer dialog ──────────────────────────────────────────────────────
@Composable
private fun DeveloperDialog(
    context   : Context,
    onDismiss : () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBlack)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SafaricomGreen.copy(alpha = 0.15f))
                    .border(1.dp, SafaricomGreen.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    DEVELOPER_NAME.first().uppercaseChar().toString(),
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = SafaricomGreen,
                    fontFamily = InterFontFamily
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    DEVELOPER_NAME,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    fontFamily = InterFontFamily
                )
                Text(
                    "Android Developer",
                    fontSize   = 11.sp,
                    color      = SafaricomGreen,
                    fontFamily = InterFontFamily
                )
                Text(
                    "+254740595867",
                    fontSize   = 11.sp,
                    color      = TextSecondary,
                    fontFamily = InterFontFamily
                )
            }
            HorizontalDivider(color = BorderSubtle)
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContactButton(
                    icon    = Icons.Rounded.Email,
                    label   = "Send email",
                    value   = DEVELOPER_EMAIL,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$DEVELOPER_EMAIL")
                            putExtra(Intent.EXTRA_SUBJECT, "DohLog Feedback")
                        }
                        context.startActivity(
                            Intent.createChooser(intent, "Send email")
                        )
                    }
                )
                ContactButton(
                    icon    = Icons.Rounded.Code,
                    label   = "GitHub",
                    value   = DEVELOPER_GITHUB,
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://$DEVELOPER_GITHUB")
                        )
                        context.startActivity(intent)
                    }
                )
            }
            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    "Close",
                    fontSize   = 12.sp,
                    color      = TextSecondary,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun ContactButton(
    icon    : ImageVector,
    label   : String,
    value   : String,
    onClick : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceBlack)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint               = SafaricomGreen,
            modifier           = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize   = 11.sp,
                color      = TextPrimary,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                fontSize   = 10.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily
            )
        }
        Icon(
            Icons.Rounded.OpenInNew,
            contentDescription = null,
            tint               = TextMuted,
            modifier           = Modifier.size(12.dp)
        )
    }
}

// ── Logout dialog ─────────────────────────────────────────────────────────
@Composable
private fun LogoutDialog(
    onConfirm : () -> Unit,
    onDismiss : () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBlack)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ErrorRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ExitToApp,
                    contentDescription = null,
                    tint               = ErrorRed,
                    modifier           = Modifier.size(22.dp)
                )
            }
            Text(
                "Log out?",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )
            Text(
                "You will need to use your fingerprint\nor PIN to log back in.",
                fontSize   = 11.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily,
                textAlign  = TextAlign.Center,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    )
                ) {
                    Text(
                        "Cancel",
                        fontSize   = 12.sp,
                        fontFamily = InterFontFamily
                    )
                }
                Button(
                    onClick  = onConfirm,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor   = Color.White
                    )
                ) {
                    Text(
                        "Log out",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}