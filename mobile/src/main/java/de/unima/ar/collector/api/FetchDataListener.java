package de.unima.ar.collector.api;

import org.json.JSONObject;

public interface FetchDataListener {
  public  void onFetchComplete(JSONObject data);

    public void onFetchFailure(String msg);

    public void onFetchStart();
}