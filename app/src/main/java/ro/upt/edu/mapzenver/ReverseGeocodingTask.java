package ro.upt.edu.mapzenver;

import android.content.Context;
import android.os.AsyncTask;

import com.mapzen.android.search.MapzenSearch;
import com.mapzen.pelias.gson.Feature;
import com.mapzen.pelias.gson.Result;
import com.mapzen.tangram.LngLat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ReverseGeocodingTask extends AsyncTask<LngLat, Void, String> {

    private Context context;
    private MapzenSearch mapzenSearch;
    private boolean callbackcomplete;
    private String returnString;
    private LngLat pointSearched;

    ReverseGeocodingTask(Context context) {

        this.context = context;
        mapzenSearch = new MapzenSearch(this.context);
        callbackcomplete = false;
    }


    @Override
    protected String doInBackground(LngLat... params) {

        for (LngLat param : params) {
            pointSearched = param;

            mapzenSearch.reverse(param.latitude, param.longitude, new Callback<Result>() {
                @Override
                public void onResponse(Call<Result> call, Response<Result> response) {
                    Feature feature = response.body().getFeatures().get(0);
                    returnString = feature.properties.name;
                    callbackcomplete = true;
                }

                @Override
                public void onFailure(Call<Result> call, Throwable t) {
                    callbackcomplete = true;
                    returnString = "No location for marker";
                }
            });
        }

        try {
            while (callbackcomplete == false || returnString == null)
                Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        return returnString;

    }


    @Override
    public void onPostExecute(String result) {
        RoutePlanner routePlanner = RoutePlanner.getInstance();
        routePlanner.setPointData(pointSearched, result);
    }
}
