package com.voltpay.app.data.api

import com.voltpay.app.data.model.ContactSyncRequest
import com.voltpay.app.data.model.ContactSyncResponse
import com.voltpay.app.data.model.SettingsSyncRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface VoltPayApi {

    @GET("api/health")
    fun pingHealth(): Call<Void>

    @POST("api/users/login")
    fun login(@Body body: Map<String, String>): Call<Map<String, Any>>

    @PUT("api/contacts/sync")
    fun syncContacts(
        @Header("Authorization") token: String,
        @Header("X-Phone-Number") phoneNumber: String,
        @Body request: ContactSyncRequest
    ): Call<ContactSyncResponse>

    @GET("api/contacts/restore/{phoneNumber}")
    fun restoreContacts(
        @Header("Authorization") token: String,
        @Path("phoneNumber") phoneNumber: String
    ): Call<ContactSyncResponse>

    @PUT("api/settings/sync")
    fun syncSettings(
        @Header("Authorization") token: String,
        @Header("X-Phone-Number") phoneNumber: String,
        @Body body: SettingsSyncRequest
    ): Call<Map<String, Any>>

    @POST("api/requests/create")
    fun createRequest(
        @Header("Authorization") token: String,
        @Body body: Map<String, Any>
    ): Call<Map<String, Any>>

    @PUT("api/requests/{requestId}/complete")
    fun completeRequest(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: String,
        @Body body: Map<String, String>
    ): Call<Map<String, Any>>

    @PUT("api/requests/{requestId}/decline")
    fun declineRequest(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: String,
        @Body body: Map<String, String>
    ): Call<Map<String, Any>>



    @POST("api/users/refresh-token")
    fun refreshToken(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Call<Map<String, Any>>
}
