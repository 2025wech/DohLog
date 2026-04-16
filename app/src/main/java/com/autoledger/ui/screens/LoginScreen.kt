package com.autoledger.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.autoledger.R
import com.autoledger.ui.*
import com.autoledger.ui.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    viewModel       : AuthViewModel = hiltViewModel(),
    onAuthenticated : () -> Unit
) {
    val context     = LocalContext.current
    val authUiState by viewModel.authUiState.collectAsState()
    val userPrefs   by viewModel.userPreferences.collectAsState()

    // Navigate when authenticated
    LaunchedEffect(authUiState.isAuthenticated) {
        if (authUiState.isAuthenticated) onAuthenticated()
    }

    when {
        // ── Loading DataStore ─────────────────────────────────────────
        authUiState.isLoading -> {
            BrandingScreen(isLoading = true)
        }

        // ── First launch: show branding then setup ────────────────────
        !userPrefs.hasSetup -> {
            BrandingScreen(
                isLoading = false,
                onGetStarted = {
                    // No-op — SetupScreen shown below after button tap
                }
            )
            // Show setup after branding button tap
            // We use a local state to track this
        }

        // ── Returning user: biometric ─────────────────────────────────
        else -> {
            BiometricScreen(
                userName       = userPrefs.userName,
                biometricError = authUiState.biometricError,
                onAuthenticate = {
                    launchBiometric(
                        context   = context,
                        onSuccess = { viewModel.onBiometricSuccess() },
                        onError   = { msg -> viewModel.onBiometricError(msg) }
                    )
                },
                onClearError = { viewModel.clearBiometricError() }
            )
        }
    }
}

// ── Branding screen ───────────────────────────────────────────────────────
@Composable
fun BrandingScreen(
    isLoading    : Boolean = false,
    onGetStarted : () -> Unit = {}
) {
    // Local state to flip to setup
    var showSetup by remember { mutableStateOf(false) }
    val viewModel : AuthViewModel = hiltViewModel()
    val authUiState by viewModel.authUiState.collectAsState()
    val context = LocalContext.current

    if (showSetup) {
        SetupScreen(
            nameError  = authUiState.nameError,
            onSaveName = { name -> viewModel.saveNameAndSetup(name) }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
    ) {
        // ── Wallet image — upper portion ──────────────────────────────
        Image(
            painter            = painterResource(id = R.drawable.autoledger_icon),
            contentDescription = "AutoLedger wallet",
            modifier           = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)        // top 60% of screen
                .align(Alignment.TopCenter),
            contentScale       = ContentScale.Fit
        )

        // ── Gradient fade over bottom of image ────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.Center)
                .offset(y = 60.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            TrueBlack.copy(alpha = 0f),
                            TrueBlack
                        )
                    )
                )
        )

        // ── Bottom branding content ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(TrueBlack)
                .padding(horizontal = 28.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App name
            Text(
                text          = "DohLog",
                fontSize      = 26.sp,
                fontWeight    = FontWeight.Bold,
                color         = TextPrimary,
                fontFamily    = InterFontFamily,
                letterSpacing = (-0.5).sp
            )

            // Tagline
            Text(
                text       = "Your Finances, Made Effortless.",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = SafaricomGreen,
                fontFamily = InterFontFamily,
                textAlign  = TextAlign.Center
            )

            // Subtitle
            Text(
                text       = "Track M-Pesa & Airtel Money\ntransactions automatically.",
                fontSize   = 11.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily,
                textAlign  = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(16.dp))

            // Loading or Get Started button
            if (isLoading) {
                CircularProgressIndicator(
                    color       = SafaricomGreen,
                    modifier    = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Button(
                    onClick  = { showSetup = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafaricomGreen,
                        contentColor   = TrueBlack
                    )
                ) {
                    Text(
                        text       = "Get started",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

// ── Setup screen ──────────────────────────────────────────────────────────
@Composable
fun SetupScreen(
    nameError  : String?,
    onSaveName : (String) -> Unit
) {
    var nameInput      by remember { mutableStateOf("") }
    val focusRequester =  remember { FocusRequester() }
    val focusManager   =  LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SafaricomGreen.copy(alpha = 0.12f))
                    .border(1.dp, SafaricomGreen.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter            = painterResource(id = R.drawable.autoledger_icon),
                    contentDescription = "DohLog",
                    modifier           = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale       = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text       = "Welcome to DohLog",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )
            Text(
                text       = "What should we call you?",
                fontSize   = 11.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            // Name input
            OutlinedTextField(
                value         = nameInput,
                onValueChange = { nameInput = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder   = {
                    Text(
                        "Enter your name",
                        fontSize   = 12.sp,
                        color      = TextMuted,
                        fontFamily = InterFontFamily
                    )
                },
                singleLine    = true,
                isError       = nameError != null,
                supportingText = nameError?.let { error ->
                    {
                        Text(
                            error,
                            fontSize   = 10.sp,
                            color      = ErrorRed,
                            fontFamily = InterFontFamily
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSaveName(nameInput)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = SafaricomGreen,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = SafaricomGreen,
                    errorBorderColor     = ErrorRed
                ),
                shape     = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = InterFontFamily,
                    fontSize   = 13.sp,
                    color      = TextPrimary
                )
            )

            // Continue button
            Button(
                onClick  = {
                    focusManager.clearFocus()
                    onSaveName(nameInput)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafaricomGreen,
                    contentColor   = TrueBlack
                )
            ) {
                Text(
                    text       = "Continue",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

// ── Biometric screen ──────────────────────────────────────────────────────
@Composable
fun BiometricScreen(
    userName       : String,
    biometricError : String?,
    onAuthenticate : () -> Unit,
    onClearError   : () -> Unit
) {
    LaunchedEffect(Unit) { onAuthenticate() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SafaricomGreen.copy(alpha = 0.15f))
                    .border(1.dp, SafaricomGreen.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = userName.firstOrNull()
                        ?.uppercaseChar()?.toString() ?: "A",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color      = SafaricomGreen,
                    fontFamily = InterFontFamily
                )
            }

            Text(
                text       = "Welcome back,",
                fontSize   = 11.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily
            )
            Text(
                text       = userName,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )

            Spacer(Modifier.height(8.dp))

            // Fingerprint icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SafaricomGreen.copy(alpha = 0.10f))
                    .border(1.dp, SafaricomGreen.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83D\uDEE1\uFE0F", fontSize = 26.sp, color = SafaricomGreen)
            }

            Text(
                text       = "Touch sensor or use PIN",
                fontSize   = 11.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily,
                textAlign  = TextAlign.Center
            )

            // Error
            biometricError?.let { error ->
                Text(
                    text       = error,
                    fontSize   = 11.sp,
                    color      = ErrorRed,
                    fontFamily = InterFontFamily,
                    textAlign  = TextAlign.Center
                )
            }

            Spacer(Modifier.height(4.dp))

            // Unlock button
            Button(
                onClick  = {
                    onClearError()
                    onAuthenticate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafaricomGreen,
                    contentColor   = TrueBlack
                )
            ) {
                Text(
                    text       = "Unlock DohLog",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

// ── Biometric launcher ────────────────────────────────────────────────────
fun launchBiometric(
    context   : android.content.Context,
    onSuccess : () -> Unit,
    onError   : (String) -> Unit
) {
    val activity = context as? FragmentActivity ?: run {
        onError("Auth not available")
        return
    }

    val biometricManager = BiometricManager.from(context)
    val canAuthenticate  = biometricManager.canAuthenticate(
        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    )

    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
        onError("No biometric or PIN set up on this device")
        return
    }

    val executor = ContextCompat.getMainExecutor(context)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(
            result: BiometricPrompt.AuthenticationResult
        ) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
        }

        override fun onAuthenticationError(
            errorCode  : Int,
            errString  : CharSequence
        ) {
            super.onAuthenticationError(errorCode, errString)
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                onError(errString.toString())
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            onError("Authentication failed — try again")
        }
    }

    val biometricPrompt = BiometricPrompt(activity, executor, callback)

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock DohLog")
        .setSubtitle("Use your fingerprint or PIN")
        .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        .build()

    biometricPrompt.authenticate(promptInfo)
}