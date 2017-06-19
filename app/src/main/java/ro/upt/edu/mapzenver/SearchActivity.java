package ro.upt.edu.mapzenver;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ButtonBarLayout;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.mapzen.android.graphics.MapView;
import com.mapzen.android.graphics.MapzenMap;
import com.mapzen.android.graphics.MapzenMapPeliasLocationProvider;
import com.mapzen.android.graphics.OnMapReadyCallback;
import com.mapzen.android.graphics.model.Marker;
import com.mapzen.android.search.MapzenSearch;
import com.mapzen.pelias.gson.Feature;
import com.mapzen.pelias.gson.Result;
import com.mapzen.pelias.widget.AutoCompleteAdapter;
import com.mapzen.pelias.widget.AutoCompleteListView;
import com.mapzen.pelias.widget.PeliasSearchView;
import com.mapzen.tangram.LngLat;
import com.mapzen.tangram.TouchInput;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Demonstrates use of {@link MapzenSearch} with a {@link PeliasSearchView} and {@link MapzenMap}.
 * Allows the user to search for a place, displays autocomplete results in a list and search
 * results on a map.
 */
public class SearchActivity extends BaseActivity {

    AutoCompleteListView listView;

    LostApiConnection lostApiConnection;

    PeliasSearchView peliasSearchView;

    MapzenMapPeliasLocationProvider peliasLocationProvider;
    MapzenMap mapzenMap;
    MapzenSearch mapzenSearch;
    ImageButton btnAdd;
    ImageButton btnClear;
    FloatingActionButton fabGo;
    ButtonBarLayout btnBar;
    List<LngLat> routePoints;
    Marker currentMarker;
    RoutePlanner routePlanner;

    AutoCompleteAdapter autocompleteAdapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        btnAdd = (ImageButton) findViewById(R.id.buttonAdd);
        btnClear = (ImageButton) findViewById(R.id.buttonClear);
        fabGo   = (FloatingActionButton) findViewById(R.id.fabGo);
        btnBar = (ButtonBarLayout) findViewById(R.id.btnBar);
        routePoints= new ArrayList<LngLat>();
        routePlanner= RoutePlanner.getInstance();
        routePlanner.setContext(this);


        listView = (AutoCompleteListView) findViewById(R.id.list_view);
        autocompleteAdapter = new SearchListAdapter(this, R.layout.list_item_double);



        listView.setAdapter(autocompleteAdapter);

        peliasLocationProvider = new MapzenMapPeliasLocationProvider(this);

        mapzenSearch = new MapzenSearch(this);
        mapzenSearch.setLocationProvider(peliasLocationProvider);
        peliasSearchView = new PeliasSearchView(this);
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT);
        getSupportActionBar().setCustomView(peliasSearchView, lp);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        MapView mapView = (MapView) findViewById(R.id.map_view);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override public void onMapReady(MapzenMap mapzenMap) {
                SearchActivity.this.mapzenMap = mapzenMap;
                lostApiConnection=new LostApiConnection(SearchActivity.this,mapzenMap);
                mapzenMap.setPersistMapData(true);
                configMap();
                setupPeliasSearchView(peliasSearchView);
            }
        });



        configBtns();

    }

    private void configBtns() {

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentMarker !=null) {
                    routePlanner.addCurrentMarkerToRoute(currentMarker);
                    Toast.makeText(SearchActivity.this,"marker added to route!",Toast.LENGTH_SHORT).show();
                    currentMarker=null;
                }
                else
                {
                    Toast.makeText(SearchActivity.this,"No marker to add, long press on a spot on the map first!",Toast.LENGTH_LONG).show();
                }


            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapzenMap.removeMarker();
                routePlanner.clearAll();
                currentMarker=null;
                Toast.makeText(SearchActivity.this,"All markers clearead, start over!",Toast.LENGTH_LONG).show();
                btnBar.setVisibility(View.GONE);
                fabGo.setVisibility(View.GONE);

            }
        });

        fabGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(SearchActivity.this, PlannerActivity.class);
                startActivity(intent);
                routePlanner.setStartPoint(lostApiConnection.getMyLocation());
            }
        });
    }

    private void configMap() {
        peliasLocationProvider.setMapzenMap(mapzenMap);
        mapzenMap.setMyLocationEnabled(true);

        mapzenMap.setCompassButtonEnabled(true);
        mapzenMap.setZoom(15f);

        mapzenMap.setLongPressResponder(new TouchInput.LongPressResponder() {
            @Override
            public void onLongPress(float x, float y) {
                LngLat screenLngLat = mapzenMap.screenPositionToLngLat(new PointF(x,y));
                Marker marker = new Marker(screenLngLat.longitude,screenLngLat.latitude);
                routePlanner.addPoint(marker);

                mapzenMap.addMarker(marker);
                currentMarker=marker;


                //Toast.makeText(SearchActivity.this,"click the +",Toast.LENGTH_LONG).show();
                btnBar.setVisibility(View.VISIBLE);
                fabGo.setVisibility(View.VISIBLE);


            }
        });



    }

    private void setupPeliasSearchView(PeliasSearchView searchView) {
        searchView.setAutoCompleteListView(listView);
        searchView.setPelias(mapzenSearch.getPelias());


        searchView.setCallback(new Callback<Result>() {
            @Override public void onResponse(Call<Result> call, Response<Result> response) {
                mapzenMap.clearSearchResults();

                Feature feature = response.body().getFeatures().get(0);
                List<Double> coordinates = feature.geometry.coordinates;
                LngLat point = new LngLat(coordinates.get(0), coordinates.get(1));
                Marker marker = new Marker(point.longitude,point.latitude);
                currentMarker=marker;
                mapzenMap.setPosition(point, 1000);
                mapzenMap.setZoom(15f,250);
                routePlanner.addPoint(marker);

                mapzenMap.addMarker(marker);

                btnBar.setVisibility(View.VISIBLE);
                fabGo.setVisibility(View.VISIBLE);
            }

            @Override public void onFailure(Call<Result> call, Throwable t) {
            }
        });
        searchView.setIconifiedByDefault(false);
        mapzenSearch.getPelias().setDebug(true);

        searchView.setQueryHint(this.getString(R.string.search_hint));
        searchView.setCacheSearchResults(true);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                if(listView.getVisibility() != View.VISIBLE)
                    listView.setVisibility(View.VISIBLE);
                return false;
            }
        });


        searchView.setOnBackPressListener(new PeliasSearchView.OnBackPressListener() {
            @Override public void onBackPressed() {
                mapzenMap.clearSearchResults();
            }
        });






    }

    @Override protected void onDestroy() {
        super.onDestroy();
        mapzenMap.setPersistMapData(false);

        if(lostApiConnection.connected == true)
            lostApiConnection.getClient().disconnect();
    }
}