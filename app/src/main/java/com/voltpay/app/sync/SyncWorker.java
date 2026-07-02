package com.voltpay.app.sync;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.voltpay.app.BuildConfig;
import com.voltpay.app.data.api.VoltPayApi;
import com.voltpay.app.data.db.ContactDao;
import com.voltpay.app.data.db.SyncItemDao;
import com.voltpay.app.data.model.Contact;
import com.voltpay.app.data.model.ContactSyncRequest;
import com.voltpay.app.data.model.ContactSyncResponse;
import com.voltpay.app.data.model.SettingsSyncRequest;
import com.voltpay.app.data.model.SyncItem;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private final SyncItemDao syncItemDao;
    private final ContactDao contactDao;
    private final VoltPayApi api;
    private final Gson gson;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        syncItemDao = new SyncItemDao(context);
        contactDao = new ContactDao(context);
        gson = new Gson();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BACKEND_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(VoltPayApi.class);
    }

    @NonNull
    @Override
    public Result doWork() {
        List<SyncItem> pendingItems = syncItemDao.getPendingItems();

        if (pendingItems.isEmpty()) {
            return Result.success();
        }

        String phoneNumber = getPhoneNumber();
        String token = getAuthToken();
        if (phoneNumber == null || phoneNumber.isEmpty() || token == null || token.isEmpty()) {
            return Result.failure();
        }

        boolean allSuccess = true;
        boolean needsRetry = false;
        boolean showedFailureNotification = false;

        for (SyncItem item : pendingItems) {
            if (item.getRetryCount() > 0 && (System.currentTimeMillis() - item.getLastAttempted()) < (Math.pow(2, item.getRetryCount()) * 1000)) {
                needsRetry = true;
                continue;
            }

            boolean success = false;
            if ("SYNC_CONTACTS".equals(item.getActionType())) {
                success = syncContacts(item, phoneNumber, token);
            } else if ("SYNC_SETTINGS".equals(item.getActionType())) {
                success = syncSettings(item, phoneNumber, token);
            } else if ("SEND_REQUEST".equals(item.getActionType())) {
                success = sendRequest(item, phoneNumber, token);
            } else if ("UPDATE_FCM".equals(item.getActionType())) {
                success = updateFcm(item, phoneNumber, token);
            }

            if (!success) {
                allSuccess = false;
                
                if (item.getRetryCount() + 1 > 5) {
                    syncItemDao.markFailed(item.getId());
                    if (!showedFailureNotification) {
                        showFailureNotification();
                        showedFailureNotification = true;
                    }
                } else {
                    syncItemDao.incrementRetry(item.getId());
                    needsRetry = true;
                }
            } else {
                syncItemDao.deleteSyncItem(item.getId());
            }
        }

        if (needsRetry) {
            return Result.retry();
        }

        return allSuccess ? Result.success() : Result.failure();
    }

    private boolean syncContacts(SyncItem item, String phoneNumber, String token) {
        try {
            List<Contact> contacts = contactDao.getAllContacts();
            ContactSyncRequest request = new ContactSyncRequest(contacts);
            
            Call<ContactSyncResponse> call = api.syncContacts(token, phoneNumber, request);
            Response<ContactSyncResponse> response = call.execute();

            if (handle401AndRetry(response, phoneNumber)) {
                String newToken = getAuthToken();
                response = api.syncContacts(newToken, phoneNumber, request).execute();
            }

            return response.isSuccessful() && response.body() != null && response.body().isSuccess();
        } catch (IOException e) {
            Log.e(TAG, "Network error during contacts sync", e);
            return false;
        }
    }

    private boolean syncSettings(SyncItem item, String phoneNumber, String token) {
        try {
            SettingsSyncRequest request = gson.fromJson(item.getPayload(), SettingsSyncRequest.class);
            Call<Map<String, Object>> call = api.syncSettings(token, phoneNumber, request);
            Response<Map<String, Object>> response = call.execute();
            
            if (handle401AndRetry(response, phoneNumber)) {
                String newToken = getAuthToken();
                response = api.syncSettings(newToken, phoneNumber, request).execute();
            }
            
            return response.isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Network error during settings sync", e);
            return false;
        }
    }

    private boolean sendRequest(SyncItem item, String phoneNumber, String token) {
        try {
            java.lang.reflect.Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> body = gson.fromJson(item.getPayload(), type);
            Call<Map<String, Object>> call = api.createRequest(token, body);
            Response<Map<String, Object>> response = call.execute();
            
            if (handle401AndRetry(response, phoneNumber)) {
                String newToken = getAuthToken();
                response = api.createRequest(newToken, body).execute();
            }
            
            return response.isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Network error during send request sync", e);
            return false;
        }
    }

    private boolean updateFcm(SyncItem item, String phoneNumber, String token) {
        try {
            String fcm = item.getPayload();
            Map<String, String> body = new HashMap<>();
            body.put("phoneNumber", phoneNumber);
            body.put("fcmToken", fcm);
            Call<Map<String, Object>> call = api.updateFcmToken(token, body);
            Response<Map<String, Object>> response = call.execute();
            
            if (handle401AndRetry(response, phoneNumber)) {
                String newToken = getAuthToken();
                response = api.updateFcmToken(newToken, body).execute();
            }
            
            return response.isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Network error during fcm sync", e);
            return false;
        }
    }

    private String getPhoneNumber() {
        try {
            MasterKey masterKey = new MasterKey.Builder(getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    getApplicationContext(),
                    "voltpay_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            return prefs.getString("user_phone", "");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getAuthToken() {
        try {
            MasterKey masterKey = new MasterKey.Builder(getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    getApplicationContext(),
                    "voltpay_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            String token = prefs.getString("auth_token", "");
            return token.isEmpty() ? null : "Bearer " + token;
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveAuthToken(String token) {
        try {
            MasterKey masterKey = new MasterKey.Builder(getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    getApplicationContext(),
                    "voltpay_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            prefs.edit().putString("auth_token", token).apply();
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to save auth token", e);
        }
    }

    private boolean handle401AndRetry(Response<?> response, String phoneNumber) {
        if (response.code() == 401) {
            try {
                String oldToken = getAuthToken();
                if (oldToken == null) return false;
                
                Map<String, String> body = new HashMap<>();
                body.put("phoneNumber", phoneNumber);
                Response<Map<String, Object>> refreshResponse =
                    api.refreshToken(oldToken, body).execute();
                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                    String newToken = (String) refreshResponse.body().get("authToken");
                    saveAuthToken(newToken);
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Token refresh failed", e);
            }
        }
        return false;
    }

    private void showFailureNotification() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "voltpay_sync_errors";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Sync Errors",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("VoltPay Sync Failed")
                .setContentText("VoltPay: Some actions failed to sync after multiple attempts")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
                
        if (notificationManager != null) {
            notificationManager.notify(102, builder.build());
        }
    }
}
