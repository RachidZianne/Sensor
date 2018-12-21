package de.unima.ar.collector.services;

public class ApiUtils {
    private ApiUtils() {}
    public static APIService getAPIService() {
        return RetrofitInstance.getRetrofitInstance().create(APIService.class);
    }
}
