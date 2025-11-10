// app/src/main/java/com/example/nussocprint/util/EncryptedDataStore.kt
package com.example.nussocprint.util

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "socprint_settings")

object EncryptedDataStore {
    private const val KEYSET_PREF_NAME = "socprint_encryption_keyset"
    private const val MASTER_KEY_URI = "android-keystore://socprint_master_key"

    private var aead: Aead? = null
    fun init(context: Context) {
        AeadConfig.register()

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "socprint_keyset", KEYSET_PREF_NAME)
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        aead = keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private fun requireAead(): Aead = aead ?: throw IllegalStateException("EncryptedDataStore not initialized. Call EncryptedDataStore.init(context) before use.")

    data class Credentials(
        val username: String,
        val password: String
    )

    suspend fun saveCredentials(context: Context, username: String, password: String) {
        val primitive = requireAead()

        val encryptedUsername = primitive.encrypt(username.toByteArray(Charsets.UTF_8), "username".toByteArray(Charsets.UTF_8))
        val encryptedPassword = primitive.encrypt(password.toByteArray(Charsets.UTF_8), "password".toByteArray(Charsets.UTF_8))

        // Store as Base64 to avoid charset issues
        val encUserB64 = Base64.encodeToString(encryptedUsername, Base64.NO_WRAP)
        val encPassB64 = Base64.encodeToString(encryptedPassword, Base64.NO_WRAP)

        context.dataStore.edit { prefs ->
            prefs[USERNAME_KEY] = encUserB64
            prefs[PASSWORD_KEY] = encPassB64
        }
    }

    suspend fun getCredentials(context: Context): Credentials? {
        val primitive = requireAead()
        return try {
            val prefs = context.dataStore.data.first()
            val encUserB64 = prefs[USERNAME_KEY] ?: return null
            val encPassB64 = prefs[PASSWORD_KEY] ?: return null

            val encUser = Base64.decode(encUserB64, Base64.NO_WRAP)
            val encPass = Base64.decode(encPassB64, Base64.NO_WRAP)

            val username = primitive.decrypt(encUser, "username".toByteArray(Charsets.UTF_8)).toString(Charsets.UTF_8)
            val password = primitive.decrypt(encPass, "password".toByteArray(Charsets.UTF_8)).toString(Charsets.UTF_8)

            Credentials(username = username, password = password)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearCredentials(context: Context) {
        context.dataStore.edit { it.clear() }
    }

    private val USERNAME_KEY = stringPreferencesKey("enc_username")
    private val PASSWORD_KEY = stringPreferencesKey("enc_password")
}