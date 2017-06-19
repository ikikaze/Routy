package ro.upt.edu.mapzenver;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.mapzen.android.graphics.MapView;
import com.mapzen.android.graphics.MapzenMap;
import com.mapzen.android.graphics.OnMapReadyCallback;
import com.mapzen.android.graphics.model.Marker;
import com.mapzen.android.graphics.model.Polyline;
import com.mapzen.android.routing.MapzenRouter;
import com.mapzen.android.search.MapzenSearch;
import com.mapzen.model.ValhallaLocation;
import com.mapzen.tangram.LngLat;
import com.mapzen.valhalla.Route;
import com.mapzen.valhalla.RouteCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlannerActivity extends BaseActivity {

    ExpandableListView expandableListView;
    PlannerListAdapter adapter;

    MapzenSearch mapzenSearch;
    RoutePlanner routePlanner;

    Button btnPreview;
    FloatingActionButton btnInfo;
    FloatingActionButton btnGo;
    FrameLayout btnGoLayout;

    MapzenMap mapzenMap;
    MapzenRouter mapzenRouter;

    List<String> listGroups;
    HashMap<String,List<Map.Entry<Marker,String>>> listChildren;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planner);

        mapzenSearch=new MapzenSearch(this);
        mapzenRouter=new MapzenRouter(this);



        routePlanner= RoutePlanner.getInstance();
        expandableListView= (ExpandableListView) findViewById(R.id.pointList);
        expandableListView.setBackground(getResources().getDrawable(R.drawable.list_bg));
        configList();

        adapter=new PlannerListAdapter(this,listGroups,listChildren);
        expandableListView.setAdapter(adapter);
        expandableListView.expandGroup(0);
        expandableListView.expandGroup(1);

        btnPreview = (Button) findViewById(R.id.btnPreview);
        btnInfo= (FloatingActionButton) findViewById(R.id.btnInfo);
        btnGo = (FloatingActionButton) findViewById(R.id.btnStartRoute);
        btnGoLayout =(FrameLayout) findViewById(R.id.btnGoLayout);
        configBtns();

        MapView mapView=(MapView) findViewById(R.id.map_view_plan);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapzenMap mapzenMap) {
                PlannerActivity.this.mapzenMap = mapzenMap;
                mapzenMap.setPersistMapData(true);
                mapzenMap.setMyLocationEnabled(true);
                mapzenMap.setCompassButtonEnabled(true);
                mapzenMap.removeMarker();

            }
        });

        configRouter();



    }

    private void configRouter() {
            mapzenRouter.setDriving();
            mapzenRouter.setCallback(new RouteCallback() {
                @Override public void success(Route route) {
                    List<LngLat> coordinates = new ArrayList<>();
                    for (ValhallaLocation location : route.getGeometry()) {
                        coordinates.add(new LngLat(location.getLongitude(), location.getLatitude()));

                    }

                    routePlanner.setRoute(route);
                    Polyline polyline = new Polyline(coordinates);
                   // mapzenMap.addPolyline(polyline);

                    mapzenMap.drawRouteLine(coordinates);
                   // LngLat dest = coordinates.get(coordinates.size()-1);
                   // mapzenMap.drawDroppedPin(dest);
                    for (Marker point : routePlanner.getRoutePoints().keySet())
                    {
                        LngLat location = point.getLocation();
                        mapzenMap.drawDroppedPin(location);
                    }
                }

                @Override public void failure(int i) {
                    Log.d("Fail", "Failed to get route");
                    Toast.makeText(PlannerActivity.this,"Whoops!Something went wrong, check internet connection and restart application!", Toast.LENGTH_LONG).show();
                }
            });
        }



    private void configBtns() {

        btnPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reorderRoutePointList();
                expandableListView.setVisibility(View.GONE);
                btnInfo.setVisibility(View.VISIBLE);
                btnGoLayout.setVisibility(View.VISIBLE);
                btnPreview.setVisibility(View.GONE);

                updateRoute();


            }
        });

        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandableListView.setVisibility(View.VISIBLE);
                btnPreview.setVisibility(View.VISIBLE);
                btnInfo.setVisibility(View.GONE);
                btnGoLayout.setVisibility(View.GONE);

            }
        });
    }

    private void updateRoute() {

        mapzenRouter.clearLocations();
        mapzenMap.clearRouteLine();
        mapzenMap.removeMarker();
        mapzenMap.clearDroppedPins();
        Marker start =routePlanner.getStartPoint();
        LngLat startLngLat = start.getLocation();
        double[] startCoords = {startLngLat.latitude,startLngLat.longitude};
        mapzenRouter.setLocation(startCoords);

        for(Marker point :routePlanner.getRoutePoints().keySet())
        {
            LngLat location = point.getLocation();
            double[] coords={location.latitude,location.longitude};
            mapzenRouter.setLocation(coords);

        }

        mapzenRouter.fetch();




    }


    private void configList() {

        listGroups=new ArrayList<String>();
        listGroups.add("Route");
        listGroups.add("Other Markers");

        List<Map.Entry<Marker,String>> routePoints = new ArrayList<Map.Entry<Marker,String>>();

        for(Map.Entry<Marker,String> point : routePlanner.getRoutePoints().entrySet())
        {

            routePoints.add(point);
        }

        List<Map.Entry<Marker,String>> nonRoutePoints = new ArrayList<Map.Entry<Marker,String>>();

        for(Map.Entry<Marker,String> point : routePlanner.getNonRoutePoints().entrySet())
        {

            nonRoutePoints.add(point);
        }

        listChildren=new HashMap<String,List<Map.Entry<Marker,String>>>();

        listChildren.put(listGroups.get(0),routePoints);
        listChildren.put(listGroups.get(1),nonRoutePoints);

    }

    public void goClicked(View v)
    {
        switch (v.getId())
        {
            case R.id.textGo:
            case R.id.btnStartRoute:
                Intent intent = new Intent(PlannerActivity.this,TripActivity.class);
                startActivity(intent);
                break;
            default: break;

        }
    }


    @Override
    public void onBackPressed()
    {
        if(expandableListView.getVisibility() == View.VISIBLE) {
            if(adapter.isListOrderChanged())
                reorderRoutePointList();
            expandableListView.setVisibility(View.GONE);
            btnInfo.setVisibility(View.VISIBLE);
            btnGoLayout.setVisibility(View.VISIBLE);
            btnPreview.setVisibility(View.GONE);

        }
        else
            super.onBackPressed();
    }



    private void reorderRoutePointList() {
            HashMap<String, List<Map.Entry<Marker, String>>> adapterItems =adapter.getItems();

        List<Map.Entry<Marker, String>> adapterRoutePoints = adapterItems.get("Route");
        List<Map.Entry<Marker, String>> adapterNonRoutePoints = adapterItems.get("Other Markers");

        routePlanner.reorderPoints(adapterRoutePoints,adapterNonRoutePoints);
        setAdapterData();
        adapter.setListReordered();
    }

    private void setAdapterData() {
        configList();

        adapter.notifyDataSetChanged();

    }
}
