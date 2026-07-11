package com.voltpay.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.app.ActivityCompat
import java.util.Locale

object SimUtils {

    class SimInfo(
        val subscriptionId: Int,
        val slotIndex: Int,
        carrierName: String?,
        phoneNumber: String?
    ) {
        val carrierName: String = carrierName ?: "Unknown Carrier"
        val phoneNumber: String = phoneNumber ?: "Unknown Number"
        val isJio: Boolean

        init {
            val lowerCarrier = this.carrierName.toLowerCase(Locale.getDefault())
            this.isJio = lowerCarrier.contains("jio") || lowerCarrier.contains("reliance")
        }
    }

    @JvmStatic
    fun getActiveSims(context: Context): List<SimInfo> {
        val simList = ArrayList<SimInfo>()
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        
        if (subscriptionManager != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
                if (activeSubscriptionInfoList != null) {
                    for (info in activeSubscriptionInfoList) {
                        val simInfo = SimInfo(
                            info.subscriptionId,
                            info.simSlotIndex,
                            info.carrierName?.toString(),
                            info.number
                        )
                        simList.add(simInfo)
                    }
                }
            }
        }
        return simList
    }

    @JvmStatic
    fun isCarrierSupported(carrierName: String?): Boolean {
        if (carrierName == null) return false
        val lower = carrierName.toLowerCase(Locale.getDefault())
        if (lower.contains("jio") || lower.contains("reliance")) {
            return false
        }
        return lower.contains("airtel") || lower.contains("vi") || lower.contains("vodafone") || lower.contains("idea") || lower.contains("bsnl")
    }
}
