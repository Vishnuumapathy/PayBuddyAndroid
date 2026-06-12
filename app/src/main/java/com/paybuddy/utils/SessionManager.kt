package com.paybuddy.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "PayBuddySession")

class SessionManager(private val context: Context) {

    companion object {
        @Volatile
        var isAuthTransitioning: Boolean = false

        private val KEY_VENDOR_ID = stringPreferencesKey("vendorId")
        private val KEY_VENDOR_NAME = stringPreferencesKey("vendorName")
        private val KEY_SHOP_NAME = stringPreferencesKey("shopName")
        private val KEY_UPI_ID = stringPreferencesKey("upiId")
        private val KEY_LOGIN_METHOD = stringPreferencesKey("loginMethod")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("isLoggedIn")
        private val KEY_NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notificationPermissionRequested")
    }

    suspend fun saveSession(vendorId: String, name: String, shopName: String, upiId: String = "", loginMethod: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VENDOR_ID] = vendorId
            preferences[KEY_VENDOR_NAME] = name
            preferences[KEY_SHOP_NAME] = shopName
            preferences[KEY_UPI_ID] = upiId
            preferences[KEY_LOGIN_METHOD] = loginMethod
            preferences[KEY_IS_LOGGED_IN] = true
        }
    }

    suspend fun getVendorId(): String? = context.dataStore.data.map { preferences ->
        preferences[KEY_VENDOR_ID]
    }.first()

    suspend fun getVendorName(): String? = context.dataStore.data.map { preferences ->
        preferences[KEY_VENDOR_NAME]
    }.first()

    suspend fun getShopName(): String? = context.dataStore.data.map { preferences ->
        preferences[KEY_SHOP_NAME]
    }.first()

    suspend fun getVendorUpi(): String? = context.dataStore.data.map { preferences ->
        preferences[KEY_UPI_ID]
    }.first()

    suspend fun getLoginMethod(): String? = context.dataStore.data.map { preferences ->
        preferences[KEY_LOGIN_METHOD]
    }.first()

    suspend fun isLoggedIn(): Boolean = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_LOGGED_IN] ?: false
    }.first()

    suspend fun isNotificationPermissionRequested(): Boolean = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATION_PERMISSION_REQUESTED] ?: false
    }.first()

    suspend fun setNotificationPermissionRequested(requested: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_PERMISSION_REQUESTED] = requested
        }
    }

    suspend fun clearSession() {
        isAuthTransitioning = false
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
