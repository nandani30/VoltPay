package com.voltpay.app

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.voltpay.app.service.SyncService
import com.voltpay.app.sync.HealthPingWorker
import java.util.concurrent.TimeUnit

class VoltPayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val serviceIntent = Intent(this, SyncService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val pingRequest = PeriodicWorkRequest.Builder(
            HealthPingWorker::class.java,
            15, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "HealthPing",
            ExistingPeriodicWorkPolicy.KEEP,
            pingRequest
        )
    }
}
