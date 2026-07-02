# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# GSON
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class com.voltpay.app.data.model.** { *; }

# RootBeer
-keep class com.scottyab.rootbeer.** { *; }

# WorkManager
-keep class androidx.work.** { *; }

# SQLCipher
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class net.sqlcipher.** { *; }
