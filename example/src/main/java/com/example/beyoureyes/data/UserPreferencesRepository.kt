package com.example.beyoureyes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val rememberMe: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {
    
    companion object {
        private val REMEMBER_ME_KEY = booleanPreferencesKey("remember_me")
    }

    // 获取用户偏好设置
    suspend fun getUserPreferences(): UserPreferences {
        return context.dataStore.data.map { preferences ->
            UserPreferences(
                rememberMe = preferences[REMEMBER_ME_KEY] ?: false
            )
        }.first()
    }

    // 更新记住我设置
    suspend fun updateRememberMe(rememberMe: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMEMBER_ME_KEY] = rememberMe
        }
    }
} 