package rs.luka.android.bgbus.logic.providers;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import rs.luka.android.bgbus.R;
import rs.luka.android.bgbus.logic.Base;
import rs.luka.android.bgbus.misc.LocationFinder;
import rs.luka.android.bgbus.model.Line;
import rs.luka.android.bgbus.model.Station;
import rs.luka.android.bgbus.ui.MapsActivity;

/**
 * Provides search results based on {@link LocationFinder}
 * Created by luka on 23.10.15..
 */
public class SearchProvider extends ContentProvider {
    private static final int API_REQUEST_DELAY = 800; //in ms

    private static final String TAG         = "bgbus.SearchProvider";
    public static final  String AUTHORITY   = "rs.luka.android.bgbus.data.providers.SearchProvider";
    public static final  Uri    CONTENT_URI = Uri.parse(
            "content://" + AUTHORITY + "/" + SearchManager.SUGGEST_URI_PATH_QUERY);

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final String query = uri.getLastPathSegment().toLowerCase();
        Log.d(TAG, "Query term: " + query);
        final MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID,
                                                                  SearchManager.SUGGEST_COLUMN_TEXT_1,
                                                                  SearchManager.SUGGEST_COLUMN_TEXT_2,
                                                                  SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
                                                                  SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA});
        if("search_suggest_query".equals(query)) return cursor;
        Line line;
        if((line = Base.getInstance().getLine(query)) != null) {
            cursor.addRow(new Object[]{-3, line.getId() + "-A", getContext().getString(R.string.show_stations),
                                       MapsActivity.INTENT_ACTION_SHOW_LINE, line.getId()+"-A"});
            cursor.addRow(new Object[]{-2, line.getId() + "-B", getContext().getString(R.string.show_stations),
                                       MapsActivity.INTENT_ACTION_SHOW_LINE, line.getId()+"-B"});
        } else { //workaround (todo)
            List<Station> localResults = LocationFinder.find(query);
            Log.d(TAG, "Results: " + localResults.size());
            for (Station res : localResults)
                cursor.addRow(new Object[]{res.getId(), res.getName(), res.getLineList(),
                                           MapsActivity.INTENT_ACTION_MOVE_TO_STATION,
                                           res.toExtraString()});
        }
        return cursor;
    }

    private static final ExecutorService netExecutor = Executors.newSingleThreadExecutor();
    private static Future currentTask;

    /**
     * Queries places using Places API after a delay
     * @param query query to search for
     * @param ac activity containing appropriate callback
     * @param apiClient GoogleApiClient used for request
     */
    public static void queryPlaces(final String query, final MapsActivity ac, final GoogleApiClient apiClient) {
        if (currentTask != null) currentTask.cancel(true);

        currentTask = netExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(API_REQUEST_DELAY);
                    MatrixCursor results = new MatrixCursor(new String[]{BaseColumns._ID,
                                                                         SearchManager.SUGGEST_COLUMN_TEXT_1,
                                                                         SearchManager.SUGGEST_COLUMN_TEXT_2,
                                                                         SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
                                                                         SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA});
                    AutocompletePredictionBuffer autocompleteBuffer =
                            Places.GeoDataApi.getAutocompletePredictions(apiClient, query,
                                                                         new LatLngBounds(MapsActivity.BELGRADE_EXTREME_SOUTHWEST,
                                                                                          MapsActivity.BELGRADE_EXTREME_NORTHEAST),
                                                                         null).await();
                    int i = 0, mask = 1 << 31;
                    for (AutocompletePrediction prediction : autocompleteBuffer) {
                        results.addRow(new Object[]{mask & i,
                                                    prediction.getPrimaryText(null),
                                                    prediction.getSecondaryText(null),
                                                    MapsActivity.INTENT_ACTION_MOVE_TO_PLACE,
                                                    prediction.getPlaceId()});
                    }
                    ac.addSuggestions(results);
                    autocompleteBuffer.release();
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        });
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return "vnd.android.cursor.item/vnd.rs.luka.android.bgbus.model.Station";
    }

    /**
     * Not supported
     * @param uri
     * @param values
     * @return
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    /**
     * Not supported
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    /**
     * Not supported
     * @param uri
     * @param values
     * @return
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
