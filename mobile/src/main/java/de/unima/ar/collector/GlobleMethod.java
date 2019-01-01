package de.unima.ar.collector;

import android.app.Activity;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.services.APIService;
import de.unima.ar.collector.services.ApiUtils;
import de.unima.ar.collector.util.UIUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GlobleMethod implements MediaScannerConnection.OnScanCompletedListener {

    private static APIService mAPIService;
    private static Context context;
    public static GlobleMethod gmethod;

    public static GlobleMethod getInstance(Context mContext) {
        if (gmethod == null) {
            gmethod = new GlobleMethod();
        }
        context = mContext;
        mAPIService = ApiUtils.getAPIService();
        return gmethod;
    }

    //save database on device external path
    public void saveDataBase()  {
        // Make dirs on sd card
        File extStore = Environment.getExternalStorageDirectory();

        // sdcard does not exist
        if (extStore == null) {
            UIUtils.makeToast((Activity) context, R.string.option_export_nosdcardfound, Toast.LENGTH_LONG);
        }

        // check if storage is available and writable
        String storageState = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(storageState)) {
            extStore = Environment.getDataDirectory();

            if (extStore == null || !extStore.canRead() || !extStore.canWrite()) {
                UIUtils.makeToast((Activity) context, R.string.option_export_nowritablemedia, Toast.LENGTH_LONG);
            }
        }

        // Unseren eigenen Ordner auf der SD Karte erstellen falls nicht vorhanden
        File root = new File(extStore.getAbsolutePath(), "SensorDataCollector");
        boolean result = root.mkdir();
        if (!result && !root.exists()) {
            UIUtils.makeToast((Activity) context, R.string.option_export_nowritablemedia, Toast.LENGTH_LONG);
        }

        //write data
        File source = new File(SQLDBController.getInstance().getPath());
        File target = new File(root, "db" + System.currentTimeMillis() + ".sqlite");
        boolean success = writeDataToDisk(source, target);

        if (success) {
            if (target.exists()) {
                GlobleMethod.getInstance(context).sendDataToServer(target);
            }

            // make new file discoverable
            MediaScannerConnection.scanFile(context, new String[]{target.getAbsolutePath()}, null, gmethod);
        }
    }

    //write data in database file
    private boolean writeDataToDisk(File source, File target) {
        try {
            boolean success = target.createNewFile();

            if(!success) {
                UIUtils.makeToast((Activity) context, R.string.option_export_fileexists, Toast.LENGTH_LONG);
                return false;
            }
        } catch(IOException e1) {
            UIUtils.makeToast((Activity) context, R.string.option_export_couldnotcreatefile, Toast.LENGTH_LONG);
            return false;
        }

        if(source == null || !source.exists()) {
            UIUtils.makeToast((Activity) context, R.string.option_export_filedoesnotexist, Toast.LENGTH_LONG);
            return false;
        }

        try {
            InputStream instream = new FileInputStream(source);
            OutputStream outstream = new FileOutputStream(target);

            byte[] buf = new byte[1024];
            int len;

            while((len = instream.read(buf)) > 0) {
                outstream.write(buf, 0, len);
            }

            instream.close();
            outstream.close();
        } catch(IOException e) {
            UIUtils.makeToast((Activity) context, R.string.option_export_errorwritingfile, Toast.LENGTH_LONG);
            return false;
        }

        return true;
    }

    @Override
    public void onScanCompleted(String s, Uri uri) {
        UIUtils.makeToast((Activity) context, R.string.option_export_copysuccessful, Toast.LENGTH_SHORT);
    }


    //every 30 second to send data on server
    public void sendDataToServer(final File filePath) {
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        //API call for send sqlite file to server
                        APIService mAPIService = ApiUtils.getAPIService();
                        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), filePath);
                        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("file",filePath.getName(),requestFile);

                        RequestBody device_id = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(Settings.Secure.getString(context.getContentResolver(),
                                Settings.Secure.ANDROID_ID)));
                        RequestBody file_type = RequestBody.create(MediaType.parse("text/plain"), "mobile");

                        Call<ResponseBody> responseBodyCall = mAPIService.saveWatchDeviceData(device_id, file_type, multipartBody);
                        responseBodyCall.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                Log.d("Response", "="+response.code());
                                Log.d("Response", "= "+response.message());

                                if (response.code() == 200) {
                                    Toast.makeText(context, "file send successful from device!",
                                            Toast.LENGTH_LONG).show();
                                    GlobleMethod.getInstance(context).saveDataBase();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.d("failure", "message = " + t.getMessage());
                                Log.d("failure", "cause = " + t.getCause());
                            }
                        });
                    }
                },
                30000
        );
    }
}
