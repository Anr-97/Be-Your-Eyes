package com.example.beyoureyes.api;

import com.example.beyoureyes.model.AuthRequest;
import com.example.beyoureyes.model.AuthResponse;
import com.example.beyoureyes.model.StatisticsResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @POST("auth/register")
    Call<AuthResponse> register(@Body AuthRequest request);

    @POST("auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    @GET("auth/statistics")
    Call<StatisticsResponse> getStatistics();
} 