package com.voltpay.app;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import com.voltpay.app.service.SyncService;

public class VoltPayApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Intent serviceIntent = new Intent(this, SyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
