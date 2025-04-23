package xyz.lanshive.beyoureyes.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.lanshive.beyoureyes.BeYourEyesApplication;

public class RetrofitClient {
    private static final String BASE_URL = "http://47.109.150.64:3000/api/";
    private static RetrofitClient instance;
    private final Retrofit retrofit;

    private RetrofitClient() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    String token = BeYourEyesApplication.getInstance().getAuthToken();
                    Request.Builder builder = original.newBuilder();
                    
                    if (!token.isEmpty()) {
                        builder.header("Authorization", "Bearer " + token);
                    }
                    
                    Request request = builder.build();
                    return chain.proceed(request);
                })
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public ApiService create(ApiService apiService) {
        return retrofit.create(ApiService.class);
    }
} 