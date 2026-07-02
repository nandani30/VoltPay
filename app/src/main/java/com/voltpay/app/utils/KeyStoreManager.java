package com.voltpay.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class KeyStoreManager {

    private static final String KEY_ALIAS = "VoltPayDatabaseKeyAlias";
    private static final String PREF_NAME = "VoltPaySecurePrefs";
    private static final String KEY_ENCRYPTED_PASSPHRASE = "encrypted_db_passphrase";
    private static final String KEY_IV = "db_passphrase_iv";

    /**
     * Gets or generates the 256-bit secure passphrase for SQLCipher, derived via Android Keystore.
     */
    public static String getDatabasePassphrase(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String encryptedPassphraseBase64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null);
            String ivBase64 = prefs.getString(KEY_IV, null);

            if (encryptedPassphraseBase64 == null || ivBase64 == null) {
                return generateAndStorePassphrase(prefs);
            }

            return decryptPassphrase(encryptedPassphraseBase64, ivBase64);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve or generate secure database passphrase", e);
        }
    }

    private static String generateAndStorePassphrase(SharedPreferences prefs) throws Exception {
        // Generate a random 32-byte (256-bit) passphrase
        byte[] rawPassphrase = new byte[32];
        new SecureRandom().nextBytes(rawPassphrase);
        String passphrase = Base64.encodeToString(rawPassphrase, Base64.NO_WRAP);

        // Encrypt the passphrase using Android Keystore
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build();
            keyGenerator.init(keySpec);
            keyGenerator.generateKey();
        }

        SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] iv = cipher.getIV();
        byte[] encryptedPassphrase = cipher.doFinal(passphrase.getBytes("UTF-8"));

        // Store encrypted passphrase and IV
        prefs.edit()
                .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encryptedPassphrase, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply();

        return passphrase;
    }

    private static String decryptPassphrase(String encryptedPassphraseBase64, String ivBase64) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);

        byte[] encryptedPassphrase = Base64.decode(encryptedPassphraseBase64, Base64.NO_WRAP);
        byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] decryptedPassphrase = cipher.doFinal(encryptedPassphrase);
        return new String(decryptedPassphrase, "UTF-8");
    }
}
