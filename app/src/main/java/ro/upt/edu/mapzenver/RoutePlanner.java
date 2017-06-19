package ro.upt.edu.mapzenver;


import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.mapzen.android.graphics.model.Marker;
import com.mapzen.android.search.MapzenSearch;
import com.mapzen.pelias.gson.Feature;
import com.mapzen.pelias.gson.Result;
import com.mapzen.tangram.LngLat;
import com.mapzen.valhalla.Route;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoutePlanner {


    private static HashMap<Marker,String> routePoints = new LinkedHashMap<Marker,String>();
    private static HashMap<Marker,String> nonRoutePoints = new LinkedHashMap<Marker,String>();
    private static Marker  destination,startPoint;
    private Route route;

    MapzenSearch mapzenSearch;


    private static RoutePlanner ourInstance = new RoutePlanner();
    private Context context;

    public static RoutePlanner getInstance() {
        return ourInstance;
    }

    private RoutePlanner() {
    }


    public void setDestination()
    {
        //TODO fix this if needed

        //destination = routePoints.get(routePoints.size()-1);
    }

    public void setStartPoint(Marker startPoint)
    {
        RoutePlanner.startPoint=startPoint;
    }

    public void addPointToRoute(Marker point)
    {
        routePoints.put(point,"null");
        LngLat location= point.getLocation();
        reverseRoutePoint(point);
    }

    private void reverseRoutePoint(final Marker point) {
        //only reverse points if context was set and search is initialized
        if(context!=null )
        {
            LngLat coordinates =point.getLocation();
            mapzenSearch.reverse(coordinates.latitude, coordinates.longitude, new Callback<Result>() {
                @Override
                public void onResponse(Call<Result> call, Response<Result> response) {
                    Feature feature =response.body().getFeatures().get(0); //get first suggestion
                    List<Double> coordinates = feature.geometry.coordinates;


                    Marker marker = new Marker(coordinates.get(0),coordinates.get(1));

                    routePoints.put(point,feature.properties.name);
                }

                @Override
                public void onFailure(Call<Result> call, Throwable t) {
                    Log.d("FAIL","callbackfailroutepoint");
                }
            });

        }

    }

    private void reverseNonRoutePoint(final Marker point) {
        //only reverse points if context was set and search is initialized
        if(context!=null )
        {
            LngLat coordinates =point.getLocation();
            mapzenSearch.reverse(coordinates.latitude, coordinates.longitude, new Callback<Result>() {
                @Override
                public void onResponse(Call<Result> call, Response<Result> response) {
                    Feature feature =response.body().getFeatures().get(0); //get first suggestion
                    List<Double> coordinates = feature.geometry.coordinates;
                    Marker marker = new Marker(coordinates.get(0),coordinates.get(1));
                    nonRoutePoints.put(point,feature.properties.name);
                }

                @Override
                public void onFailure(Call<Result> call, Throwable t) {
                    Log.d("FAIL","callbackfailnonroutepoint");
                }
            });
        }
    }



    public void addCurrentMarkerToRoute(Marker marker)
    {
        routePoints.put(marker,nonRoutePoints.get(marker));
        //reverseRoutePoint(marker);
        nonRoutePoints.remove(marker);
    }

    public void addPoint(Marker point)
    {
        nonRoutePoints.put(point,"");
        reverseNonRoutePoint(point);
    }

    public Marker getDestination()
    {
        return destination;
    }

    public Marker getStartPoint()
    {
        return startPoint;
    }


    public HashMap<Marker,String> getRoutePoints()
    {
        return routePoints;
    }

    public HashMap<Marker,String> getNonRoutePoints()
    {
        return nonRoutePoints;
    }

    public void setStartPoint(Location location)
    {
        startPoint = new Marker(location.getLongitude(),location.getLatitude());

    }

    public void clearAll()
    {
        routePoints.clear();
        nonRoutePoints.clear();
    }

    public void setContext(Context context)
    {
        this.context=context;
        initSearch();
    }

    public Context getContext()
    {
        return context;
    }

    private void initSearch()
    {
        mapzenSearch=new MapzenSearch(context);
    }


    public void reorderPoints(List<Map.Entry<Marker, String>> route, List<Map.Entry<Marker, String>> nonRoute)
    {
        HashMap<Marker,String> newRouteOrder=new LinkedHashMap<Marker,String>();

        for(Map.Entry<Marker,String> point : route)
        {
            newRouteOrder.put(point.getKey(),point.getValue());
        }

        routePoints.clear();
       routePoints=(HashMap<Marker,String>) newRouteOrder.clone();

        for(Marker point : routePoints.keySet())
        {
            routePoints.get(point);
        }

        newRouteOrder.clear();
        for(Marker point : newRouteOrder.keySet())
        {
            routePoints.put(point,newRouteOrder.get(point));
        }

        nonRoutePoints.putAll(newRouteOrder);

    }

    public void setRoute(Route route) {
        this.route=route;
    }

    public Route getRoute() {
        return route;
    }
}
