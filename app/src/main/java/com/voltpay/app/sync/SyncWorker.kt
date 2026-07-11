package com.voltpay.app.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.voltpay.app.BuildConfig
import com.voltpay.app.R
import com.voltpay.app.data.api.VoltPayApi
import com.voltpay.app.data.db.ContactDao
import com.voltpay.app.data.db.SyncItemDao
import com.voltpay.app.data.model.ContactSyncRequest
import com.voltpay.app.data.model.SettingsSyncRequest
import com.voltpay.app.data.model.SyncItem
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import kotlin.math.pow

class SyncWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val syncItemDao = SyncItemDao(context)
    private val contactDao = ContactDao(context)
    private val gson = Gson()
    private val api: VoltPayApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(VoltPayApi::class.java)
    }

    override fun doWork(): Result {
        val pendingItems = syncItemDao.pendingItems

        if (pendingItems.isEmpty()) {
            return Result.success()
        }

        val phoneNumber = getPhoneNumber()
        val token = getAuthToken()
        if (phoneNumber.isNullOrEmpty() || token.isNullOrEmpty()) {
            return Result.failure()
        }

        var allSuccess = true
        var needsRetry = false
        var showedFailureNotification = false

        for (item in pendingItems) {
            if (item.retryCount > 0 && (System.currentTimeMillis() - item.lastAttempted) < (2.0.pow(item.retryCount) * 1000)) {
                needsRetry = true
                continue
            }

            var success = false
            when (item.actionType) {
                "SYNC_CONTACTS" -> success = syncContacts(item, phoneNumber, token)
                "SYNC_SETTINGS" -> success = syncSettings(item, phoneNumber, token)
                "SEND_REQUEST" -> success = sendRequest(item, phoneNumber, token)
            }

            if (!success) {
                allSuccess = false

                if (item.retryCount + 1 > 5) {
                    syncItemDao.markFailed(item.id)
                    if (!showedFailureNotification) {
                        showFailureNotification()
                        showedFailureNotification = true
                    }
                } else {
                    syncItemDao.incrementRetry(item.id)
                    needsRetry = true
                }
            } else {
                syncItemDao.deleteSyncItem(item.id)
            }
        }

        if (needsRetry) {
            return Result.retry()
        }

        return if (allSuccess) Result.success() else Result.failure()
    }

    private fun syncContacts(item: SyncItem, phoneNumber: String, token: String): Boolean {
        return try {
            val contacts = contactDao.getAllContacts()
            val request = ContactSyncRequest(contacts)

            var response = api.syncContacts(token, phoneNumber, request).execute()

            if (handle401AndRetry(response, phoneNumber)) {
                val newToken = getAuthToken() ?: return false
                response = api.syncContacts(newToken, phoneNumber, request).execute()
            }

            response.isSuccessful && response.body()?.success == true
        } catch (e: IOException) {
            Log.e(TAG, "Network error during contacts sync", e)
            false
        }
    }

    private fun syncSettings(item: SyncItem, phoneNumber: String, token: String): Boolean {
        return try {
            val request = gson.fromJson(item.payload, SettingsSyncRequest::class.java)
            var response = api.syncSettings(token, phoneNumber, request).execute()

            if (handle401AndRetry(response, phoneNumber)) {
                val newToken = getAuthToken() ?: return false
                response = api.syncSettings(newToken, phoneNumber, request).execute()
            }
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing settings", e)
            false
        }
    }

    private fun sendRequest(item: SyncItem, phoneNumber: String, token: String): Boolean {
        return try {
            val typeToken = object : TypeToken<Map<String, Any>>() {}.type
            val payloadMap: Map<String, Any> = gson.fromJson(item.payload, typeToken)

            var response = api.createRequest(token, payloadMap).execute()

            if (handle401AndRetry(response, phoneNumber)) {
                val newToken = getAuthToken() ?: return false
                response = api.createRequest(newToken, payloadMap).execute()
            }

            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error sending request via backend", e)
            false
        }
    }



    private fun handle401AndRetry(response: Response<*>, phoneNumber: String): Boolean {
        if (response.code() == 401) {
            try {
                val prefs = getSecurePrefs()
                val password = prefs?.getString("user_password", null)
                if (password != null) {
                    val loginBody = mapOf("phoneNumber" to phoneNumber, "password" to password)
                    val loginResponse = api.login(loginBody).execute()
                    if (loginResponse.isSuccessful && loginResponse.body() != null) {
                        val newToken = loginResponse.body()!!["token"] as? String
                        if (newToken != null) {
                            prefs.edit().putString("auth_token", "Bearer $newToken").apply()
                            return true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh token", e)
            }
        }
        return false
    }

    private fun getPhoneNumber(): String? {
        return getSecurePrefs()?.getString("user_phone", null)
    }

    private fun getAuthToken(): String? {
        return getSecurePrefs()?.getString("auth_token", null)
    }

    private fun getSecurePrefs(): android.content.SharedPreferences? {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "voltpay_secure_prefs", masterKeyAlias, applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showFailureNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("voltpay_sync_errors", "Sync Errors", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, "voltpay_sync_errors")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Sync Failed")
            .setContentText("VoltPay was unable to backup some data to the cloud.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(202, builder.build())
    }

    companion object {
        private const val TAG = "SyncWorker"
    }
}
