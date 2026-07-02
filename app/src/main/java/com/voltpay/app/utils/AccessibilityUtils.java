package com.voltpay.app.utils;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public class AccessibilityUtils {
    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(accessibilityService.getName()))
                return true;
        }
        return false;
    }
}
