package com.autoledger.domain.model

data class UserPreferences(
    val userName   : String  = "",
    val isLoggedIn : Boolean = false,
    val hasSetup   : Boolean = false,
    val photoUri   : String  = "",
    val appTheme   : String  = "OLED Black"   // ← add
)