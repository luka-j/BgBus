package rs.luka.android.bgbus.ui;

import android.Manifest;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rs.luka.android.bgbus.R;
import rs.luka.android.bgbus.logic.Base;
import rs.luka.android.bgbus.logic.Pathfinder;
import rs.luka.android.bgbus.logic.providers.SearchProvider;
import rs.luka.android.bgbus.model.FullPath;
import rs.luka.android.bgbus.model.Line;
import rs.luka.android.bgbus.model.Station;

public class MapsActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
                                                               GoogleApiClient.OnConnectionFailedListener,
                                                               InfoDialog.Callbacks {

    public static final  String ACTION_DIRECTIONS               = "rs.luka.android.bgbus.intent.MAPS_SHOW_DIRECTIONS";
    public static final  String EXTRA_ROUTE                     = "bgbus.MapsActivity.route";
    public static final  String INTENT_ACTION_MOVE_TO_PLACE
                                                                = "rs.luka.android.bgbus.MOVE_MAP_CAMERA_TO_PLACE";
    public static final  String INTENT_ACTION_MOVE_TO_STATION
                                                                = "rs.luka.android.bgbus.MOVE_MAP_CAMERA_TO_STATION";
    public static final String INTENT_ACTION_SHOW_LINE = "rs.luka.android.bgbus.MOVE_MAP_CAMERA_TO_LINE";
    private static final int    REQUEST_SPLASH                  = 0;
    private static final String STATE_RESOLVING_ERROR           = "resolving_error";
    private static final String REQUEST_PERMISSIONS_INFO_DIALOG = "bgbus.permissionsInfo";
    private static final String REQUEST_ERROR_DIALOG            = "bgbus.permissionsDenied";

    private static final String TAG                        = "bgbus.MapsActivity";
    public static final  LatLng BELGRADE_EXTREME_SOUTHWEST = new LatLng(44.600266, 20.092623);
    public static final  LatLng BELGRADE_EXTREME_NORTHEAST = new LatLng(44.906855, 20.648886);
    private static final String EXTRA_LINE_NUMBER          = "rs.luka.android.bgbus.extra.line";
    private Toolbar   toolbar;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private static final float SHOW_MARKERS_THRESHOLD = 15.1f;
    private static final float DEFAULT_ZOOM_LEVEL     = 10.2f;
    private static final float CLOSEUP_MARKER_ZOOM    = 17;
    private static final int   DIRECTIONS_PADDING     = 40;

    private boolean showingPermissionsDialog = false;

    private Station realStart, realGoal;
    private Station      selectedStation;
    private boolean      returnToResults;
    private Parcelable[] allRoutes;
    private Parcelable[] allAlternatives;

    private Marker selectedMarker, locationMarker; //KOPIJE markera (anullirati ih, NE remove())
    private final Map<Marker, Station> markers           = new HashMap<>(5000);
    private       boolean              areMarkersShowing = false;
    private android.support.v4.widget.CursorAdapter suggestionsAdapter;
    private BitmapDescriptor                        defaultMarkerIcon;

    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        returnToResults = false;
        allRoutes = allAlternatives = null;

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        startSet = false;
        goalSet = false;
        registerForContextMenu(findViewById(android.R.id.content));

        GoogleApiClient.Builder apiClientBuilder = new GoogleApiClient.Builder(this, this, this);
        apiClientBuilder.addApi(Places.GEO_DATA_API);
        googleApiClient = apiClientBuilder.build();
        mResolvingError = savedInstanceState != null
                          && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        MapsInitializer.initialize(getApplicationContext());
        if (!Base.getInstance().isLoaded())
            startActivityForResult(new Intent(this, SplashActivity.class), REQUEST_SPLASH);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    private void checkPermissions() {
        try {
            if (showingPermissionsDialog) return;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                   != PackageManager.PERMISSION_GRANTED) {
                InfoDialog.newInstance(getString(R.string.request_permissions_title),
                                       getString(R.string.request_permissions_message))
                          .show(getSupportFragmentManager(), REQUEST_PERMISSIONS_INFO_DIALOG);
                showingPermissionsDialog = true;
            } else {
                getMapIfNeeded();
            }
        } catch (IllegalStateException ex) { //shit happens
            Log.e(TAG, "Oops! Illegal state while trying to display dialog");
            showingPermissionsDialog = false;
        }
    }

    @Override
    public void onDialogClosed(DialogFragment dialog) {
        showingPermissionsDialog = false;
        switch (dialog.getTag()) {
            case REQUEST_PERMISSIONS_INFO_DIALOG:
                ActivityCompat.requestPermissions(this,
                                                  new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                               android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                  0);
                break;
            case REQUEST_ERROR_DIALOG:
                finish();
                break;
            default:
                Log.e(TAG, "invalid dialog tag: " + dialog.getTag());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int res : grantResults) {
            if (res == PackageManager.PERMISSION_DENIED) {
                new Handler(getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InfoDialog.newInstance(getString(R.string.permissions_denied_title),
                                               getString(R.string.permissions_denied_message))
                                  .show(getSupportFragmentManager(), REQUEST_ERROR_DIALOG);
                    }
                }, 1); //workaround, see http://stackoverflow.com/a/34204394/4965786
                return;
            }
        }
        getMapIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
        realGoal = null;
        realStart = null;
        if (locationMarker != null && locationMarker.isVisible()) {
            locationMarker.setIcon(defaultMarkerIcon);
            locationMarker = null;
        }
    }

    private void clearExtraMarkers() {
        for (Marker marker : extraMarkers) {
            if (!marker.equals(locationMarker))
                marker.remove();
        }
        extraMarkers.clear();
        returnToResults = false;
        allRoutes = allAlternatives = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
        if (locationMarker != null) {
            locationMarker.setIcon(defaultMarkerIcon);
            locationMarker = null;
        }
        clearExtraMarkers();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.menu_station, menu);
    }

    private boolean startSet = false, goalSet = false;

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (locationMarker == null) {
            selectedMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location));
            locationMarker = selectedMarker;
        }
        switch (item.getItemId()) {
            case R.id.set_start:
                Base.getInstance().setStart(selectedStation.getId());
                realStart = selectedStation;
                if (realStart.equals(realGoal)) {
                    realGoal = null;
                    goalSet = false;
                }
                if (goalSet) {
                    startActivity(new Intent(this, ResultsActivity.class));
                    startSet = false;
                    goalSet = false;
                } else {
                    startSet = true;
                }
                return true;
            case R.id.set_goal:
                Base.getInstance().setGoal(selectedStation.getId());
                realGoal = selectedStation;
                if (realGoal.equals(realStart)) {
                    realStart = null;
                    startSet = false;
                }
                if (startSet) {
                    startActivity(new Intent(this, ResultsActivity.class));
                    startSet = false;
                    goalSet = false;
                } else {
                    goalSet = true;
                }
                return true;
        }
        return false;
    }

    private final Set<Marker> extraMarkers = new HashSet<>();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.i(TAG, "new intent; action: " + intent.getAction());
        if (INTENT_ACTION_MOVE_TO_STATION.equals(intent.getAction())) {
            if (intent.getStringExtra(SearchManager.EXTRA_DATA_KEY) == null) return;
            returnToResults = false;
            allRoutes = allAlternatives = null;
            String[] stationInfo = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY).split("#");
            String[] location    = stationInfo[1].split(",");
            double   lat         = Math.toDegrees(Double.parseDouble(location[0]));
            double   lng         = Math.toDegrees(Double.parseDouble(location[1]));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), CLOSEUP_MARKER_ZOOM));
            int stationId = Integer.parseInt(stationInfo[0]);
            prelaunchSearch(Base.getInstance().getStation(stationId));
            for (Map.Entry<Marker, Station> markerEntry : markers.entrySet()) { // TODO: 24.10.15. make BiMap
                if (markerEntry.getValue().getId() == stationId) {
                    markerEntry.getKey().showInfoWindow();
                    break;
                }
            }
        } else if (INTENT_ACTION_MOVE_TO_PLACE.equals(intent.getAction())) {
            returnToResults = false;
            allRoutes = allAlternatives = null;
            Places.GeoDataApi.getPlaceById(googleApiClient, intent.getStringExtra(SearchManager.EXTRA_DATA_KEY))
                             .setResultCallback(new ResultCallback<PlaceBuffer>() {
                                 @Override
                                 public void onResult(PlaceBuffer places) {
                                     if (places.getStatus().isSuccess()) {
                                         final Place selectedPlace = places.get(0);
                                         Marker m = mMap.addMarker(new MarkerOptions().title(selectedPlace.getName()
                                                                                                          .toString())
                                                                                      .position(selectedPlace.getLatLng())
                                                                                      .snippet(selectedPlace.getAddress()
                                                                                                            .toString())
                                                                                      .icon(BitmapDescriptorFactory.fromResource(
                                                                                              R.drawable.ic_place)));
                                         Station st = Base.getInstance()
                                                          .addArtificialStation(selectedPlace.getName().toString(),
                                                                                new rs.luka.android.bgbus.model.LatLng(
                                                                                        Math.toRadians(
                                                                                                selectedPlace.getLatLng().latitude),
                                                                                        Math.toRadians(
                                                                                                selectedPlace.getLatLng().longitude)));
                                         prelaunchSearch(st);
                                         //extraMarkers.add(m);
                                         markers.put(m, st);
                                         mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace.getLatLng(),
                                                                                           CLOSEUP_MARKER_ZOOM));
                                         m.showInfoWindow();
                                     }
                                     places.release();
                                 }
                             });
        } else if (ACTION_DIRECTIONS.equals(intent.getAction())) {
            returnToResults = true;
            allRoutes = intent.getParcelableArrayExtra(ResultsActivity.EXTRA_ALL_ROUTES);
            allAlternatives = intent.getParcelableArrayExtra(ResultsActivity.EXTRA_ALL_ALTERNATIVES);
            FullPath                       route = intent.getParcelableExtra(EXTRA_ROUTE);
            drawRoute(route);
        } else if (INTENT_ACTION_SHOW_LINE.equals(intent.getAction()))  {
            returnToResults = false;
            allRoutes = allAlternatives = null;
            String lineType = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
            String lineId = lineType.split("-")[0];
            String lineDir = lineType.split("-")[1];
            Line   line = Base.getInstance().getLine(lineId);
            FullPath route;
            switch (lineDir) {
                case "A":
                    route = line.getStations(Line.SMER_A);
                    break;
                case "B":
                    route = line.getStations(Line.SMER_B);
                    break;
                default:
                    Log.e(TAG, "invalid direction (not A or B)");
                    return;
            }
            for(Marker m : extraMarkers)
                m.remove();
            extraMarkers.clear();
            drawRoute(route);
        }
    }

    private void drawRoute(FullPath route) {
        List<FullPath.LineStationPair> path  = route.getPath();
        for (FullPath.LineStationPair stop : path) {
            int iconResId = stop.getLine().getDrawableResId();
            extraMarkers.add(mMap.addMarker(new MarkerOptions().title(stop.getStation().getName())
                                                               .snippet(stop.getLine().getId())
                                                               .position(new LatLng(stop.getStation()
                                                                                        .getLocation()
                                                                                        .getLatitudeInDegrees(),
                                                                                    stop.getStation()
                                                                                        .getLocation()
                                                                                        .getLongitudeInDegrees()))
                                                               .icon(BitmapDescriptorFactory.fromResource(
                                                                       iconResId))));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(route.getBounds(), DIRECTIONS_PADDING));
    }

    @Override
    protected void onResumeFragments() { //if called in onResume, causes state loss
        super.onResumeFragments();

        if (Base.getInstance().isLoaded() && !isFinishing())
            setUpMapAndPermissions();
    }

    private void setUpMapAndPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions();
        } else {
            getMapIfNeeded();
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void getMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            Log.d(TAG, "Trying to obtain map");
            // Try to obtain the map from the SupportMapFragment.
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    mMap = googleMap;
                    Log.d(TAG, "Got map, " + (mMap==null?"null":"not null"));
                    setupMapIfNeeded();
                }
            });
        }
        if(defaultMarkerIcon == null)
            defaultMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
    }

    private void setupMapIfNeeded() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
               == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Got location permissions, my location enabled");
            mMap.setMyLocationEnabled(true);
        }
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        // Check if we were successful in obtaining the map.
        if (mMap != null) {
            Log.d(TAG, "Map isn't null, setting up");
            setUpMap();
        }
    }

    private void prelaunchSearch(Station station) {
        if(realStart == null && realGoal != null) {
            Base.getInstance().setStart(station.getId());
            Pathfinder.setGoal(null, getApplicationContext());
        } else if(realStart != null && realGoal == null) {
            Base.getInstance().setGoal(station.getId());
            Pathfinder.setGoal(null, getApplicationContext());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return false;
    }

    /**
     * This is where we can add markers or data, add listeners or move the camera.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        Iterator<Station> stations = Base.getInstance().getStationIterator();
        Log.i(TAG, "Setting up maps; num of stations: " + Base.getInstance().getStationCount());
        Station           curr;
        while (stations.hasNext()) {
            curr = stations.next();
            Marker m = mMap.addMarker(new MarkerOptions().position(new LatLng(curr.getLocation()
                                                                                  .getLatitudeInDegrees(),
                                                                              curr.getLocation()
                                                                                  .getLongitudeInDegrees()))
                                                         .title(curr.getName())
                                                         .snippet(curr.getLineList())
                                                         .icon(defaultMarkerIcon)
                                                         .visible(false));
            markers.put(m, curr);
        }
        Log.d(TAG, "Put up all markers");
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                if (markers.containsKey(marker))
                    prelaunchSearch(markers.get(marker));
                return true;
            }
        });

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(44.809026, 20.461184), DEFAULT_ZOOM_LEVEL));
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (cameraPosition.zoom > SHOW_MARKERS_THRESHOLD && !areMarkersShowing) {
                    for (Marker m : markers.keySet()) m.setVisible(true);
                    areMarkersShowing = true;
                } else if (cameraPosition.zoom <= SHOW_MARKERS_THRESHOLD && areMarkersShowing) {
                    for (Marker m : markers.keySet())
                        if(!m.equals(locationMarker))
                            m.setVisible(false);
                    areMarkersShowing = false;
                }
            }
        });
        Log.d(TAG, "Set up all listeners, positioned map");
        /*mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {

                View v = getLayoutInflater().inflate(R.layout.info_station,
                                                     (ViewGroup) MapsActivity.this.findViewById(
                                                             android.R.id.content),
                                                     false);
                TextView stationName = (TextView) v.findViewById(R.id.station_name);
                stationName.setText(markers.get(marker).getName());
                TextView data = (TextView) v.findViewById(R.id.station_lines);
                data.setText(markers.get(marker).getLineList());
                return v;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });*/
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                selectedMarker = marker;
                if(markers.containsKey(marker)) selectedStation = markers.get(marker);
                openContextMenu(MapsActivity.this.findViewById(android.R.id.content));
                if(!extraMarkers.contains(marker))
                    clearExtraMarkers();
                marker.hideInfoWindow();
            }
        });
    }

    private SearchView searchView;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        // Get the SearchView and set the searchable configuration

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.search_icon).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setOnQueryTextListener(new SearchQueryTextListener());

        SearchView.SearchAutoComplete autoCompleteTextView = (SearchView.SearchAutoComplete) searchView.findViewById(R.id.search_src_text); //workaround
        if (autoCompleteTextView != null) {
            autoCompleteTextView.setDropDownBackgroundDrawable(getResources().getDrawable(R.drawable.abc_popup_background_mtrl_mult));
        }

        suggestionsAdapter = searchView.getSuggestionsAdapter();
        searchView.setQueryRefinementEnabled(false);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        return true;
    }

    public void addSuggestions(final Cursor newSuggestions) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(suggestionsAdapter == null) {
                    suggestionsAdapter = searchView.getSuggestionsAdapter();
                    if(suggestionsAdapter == null) {
                        Log.e(TAG, "Suggestions adapter is null");
                        return;
                    }
                }
                Log.i(TAG, "Adding additional suggestions");
                MatrixCursor allSuggestions = new MatrixCursor(new String[]{BaseColumns._ID,
                                                                            SearchManager.SUGGEST_COLUMN_TEXT_1,
                                                                            SearchManager.SUGGEST_COLUMN_TEXT_2,
                                                                            SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
                                                                            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA});
                Cursor currentSuggestions = suggestionsAdapter.getCursor();
                if (currentSuggestions != null) {
                    currentSuggestions.moveToFirst();
                    while (!currentSuggestions.isAfterLast()) {
                        allSuggestions.addRow(new String[]{String.valueOf(currentSuggestions.getInt(0)),
                                                           currentSuggestions.getString(1),
                                                           currentSuggestions.getString(2),
                                                           null,
                                                           currentSuggestions.getString(4)});
                        currentSuggestions.moveToNext();
                    }
                }
                newSuggestions.moveToFirst();
                while (!newSuggestions.isAfterLast()) {
                    allSuggestions.addRow(new String[]{String.valueOf(newSuggestions.getInt(0)),
                                                       newSuggestions.getString(1), newSuggestions.getString(2),
                                                       newSuggestions.getString(3), newSuggestions.getString(4)});
                    newSuggestions.moveToNext();
                }
                suggestionsAdapter.changeCursor(allSuggestions);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(returnToResults) {
            Intent resultsIntent = new Intent(this, ResultsActivity.class);
            resultsIntent.putExtra(ResultsActivity.EXTRA_ALL_ROUTES, allRoutes);
            resultsIntent.putExtra(ResultsActivity.EXTRA_ALL_ALTERNATIVES, allAlternatives);
            startActivity(resultsIntent);
        } else {
            super.onBackPressed();
        }
    }

    private class SearchQueryTextListener implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(final String newText) {
            SearchProvider.queryPlaces(newText, MapsActivity.this, googleApiClient);
            return false;
        }
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Successfully connected to Google services");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.e(TAG, "Connection to Google services suspended. Cause: " + cause);
    }


    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";
    private boolean mResolvingError = false;

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                googleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data); //without this line, displaying dialog causes state loss
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!googleApiClient.isConnecting() &&
                    !googleApiClient.isConnected()) {
                    googleApiClient.connect();
                }
            }
        }

        if(requestCode == REQUEST_SPLASH) { //not sure if this is necessary
            if(mMap == null)
                setUpMapAndPermissions();
            else //force add markers
                setUpMap();
        }
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MapsActivity) getActivity()).onDialogDismissed();
        }
    }
}
