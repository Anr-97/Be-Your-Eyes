package xyz.lanshive.beyoureyes.api;

import java.util.List;

import xyz.lanshive.beyoureyes.model.AuthResponse;
import xyz.lanshive.beyoureyes.model.RegisterRequest;
import xyz.lanshive.beyoureyes.model.LoginRequest;
import xyz.lanshive.beyoureyes.model.StatisticsResponse;
import xyz.lanshive.beyoureyes.model.VerificationCodeRequest;
import xyz.lanshive.beyoureyes.model.ResetPassRequest;
import xyz.lanshive.beyoureyes.model.ResetPasswordVerifyRequest;
import xyz.lanshive.beyoureyes.model.ResetStatusResponse;
import xyz.lanshive.beyoureyes.model.ResetStatusRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import xyz.lanshive.beyoureyes.model.RefreshTokenRequest;
import xyz.lanshive.beyoureyes.model.RefreshTokenResponse;
import xyz.lanshive.beyoureyes.model.HelpStatsResponse;
import xyz.lanshive.beyoureyes.model.HelpRecord;
import xyz.lanshive.beyoureyes.model.CallResponse;
import xyz.lanshive.beyoureyes.model.CallRequest;

public interface ApiService {
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("auth/verify")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("auth/register")
    Call<AuthResponse> sendVerificationCode(@Body VerificationCodeRequest request);

    @POST("auth/forgotPass")
    Call<AuthResponse> sendResetPasswordCode(@Body ResetPassRequest request);

    @POST("auth/resetPassword")
    Call<AuthResponse> resetPassword(@Body ResetPasswordVerifyRequest request);

    @GET("auth/statistics")
    Call<StatisticsResponse> getStatistics();

    @POST("auth/resetUserStatus")
    Call<ResetStatusResponse> resetUserStatus(@Body ResetStatusRequest request);

    @POST("auth/refreshToken")
    Call<RefreshTokenResponse> refreshToken(@Body RefreshTokenRequest request);

    @GET("auth/help-stats")
    Call<HelpStatsResponse> getHelpStats(@Query("email") String email, @Query("token") String token);

    @GET("auth/help-records")
    Call<List<HelpRecord>> getHelpRecords(@Query("email") String email, @Query("token") String token);
}