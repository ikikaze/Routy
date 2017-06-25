package ro.upt.edu.mapzenver;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mapzen.android.graphics.MapView;
import com.mapzen.android.graphics.MapzenMap;
import com.mapzen.android.graphics.OnMapReadyCallback;
import com.mapzen.android.graphics.model.CameraType;
import com.mapzen.android.graphics.model.CinnabarStyle;
import com.mapzen.android.graphics.model.Marker;
import com.mapzen.android.lost.api.LocationListener;
import com.mapzen.android.routing.MapzenRouter;
import com.mapzen.helpers.DistanceFormatter;
import com.mapzen.helpers.RouteEngine;
import com.mapzen.helpers.RouteListener;
import com.mapzen.model.ValhallaLocation;
import com.mapzen.tangram.LngLat;
import com.mapzen.valhalla.Instruction;
import com.mapzen.valhalla.Route;
import com.mapzen.valhalla.RouteCallback;
import com.mapzen.valhalla.Router;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TripActivity extends BaseActivity {


    MapzenMap mapzenMap;
    RoutePlanner routePlanner;
    MapzenRouter mapzenRouter;
    RouteEngine routeEngine;
    TripListener routeListener;
    LostApiConnection lostApiConnection;
    TextView textInstr, textTime, textDist;

    Location previousLocation = null;
    double movementSpeed = 0;
    private List<WaypointBox> waypointBoxes;
    private LocationListener locationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        routePlanner = RoutePlanner.getInstance();
        routePlanner.setContext(this);
        initBoxes();
        mapzenRouter = new MapzenRouter(this);
        routeEngine = new RouteEngine();
        routeListener = new TripListener();
        routeEngine.setListener(routeListener);
        configRouter();


        textTime = (TextView) findViewById(R.id.txtTime);
        textDist = (TextView) findViewById(R.id.txtDst);


        mapzenRouter.setDriving();
        mapzenRouter.setDistanceUnits(MapzenRouter.DistanceUnits.KILOMETERS);

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);

        View mCustomView = mInflater.inflate(R.layout.action_bar_trip, null);
        textInstr = (TextView) mCustomView.findViewById(R.id.text_instr);
        textInstr.setText("Instruction!");
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT);
        getSupportActionBar().setCustomView(mCustomView, lp);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);


        MapView mapView = (MapView) findViewById(R.id.routeMap);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapzenMap MapzenMap) {
                TripActivity.this.mapzenMap = MapzenMap;
                mapzenMap.setCameraType(CameraType.ISOMETRIC);
                mapzenMap.setStyle(new CinnabarStyle());
                mapzenMap.applySceneUpdates();
                lostApiConnection = new LostApiConnection(TripActivity.this, mapzenMap);
                routeEngine.setRoute(routePlanner.getRoute());

                List<LngLat> linePointList = new ArrayList<LngLat>();
                for (ValhallaLocation location : routePlanner.getRoute().getGeometry()) {
                    LngLat lngLat = new LngLat(location.getLongitude(), location.getLatitude());
                    linePointList.add(lngLat);

                }

                mapzenMap.drawRouteLine(linePointList);
                Set<Marker> markers = routePlanner.getRoutePoints().keySet();
                for (Marker pin : markers) {
                    mapzenMap.drawDroppedPin(pin.getLocation());
                }


                configRouteEngine();


            }
        });

        textDist.setText((double) (routePlanner.getRoute().getTotalDistance() / 100) / 10 + "");


    }

    private void initBoxes() {
        waypointBoxes = new ArrayList<>();
        for (Marker point : routePlanner.getRoutePoints().keySet()) {
            waypointBoxes.add(new WaypointBox(point.getLocation()));
        }
    }

    private void configRouteEngine() {

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                if (previousLocation != null) {
                    double elapsedTime = (location.getTime() - previousLocation.getTime()) / 1_000; // Convert milliseconds to seconds
                    movementSpeed = previousLocation.distanceTo(location) / elapsedTime;
                } else
                    movementSpeed = 0;
                previousLocation = location;

                double speed = location.hasSpeed() ? location.getSpeed() : movementSpeed;
                location.setSpeed((float) speed);
                lostApiConnection.incrementlocation(location);
                checkWaypointBox(location);

                ValhallaLocation valhallaLocation = new ValhallaLocation();
                valhallaLocation.setBearing(location.getBearing());
                valhallaLocation.setLatitude(location.getLatitude());
                valhallaLocation.setLongitude(location.getLongitude());

                if (routeListener.route.getNextInstruction() != null) {
                    routeEngine.onLocationChanged(valhallaLocation);
                }
                LngLat lngLat = new LngLat(valhallaLocation.getLongitude(), valhallaLocation.getLatitude());

                mapzenMap.setPosition(lngLat, 250);
                float rotation = (float) Math.toRadians(360 - location.getBearing());
                mapzenMap.setRotation(rotation);
                mapzenMap.setTilt(0.96f, 250);

            }

            @Override
            public void onProviderDisabled(String provider) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }
        };
        lostApiConnection.initNavLocationRequest();
        lostApiConnection.receiveLocationUpdates(locationListener);
    }

    private void configRouter() {

        mapzenRouter.setCallback(new RouteCallback() {
            @Override
            public void success(Route route) {
                routePlanner.setRoute(route);
                routeEngine = new RouteEngine();
                routeListener = new TripListener();
                routeListener.route = route;
                routeEngine.setListener(routeListener);
                routeEngine.setRoute(route);


                List<LngLat> routeLine = new ArrayList<LngLat>();
                for (ValhallaLocation valhallaLocation : route.getGeometry()) {
                    routeLine.add(new LngLat(valhallaLocation.getLongitude(), valhallaLocation.getLatitude()));
                }

                mapzenMap.drawRouteLine(routeLine);
            }

            @Override
            public void failure(int i) {
                mapzenRouter.fetch();
            }
        });
    }

    private void checkWaypointBox(Location location) {
        LngLat coordinates = new LngLat(location.getLongitude(), location.getLatitude());
        if (waypointBoxes.size() >= 2)
            if (waypointBoxes.get(0).isInBox(coordinates)) {
                waypointBoxes.remove(0);
                ValhallaLocation valhallaLocation = new ValhallaLocation();
                valhallaLocation.setLatitude(previousLocation.getLatitude());
                valhallaLocation.setLongitude(previousLocation.getLongitude());
                valhallaLocation.setBearing(previousLocation.getBearing());
                recalculateRoute(valhallaLocation);
            }
    }

    private void recalculateRoute(ValhallaLocation location) {

        mapzenMap.clearRouteLine();
        mapzenMap.clearDroppedPins();
        mapzenRouter.clearLocations();

        double[] startLocation = {location.getLatitude(), location.getLongitude()};
        mapzenRouter.setLocation(startLocation);

        for (WaypointBox box : waypointBoxes) {
            LngLat waypoint = box.getCenter();
            double[] waypointcoords = {waypoint.latitude, waypoint.longitude};
            mapzenRouter.setLocation(waypointcoords);
            mapzenMap.drawDroppedPin(waypoint);
        }
        mapzenRouter.fetch();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (lostApiConnection.isConnected() == true)
            lostApiConnection.getClient().disconnect();
    }

    public class TripListener implements RouteListener {

        RoutePlanner routePlanner;
        Context context;
        Route route;


        public TripListener() {
            routePlanner = RoutePlanner.getInstance();
            context = routePlanner.getContext();
            route = routePlanner.getRoute();
        }

        @Override
        public void onRouteStart() {
            Instruction firstInstr = route.getRouteInstructions().get(0);
            textInstr.setText(firstInstr.getHumanTurnInstruction());
            textTime.setText(route.getTotalTime() + "");
        }

        @Override
        public void onRecalculate(ValhallaLocation location) {
            Toast.makeText(context, "recalculating route...", Toast.LENGTH_SHORT).show();
            recalculateRoute(location);
        }

        @Override
        public void onSnapLocation(ValhallaLocation originalLocation, ValhallaLocation snapLocation) {


        }

        @Override
        public void onMilestoneReached(int index, RouteEngine.Milestone milestone) {

        }

        @Override
        public void onApproachInstruction(int index) {

            Instruction instruction = route.getRouteInstructions().get(index);
            if (instruction.getHumanTurnInstruction().length() > 0)
                textInstr.setText(instruction.getHumanTurnInstruction());
            textTime.setText(instruction.getTime() + "");
        }

        @Override
        public void onInstructionComplete(int index) {
            Instruction instruction = route.getRouteInstructions().get(index);
            if (instruction.getVerbalPostTransitionInstruction().length() > 0)
                textInstr.setText(instruction.getVerbalPostTransitionInstruction());
            route.addSeenInstruction(instruction);
            textTime.setText(instruction.getTime() + "");
        }

        @Override
        public void onUpdateDistance(int distanceToNextInstruction, int distanceToDestination) {
            String distance = DistanceFormatter.format(distanceToDestination, false, Router.DistanceUnits.KILOMETERS);
            String message = String.valueOf(distance + " remaining");
            textDist.setText(message);
        }

        @Override
        public void onRouteComplete() {

            if (waypointBoxes.size() == 1) {
                Toast.makeText(context, "You have arrived!", Toast.LENGTH_SHORT).show();
                lostApiConnection.stopReceivingUpdates(locationListener);
                mapzenMap.clearDroppedPins();
                mapzenMap.clearRouteLine();
                Intent intent = new Intent(context, SearchActivity.class);
                startActivity(intent);
            }
        }
    }

}
