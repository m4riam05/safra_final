package com.safra.app.api;

import com.safra.app.model.NotificationSenderModel;
import com.safra.app.util.NotificationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface NotificationAPI {

    @Headers("Content-Type:application/json")
    @POST("v1/projects/YOUR_PROJECT_ID/messages:send") // TODO: Replace with your own firebase project id
    Call<NotificationResponse> sendNotification(@Header("Authorization") String authHeader, @Body NotificationSenderModel body);
}
