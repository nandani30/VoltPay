package com.voltpay.app.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.voltpay.app.BuildConfig
import com.voltpay.app.data.api.VoltPayApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HealthPingWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BACKEND_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val api = retrofit.create(VoltPayApi::class.java)
            api.pingHealth().execute()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
