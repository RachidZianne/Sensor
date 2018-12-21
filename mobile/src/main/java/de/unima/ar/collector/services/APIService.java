package de.unima.ar.collector.services;

import java.util.HashMap;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @POST("/api/sdcactivity")
    @Headers({"Content-Type: application/json"})
    Call<ResponseBody> saveProductInfo(@Body HashMap<String, Object> productdata);
}
