package com.autoledger.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.autoledger.ui.*
import com.autoledger.ui.viewmodels.TransactionViewModel

@Composable
fun PermissionScreen(
    viewModel          : TransactionViewModel = hiltViewModel(),
    onPermissionGranted: () -> Unit
) {
    val context          = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    var isImporting      by remember { mutableStateOf(false) }
    val uiState          by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        val alreadyGranted =
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECEIVE_SMS
                    ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            isImporting = true
            viewModel.importSmsHistory()
            onPermissionGranted()
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && isImporting) {
            isImporting = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted =
            permissions[Manifest.permission.READ_SMS]    == true &&
                    permissions[Manifest.permission.RECEIVE_SMS] == true
        if (granted) {
            isImporting = true
            viewModel.importSmsHistory()
            onPermissionGranted()
        } else {
            permissionDenied = true
        }
    }

    // ── Full screen scrollable so button is never cut off ─────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
            .verticalScroll(rememberScrollState())   // ← scrollable
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        // ── Logo ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(SafaricomGreen.copy(alpha = 0.12f))
                .border(1.dp, SafaricomGreen.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "HEY👋",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = SafaricomGreen,
                fontFamily = InterFontFamily
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── App name ──────────────────────────────────────────────────
        Text(
            text       = "DohLog",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            fontFamily = InterFontFamily
        )

        Spacer(Modifier.height(6.dp))

        // ── Subtitle ──────────────────────────────────────────────────
        Text(
            text       = "Your smart M-Pesa & Airtel money tracker",
            fontSize   = 11.sp,
            color      = TextSecondary,
            fontFamily = InterFontFamily,
            textAlign  = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(Modifier.height(24.dp))

        // ── Permission card ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceBlack)
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text       = "Permissions needed",
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )
            PermissionRow(
                title       = "Read SMS",
                description = "Import your M-Pesa & Airtel transaction history"
            )
            PermissionRow(
                title       = "Receive SMS",
                description = "Auto-capture new transactions as they arrive"
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Importing indicator ───────────────────────────────────────
        if (isImporting) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.padding(vertical = 4.dp)
            ) {
                CircularProgressIndicator(
                    color       = SafaricomGreen,
                    modifier    = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp
                )
                Text(
                    text       = "Reading your SMS history...",
                    fontSize   = 10.sp,
                    color      = SafaricomGreen,
                    fontFamily = InterFontFamily
                )
            }
        }

        // ── Import success ────────────────────────────────────────────
        if (uiState.importCount > 0) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(SafaricomGreen)
                )
                Text(
                    text       = "Imported ${uiState.importCount} transactions",
                    fontSize   = 10.sp,
                    color      = SafaricomGreen,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Permission denied ─────────────────────────────────────────
        if (permissionDenied) {
            Spacer(Modifier.height(4.dp))
            Text(
                text       = "SMS permission is required to track your transactions.",
                fontSize   = 10.sp,
                color      = ErrorRed,
                textAlign  = TextAlign.Center,
                fontFamily = InterFontFamily
            )
        }

        // ── ViewModel error ───────────────────────────────────────────
        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(4.dp))
            Text(
                text       = error,
                fontSize   = 10.sp,
                color      = ErrorRed,
                textAlign  = TextAlign.Center,
                fontFamily = InterFontFamily
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Grant button — always fully visible ───────────────────────
        Button(
            onClick = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS
                    )
                )
            },
            enabled  = !isImporting,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = SafaricomGreen,
                contentColor           = TrueBlack,
                disabledContainerColor = SafaricomGreen.copy(alpha = 0.4f),
                disabledContentColor   = TrueBlack.copy(alpha = 0.5f)
            )
        ) {
            if (isImporting) {
                CircularProgressIndicator(
                    color       = TrueBlack,
                    modifier    = Modifier.size(15.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text       = "Grant permission & continue",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    maxLines   = 1
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Permission row ────────────────────────────────────────────────────────
@Composable
private fun PermissionRow(title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(SafaricomGreen)
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text       = title,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )
            Text(
                text       = description,
                fontSize   = 10.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily,
                lineHeight = 14.sp
            )
        }
    }
}