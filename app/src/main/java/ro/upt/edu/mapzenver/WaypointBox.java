package ro.upt.edu.mapzenver;


import com.mapzen.tangram.LngLat;

public class WaypointBox {

    private final LngLat center;
    private double minLat, minLon;
    private double maxLat, maxLon;

    public WaypointBox(LngLat center) {

        minLat = center.latitude - 0.0009000009000009;
        maxLat = center.latitude + 0.0009000009000009;
        maxLon = center.longitude - 0.0009000009000009 / Math.cos(minLat);
        minLon = center.longitude + 0.0009000009000009 / Math.cos(maxLat);
        this.center = center;
    }

    public LngLat getCenter() {
        return center;
    }


    public boolean isInBox(LngLat location) {
        if (location.latitude > minLat && location.latitude < maxLat) {
            if (location.longitude > minLon && location.longitude < maxLon)
                return true;
        }
        return false;
    }

}
