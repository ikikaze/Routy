package ro.upt.edu.mapzenver;


import android.content.Context;
import android.location.Location;

import com.mapzen.android.graphics.model.Marker;
import com.mapzen.tangram.LngLat;
import com.mapzen.valhalla.Route;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoutePlanner {

    private static RoutePlanner ourInstance = new RoutePlanner();
    private HashMap<Marker, String> routePoints = new LinkedHashMap<Marker, String>();
    private HashMap<Marker, String> nonRoutePoints = new LinkedHashMap<Marker, String>();
    private Marker startPoint;
    private Route route;
    private Context context;
    private ReverseGeocodingTask reverseGeocodingTask;

    private RoutePlanner() {
    }

    public static RoutePlanner getInstance() {
        return ourInstance;
    }


    private void reverseRoutePoint(final Marker point) {
        if (context != null) {
            reverseGeocodingTask = new ReverseGeocodingTask(context);
            reverseGeocodingTask.execute(point.getLocation());
        }
    }


    public void addCurrentMarkerToRoute(Marker marker) {
        routePoints.put(marker, nonRoutePoints.get(marker));
        nonRoutePoints.remove(marker);
    }

    public void addPoint(Marker point) {
        nonRoutePoints.put(point, "");
        reverseRoutePoint(point);
    }

    public int getWayPointNumber() {
        return routePoints.size();
    }

    public Marker getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Location location) {
        startPoint = new Marker(location.getLongitude(), location.getLatitude());
    }

    public HashMap<Marker, String> getRoutePoints() {
        return routePoints;
    }

    public HashMap<Marker, String> getNonRoutePoints() {
        return nonRoutePoints;
    }

    public void clearAll() {
        routePoints.clear();
        nonRoutePoints.clear();
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }


    public void reorderPoints(List<Map.Entry<Marker, String>> route, List<Map.Entry<Marker, String>> nonRoute) {
        HashMap<Marker, String> newRouteOrder = new LinkedHashMap<Marker, String>();

        for (Map.Entry<Marker, String> point : route) {
            newRouteOrder.put(point.getKey(), point.getValue());
        }
        routePoints.clear();
        routePoints = (HashMap<Marker, String>) newRouteOrder.clone();


        newRouteOrder.clear();
        for (Map.Entry<Marker, String> point : nonRoute) {
            newRouteOrder.put(point.getKey(), point.getValue());
        }
        nonRoutePoints = (HashMap<Marker, String>) newRouteOrder.clone();
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public void setPointData(LngLat coordinates, String result) {
        for (Marker point : nonRoutePoints.keySet()) {
            if (coordinates.equals(point.getLocation())) {
                nonRoutePoints.put(point, result);
                return;
            }
        }
        for (Marker point : routePoints.keySet()) {
            if (coordinates.equals(point.getLocation())) {
                routePoints.put(point, result);
                return;
            }
        }
    }
}
