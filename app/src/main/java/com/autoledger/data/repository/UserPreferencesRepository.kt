package com.autoledger.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.autoledger.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_USER_NAME   = stringPreferencesKey("user_name")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_HAS_SETUP   = booleanPreferencesKey("has_setup")
        val KEY_PHOTO_URI   = stringPreferencesKey("profile_photo_uri")
        val KEY_APP_THEME   = stringPreferencesKey("app_theme")   // ← add
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            userName   = prefs[KEY_USER_NAME]    ?: "",
            isLoggedIn = prefs[KEY_IS_LOGGED_IN] ?: false,
            hasSetup   = prefs[KEY_HAS_SETUP]    ?: false,
            photoUri   = prefs[KEY_PHOTO_URI]    ?: "",
            appTheme   = prefs[KEY_APP_THEME]    ?: "OLED Black"
        )
    }

    suspend fun saveUserName(name: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
            prefs[KEY_HAS_SETUP] = true
        }
    }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = loggedIn
        }
    }

    suspend fun savePhotoUri(uri: String) {
        dataStore.edit { prefs ->
            prefs[KEY_PHOTO_URI] = uri
        }
    }

    suspend fun saveAppTheme(theme: String) {    // ← add
        dataStore.edit { prefs ->
            prefs[KEY_APP_THEME] = theme
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
        }
    }
}