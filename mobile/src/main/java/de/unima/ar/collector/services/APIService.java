package de.unima.ar.collector.services;

import java.util.HashMap;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface APIService {

    @POST("/api/sdcactivity")
    @Headers({"Content-Type: application/json"})
    Call<ResponseBody> saveProductInfo(@Body HashMap<String, Object> productdata);

    @Multipart
    @POST("/api/sensordb")
    Call<ResponseBody> saveWatchDeviceData(@Part("device_id") RequestBody device_id, @Part("file_type") RequestBody file_type, @Part MultipartBody.Part file);
}
