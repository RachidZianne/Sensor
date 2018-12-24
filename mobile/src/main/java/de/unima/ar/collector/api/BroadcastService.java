package de.unima.ar.collector.api;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import static android.content.ContentValues.TAG;

public class BroadcastService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    Context context;
    private static BroadcastService SERVICE = null;
    private static GoogleApiClient  gac     = null;
    private static final String LOG_TAG = "BROADCASTMOBILE";

    private BroadcastService(Context context) {
      this.context = context;
      gac = new GoogleApiClient.Builder(context).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
    }

    public static void initInstance(Context context) {
        if(SERVICE == null) {
            SERVICE = new BroadcastService(context);
        }
    }

    public static BroadcastService getInstance() {
        return SERVICE;
    }


    public static void shutdown() {
        gac.disconnect();
    }


    public GoogleApiClient getAPIClient() {
        return gac;
    }


    public void sendMessage(final String path, final String text)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                HashSet<String> results = new HashSet<>();
                Task<List<Node>> nodeListTask =
                        Wearable.getNodeClient(context).getConnectedNodes();

                try {
                    // Block on a task and get the result synchronously (because this is on a background thread).
                    List<Node> nodes = Tasks.await(nodeListTask);

                    for (Node node : nodes) {
                        results.add(node.getId());
                        Wearable.getMessageClient(context).sendMessage(node.getId(), path, text.getBytes());
                    }
                } catch (ExecutionException exception) {
                    Log.e(TAG, "Task failed: " + exception);

                } catch (InterruptedException exception) {
                    Log.e(TAG, "Interrupt occurred: " + exception);
                }


//                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(gac).await();
//                while(nodes.getNodes().size() == 0) {
//                    Utils.sleep(500);
//                    nodes = Wearable.NodeApi.getConnectedNodes(gac).await();
//                }
//
//                for(Node node : nodes.getNodes()) {
//                    Wearable.MessageApi.sendMessage(gac, node.getId(), path, text.getBytes()).await();
//                }
            }
        }).start();
    }


    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(LOG_TAG, "Connected!");
    }


    @Override
    public void onConnectionSuspended(int cause)
    {
        Log.d(LOG_TAG, "Suspended!");
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.d(LOG_TAG, "Failed!");
    }
}