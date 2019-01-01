package de.unima.ar.collector.api;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Set;

import de.unima.ar.collector.services.APIService;
import de.unima.ar.collector.services.ApiUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.ContentValues.TAG;

public class ListenerService extends WearableListenerService
{
    private static BiMap<String, String> devices = HashBiMap.create();

    public static int getNumberOfDevices()
    {
        return devices.size();
    }

    public static void addDevice(String deviceID, String deviceMAC) {
        devices.put(deviceID, deviceMAC);
    }

    public static void rmDevice(String key) {
        devices.remove(key);
        devices.inverse().remove(key);
    }

    public static Set<String> getDevices()
    {
        return devices.keySet();
    }

    public static Set<String> getDevicesAddresses()
    {
        return devices.values();
    }

    public static String getDeviceID(String deviceAddress)
    {
        return devices.inverse().get(deviceAddress);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent)
    {
        String path = messageEvent.getPath();

        if(path.equalsIgnoreCase("/activity/started")) {
                    Tasks.informThatWearableHasStarted(messageEvent.getData(), this);
            return;
        }

        if(path.equalsIgnoreCase("/activity/destroyed")) {
            Tasks.informThatWearableHasDestroyed(messageEvent.getData());
            return;
        }

        if(path.equalsIgnoreCase("/posture/update")) {
            Tasks.updatePostureValue(messageEvent.getData());
            return;
        }

        if(path.equalsIgnoreCase("/position/update")) {
            Tasks.updatePositionValue(messageEvent.getData());
            return;
        }

        if(path.equalsIgnoreCase("/activity/update")) {
            Tasks.updateActivityValue(messageEvent.getData());
            return;
        }

        if(path.equalsIgnoreCase("/activity/delete")) {
            Tasks.deleteActivityValue(messageEvent.getData());
            return;
        }

        if(path.startsWith("/database/request")) {
            Tasks.processDatabaseRequest(path, messageEvent.getData());
            return;
        }

        if(path.startsWith("/sensor/data")) {
            Tasks.processIncomingSensorData(path, messageEvent.getData());
            return;
        }

        if(path.startsWith("/sensor/blob")) {
            Tasks.processIncomingSensorBlob(path, messageEvent.getData());
            return;
        }

        if (path.startsWith("sensordatafile")) {

              try {
                    //save file on local device
                    byte[] fileData = messageEvent.getData();
                    final File filePath;

                    filePath = File.createTempFile("db_new", ".sqlite");
                try {
                    FileOutputStream outputStream;
                    outputStream = new FileOutputStream(filePath);
                    outputStream.write(fileData);
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //API call for send sqlite file to server
                APIService mAPIService = ApiUtils.getAPIService();
                RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), filePath);
                MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("file",filePath.getName(),requestFile);

                RequestBody device_id = RequestBody.create(okhttp3.MediaType.parse("text/plain"), String.valueOf(messageEvent.getSourceNodeId()));
                RequestBody file_type = RequestBody.create(okhttp3.MediaType.parse("text/plain"), "watch");

                Call<ResponseBody> responseBodyCall = mAPIService.saveWatchDeviceData(device_id, file_type, multipartBody);
                responseBodyCall.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Log.d("Response", "="+response.code());
                        Log.d("Response", "= "+response.message());

                        if (response.code() == 200) {
                            Toast.makeText(getApplicationContext(), "file send successful from watch!",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.d("failure", "message = " + t.getMessage());
                        Log.d("failure", "cause = " + t.getCause());
                    }
                });
              } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        super.onMessageReceived(messageEvent);
    }
}