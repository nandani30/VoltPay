package com.voltpay.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeyStoreManager {

    private const val KEY_ALIAS = "VoltPayDatabaseKeyAlias"
    private const val PREF_NAME = "VoltPaySecurePrefs"
    private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_db_passphrase"
    private const val KEY_IV = "db_passphrase_iv"

    @JvmStatic
    fun getDatabasePassphrase(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val encryptedPassphraseBase64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
            val ivBase64 = prefs.getString(KEY_IV, null)

            if (encryptedPassphraseBase64 == null || ivBase64 == null) {
                generateAndStorePassphrase(prefs)
            } else {
                decryptPassphrase(encryptedPassphraseBase64, ivBase64)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to retrieve or generate secure database passphrase", e)
        }
    }

    private fun generateAndStorePassphrase(prefs: SharedPreferences): String {
        val rawPassphrase = ByteArray(32)
        SecureRandom().nextBytes(rawPassphrase)
        val passphrase = Base64.encodeToString(rawPassphrase, Base64.NO_WRAP)

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keySpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        }

        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedPassphrase = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))

        prefs.edit()
            .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encryptedPassphrase, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()

        return passphrase
    }

    private fun decryptPassphrase(encryptedPassphraseBase64: String, ivBase64: String): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        val encryptedPassphrase = Base64.decode(encryptedPassphraseBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedPassphrase = cipher.doFinal(encryptedPassphrase)
        return String(decryptedPassphrase, Charsets.UTF_8)
    }
}
