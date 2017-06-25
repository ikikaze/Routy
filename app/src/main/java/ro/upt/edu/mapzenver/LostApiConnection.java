package ro.upt.edu.mapzenver;

import android.content.Context;
import android.location.Location;
import android.widget.Toast;

import com.mapzen.android.graphics.MapzenMap;
import com.mapzen.android.lost.api.LocationListener;
import com.mapzen.android.lost.api.LocationRequest;
import com.mapzen.android.lost.api.LocationServices;
import com.mapzen.android.lost.api.LostApiClient;
import com.mapzen.tangram.LngLat;


public class LostApiConnection implements LostApiClient.ConnectionCallbacks {

    private static final int INTERVAL_NO_MOVEMENT = 3000;
    private static final int INTERVAL_LOW_SPEED = 2000;
    private static final int INTERVAL_MEDIUM_SPEED = 1500;
    private static final int INTERVAL_HIGH_SPEED = 1000;

    private static final int LOCATION_REQ_UNTIL_RECALC = 8;
    private static float AVG_SPEED = 0;

    private int updatesReceived = 0;

    private boolean connected = false;
    private LostApiClient lostApiClient;
    private MapzenMap mapzenMap;
    private Context context;
    private LocationRequest locationRequest;

    public LostApiConnection(Context context, MapzenMap map) {
        lostApiClient = new LostApiClient.Builder(context).addConnectionCallbacks(this).build();
        mapzenMap = map;
        this.context = context;
        lostApiClient.connect();
    }


    @Override
    public void onConnected() {
        connected = true;
        try {
            Location location = LocationServices.FusedLocationApi.getLastLocation(lostApiClient);

            if (location != null) {
                LngLat coords = new LngLat(location.getLongitude(), location.getLatitude());
                mapzenMap.setPosition(coords, 1);
            }
        } catch (SecurityException e) {
            Toast.makeText(context, "Location unavailable, please give required permissions", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onConnectionSuspended() {
        connected = false;
    }

    public LostApiClient getClient() {
        return lostApiClient;

    }

    public Location getMyLocation() {
        try {
            if (connected == true) {
                Location location = LocationServices.FusedLocationApi.getLastLocation(lostApiClient);
                return location;
            } else {
                Toast.makeText(context, "failed in retrieving last location, this ain't good!", Toast.LENGTH_LONG).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(context, "not connected yet!", Toast.LENGTH_LONG).show();
        }
        return null;
    }


    void initNavLocationRequest() {
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(INTERVAL_NO_MOVEMENT)
                .setSmallestDisplacement(10);
    }

    void receiveLocationUpdates(LocationListener listener) {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(lostApiClient, locationRequest, listener);
        } catch (SecurityException e) {
        }
    }


    void incrementlocation(Location location) {
        updatesReceived++;
        addToAverage(location.getSpeed());
        if (updatesReceived == LOCATION_REQ_UNTIL_RECALC) {
            modifyRequest();
            updatesReceived = 0;
        }
    }

    private void addToAverage(float speed) {
        AVG_SPEED = AVG_SPEED + (speed - AVG_SPEED) / updatesReceived;
    }

    private void modifyRequest() {
        int averagespeed = Math.round(AVG_SPEED);

        if (averagespeed > 16) {
            locationRequest.setInterval(INTERVAL_HIGH_SPEED);
            return;
        }
        if (averagespeed > 8) {
            locationRequest.setInterval(INTERVAL_MEDIUM_SPEED);
            return;
        }
        if (averagespeed > 0) {
            locationRequest.setInterval(INTERVAL_LOW_SPEED);
            return;
        }
        locationRequest.setInterval(INTERVAL_NO_MOVEMENT);


    }

    public boolean isConnected() {
        return connected;
    }

    public void stopReceivingUpdates(LocationListener listener) {
        LocationServices.FusedLocationApi.removeLocationUpdates(lostApiClient, listener);
    }
}
