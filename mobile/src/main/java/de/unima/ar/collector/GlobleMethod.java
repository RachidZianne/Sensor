package de.unima.ar.collector;

import android.app.Activity;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.services.APIService;
import de.unima.ar.collector.services.ApiUtils;
import de.unima.ar.collector.util.UIUtils;
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
                GlobleMethod.getInstance(context).sendDataToServer();
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
    public void sendDataToServer() {
          Handler myHandler = new Handler();
          myHandler.postDelayed(new Runnable() {
              @Override
              public void run() {
                  String table = "SDC_Activity";
                  String field_name = "name";

                  String query = "SELECT "+field_name+" FROM "+table;
                  List<String[]> tblData = SQLDBController.getInstance().query(query, null, false);

                  StringBuilder sdcData = new StringBuilder();
                  for (int i = 0; i < tblData.size(); i++) {
                      if (sdcData.length() <= 0) {
                          sdcData.append(tblData.get(i)[0]);
                      }
                      else {
                          sdcData.append(";"+tblData.get(i)[0]);
                      }
                  }

                  HashMap<String, Object> sdcdict = new HashMap<>();
                  sdcdict.put("name", sdcData);

                  mAPIService.saveProductInfo(sdcdict).enqueue(new Callback<ResponseBody>() {
                      @Override
                      public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                          sendDataToServer();
                      }

                      @Override
                      public void onFailure(Call<ResponseBody> call, Throwable t) {
                      }
                  });
              }
          }, 30000);
    }
}
