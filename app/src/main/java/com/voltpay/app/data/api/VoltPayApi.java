package com.voltpay.app.data.api;

import com.voltpay.app.data.model.ContactSyncRequest;
import com.voltpay.app.data.model.ContactSyncResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface VoltPayApi {

    @POST("api/users/verify-firebase-token")
    Call<java.util.Map<String, Object>> verifyFirebaseTokenAndRegister(@Body java.util.Map<String, String> body);

    @PUT("api/contacts/sync")
    Call<ContactSyncResponse> syncContacts(
            @Header("Authorization") String token,
            @Header("X-Phone-Number") String phoneNumber,
            @Body ContactSyncRequest request
    );

    @GET("api/contacts/restore/{phoneNumber}")
    Call<ContactSyncResponse> restoreContacts(
            @Header("Authorization") String token,
            @Path("phoneNumber") String phoneNumber
    );

    @PUT("api/settings/sync")
    Call<java.util.Map<String, Object>> syncSettings(
            @Header("Authorization") String token,
            @Header("X-Phone-Number") String phoneNumber,
            @Body com.voltpay.app.data.model.SettingsSyncRequest body
    );

    @POST("api/requests/create")
    Call<java.util.Map<String, Object>> createRequest(
            @Header("Authorization") String token,
            @Body java.util.Map<String, Object> body
    );

    @PUT("api/requests/{requestId}/complete")
    Call<java.util.Map<String, Object>> completeRequest(
            @Header("Authorization") String token,
            @Path("requestId") String requestId, 
            @Body java.util.Map<String, String> body
    );

    @PUT("api/requests/{requestId}/decline")
    Call<java.util.Map<String, Object>> declineRequest(
            @Header("Authorization") String token,
            @Path("requestId") String requestId, 
            @Body java.util.Map<String, String> body
    );

    @PUT("api/users/fcm-token")
    Call<java.util.Map<String, Object>> updateFcmToken(
            @Header("Authorization") String token,
            @Body java.util.Map<String, String> body
    );

    @POST("api/users/refresh-token")
    Call<java.util.Map<String, Object>> refreshToken(
            @Header("Authorization") String token,
            @Body java.util.Map<String, String> body
    );
}
