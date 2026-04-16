package com.autoledger

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Scaffold
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.autoledger.ui.AppTheme
import com.autoledger.ui.AutoledgerTheme
import com.autoledger.ui.LocalAppTheme
import com.autoledger.ui.TrueBlack
import com.autoledger.ui.WhiteBackground
import com.autoledger.ui.navigation.AutoledgerBottomBar
import com.autoledger.ui.navigation.AutoledgerNavGraph
import com.autoledger.ui.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // ── Read persisted theme from DataStore ───────────────────
            val userPrefs by authViewModel.userPreferences.collectAsState()

            // Convert saved string back to AppTheme enum
            val appTheme = when (userPrefs.appTheme) {
                "White" -> AppTheme.WHITE
                else    -> AppTheme.OLED_BLACK
            }

            CompositionLocalProvider(LocalAppTheme provides appTheme) {
                AutoledgerTheme(appTheme = appTheme) {
                    val navController = rememberNavController()
                    val scaffoldBg    = when (appTheme) {
                        AppTheme.WHITE      -> WhiteBackground
                        AppTheme.OLED_BLACK -> TrueBlack
                    }

                    Scaffold(
                        containerColor = scaffoldBg,
                        bottomBar = {
                            AutoledgerBottomBar(navController)
                        }
                    ) { _ ->
                        AutoledgerNavGraph(
                            navController = navController,
                            onThemeChange = { selectedTheme ->
                                // Save to DataStore via ViewModel
                                val themeName = when (selectedTheme) {
                                    AppTheme.WHITE      -> "White"
                                    AppTheme.OLED_BLACK -> "OLED Black"
                                }
                                authViewModel.saveAppTheme(themeName)
                            }
                        )
                    }
                }
            }
        }
    }
}