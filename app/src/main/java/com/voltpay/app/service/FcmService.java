package com.voltpay.app.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.voltpay.app.BuildConfig;
import com.voltpay.app.R;
import com.voltpay.app.data.api.VoltPayApi;
import com.voltpay.app.data.db.SyncItemDao;
import com.voltpay.app.data.model.SyncItem;
import com.voltpay.app.sync.SyncWorker;
import com.voltpay.app.ui.request.IncomingRequestActivity;

import java.util.HashMap;
import java.util.Map;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FcmService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    "voltpay_secure_prefs", masterKeyAlias, this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            String phone = prefs.getString("user_phone", null);
            if (phone != null) {
                SyncItemDao syncItemDao = new SyncItemDao(this);
                SyncItem newItem = new SyncItem();
                newItem.setActionType("UPDATE_FCM");
                newItem.setPayload(token);
                newItem.setCreatedAt(System.currentTimeMillis());
                newItem.setStatus("PENDING");
                syncItemDao.insertSyncItem(newItem);

                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();
                OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                        .setConstraints(constraints).build();
                WorkManager.getInstance(this).enqueue(syncRequest);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            String type = data.get("type");

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        "voltpay_requests", "Payment Requests", NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }

            if ("PAYMENT_REQUEST".equals(type)) {
                String requestId = data.get("requestId");
                String name = data.get("requesterName");
                String upi = data.get("requesterUpiId");
                String phone = data.get("requesterPhone");
                String amount = data.get("amount");
                String note = data.get("note");

                Intent intent = new Intent(this, IncomingRequestActivity.class);
                intent.putExtra("requestId", requestId);
                intent.putExtra("requesterName", name);
                intent.putExtra("requesterUpiId", upi);
                intent.putExtra("requesterPhone", phone);
                intent.putExtra("amount", amount);
                intent.putExtra("note", note);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "voltpay_requests")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(name + " is requesting ₹" + amount)
                        .setContentText((note != null && !note.isEmpty()) ? note : "Tap to pay")
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            } else if ("REQUEST_PAID".equals(type)) {
                String amount = data.get("amount");
                String name = data.get("name");
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "voltpay_requests")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Request Paid")
                        .setContentText("Your request of ₹" + amount + " was paid by " + name)
                        .setAutoCancel(true);
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            } else if ("REQUEST_DECLINED".equals(type)) {
                String amount = data.get("amount");
                String name = data.get("name");
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "voltpay_requests")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Request Declined")
                        .setContentText(name + " declined your request of ₹" + amount)
                        .setAutoCancel(true);
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            }
        }
    }
}
