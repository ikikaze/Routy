package ro.upt.edu.mapzenver;

import android.content.Context;
import android.location.Location;
import android.widget.Toast;

import com.mapzen.android.graphics.MapzenMap;
import com.mapzen.android.lost.api.LocationServices;
import com.mapzen.android.lost.api.LostApiClient;
import com.mapzen.tangram.LngLat;


public class LostApiConnection implements LostApiClient.ConnectionCallbacks {

    public boolean connected = false;
    LostApiClient lostApiClient;
    MapzenMap mapzenMap;
    Context context;

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
            Toast.makeText(context, "not connected yet!", Toast.LENGTH_LONG).show();
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


}
