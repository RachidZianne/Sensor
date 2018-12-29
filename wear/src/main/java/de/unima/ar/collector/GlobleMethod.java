package de.unima.ar.collector;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.unima.ar.collector.database.SQLDBController;

import static android.support.wearable.view.FullscreenFragmentHelper.TAG;

public class GlobleMethod {

    public static Context context;
    public static GlobleMethod gmethod;
    public static GlobleMethod getInstance(Context mContext) {
        if (gmethod == null) {
            gmethod = new GlobleMethod();
        }
        context = mContext;
        return gmethod;
    }

    public void sendDataToDevice() {

        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Task<List<Node>> nodeListTask =
                                Wearable.getNodeClient(context).getConnectedNodes();
                        try {
                            List<Node> nodes = Tasks.await(nodeListTask);

                            for (Node node : nodes) {
                                Wearable.getMessageClient(context).sendMessage(node.getId(), "sensordatafile", convertTobyte());
                                sendDataToDevice();
                            }
                        } catch (ExecutionException exception) {
                            Log.e(TAG, "Task failed: " + exception);

                        } catch (InterruptedException exception) {
                            Log.e(TAG, "Interrupt occurred: " + exception);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                },
                30000
        );
    }

    public byte[] convertTobyte() throws IOException {
        File source = new File(SQLDBController.getInstance().getPath());
        return fullyReadFileToBytes(source);
    }

    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);;
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }
        return bytes;
    }
}


