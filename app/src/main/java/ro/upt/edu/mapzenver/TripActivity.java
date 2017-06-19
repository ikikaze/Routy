package ro.upt.edu.mapzenver;

import android.content.Context;
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
import com.mapzen.android.lost.api.LocationRequest;
import com.mapzen.android.lost.api.LocationServices;
import com.mapzen.android.routing.MapzenRouter;
import com.mapzen.helpers.RouteEngine;
import com.mapzen.helpers.RouteListener;
import com.mapzen.model.ValhallaLocation;
import com.mapzen.tangram.LngLat;
import com.mapzen.valhalla.Instruction;
import com.mapzen.valhalla.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TripActivity extends BaseActivity {


    MapzenMap mapzenMap;
    RoutePlanner routePlanner;
    MapzenRouter mapzenRouter;
    RouteEngine routeEngine;
    LostApiConnection lostApiConnection;
    TextView textInstr, textTime, textDist;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        routePlanner = RoutePlanner.getInstance();
        routePlanner.setContext(this);
        mapzenRouter = new MapzenRouter(this);
        routeEngine = new RouteEngine();
        routeEngine.setListener(new TripListener());


        textTime = (TextView) findViewById(R.id.txtTime);
        textDist = (TextView) findViewById(R.id.txtDst);


        mapzenRouter.setDriving();

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
    }

    private void configRouteEngine() {


        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(2500)
                .setSmallestDisplacement(10);


        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                ValhallaLocation valhallaLocation = new ValhallaLocation();
                valhallaLocation.setBearing(location.getBearing());
                valhallaLocation.setLatitude(location.getLatitude());
                valhallaLocation.setLongitude(location.getLongitude());

                routeEngine.onLocationChanged(valhallaLocation);
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


        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(lostApiConnection.getClient(), request, listener);
        } catch (SecurityException e) {
        }


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


        }

        @Override
        public void onRecalculate(ValhallaLocation location) {

            //TODO recalculation
            Toast.makeText(context, "I'm LOST, implement recalculation", Toast.LENGTH_SHORT).show();


        }

        @Override
        public void onSnapLocation(ValhallaLocation originalLocation, ValhallaLocation snapLocation) {

            //Toast.makeText(context,"Snap location",Toast.LENGTH_SHORT).show();
            //Implementation unneeded as no specific action has to be taken

        }

        @Override
        public void onMilestoneReached(int index, RouteEngine.Milestone milestone) {


            Toast.makeText(context, "milestone: " + index + " reached: " + milestone.name(), Toast.LENGTH_SHORT).show();

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

            textDist.setText(distanceToDestination + " m");


            // Toast.makeText(context,"next instr dst : "+ distanceToNextInstruction + " dest dst : " + distanceToDestination ,Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onRouteComplete() {

            Toast.makeText(context, "You have arrived!", Toast.LENGTH_SHORT).show();

            //TODO

        }
    }

}
