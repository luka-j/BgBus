package rs.luka.android.bgbus.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;

import java.util.List;

import rs.luka.android.bgbus.R;
import rs.luka.android.bgbus.logic.Pathfinder;
import rs.luka.android.bgbus.misc.Utils;
import rs.luka.android.bgbus.model.FullPath;

/**
 * Activity used for displaying results. Target for callbacks in case pathfinding isn't complete after
 * this activity goes on screen.
 * Created by luka on 6.11.15.
 */
public class ResultsActivity extends AppCompatActivity implements Pathfinder.FinderCallbacks {
    private static final String     TAG                    = "bgbus.ResultsActivity";
    public static final  String     EXTRA_ALL_ROUTES       = "routeArray";
    public static final  String     EXTRA_ALL_ALTERNATIVES = "alternativeArray";
    private static final int        TAB_COUNT              = 4;
    private        final boolean[]  visited                = new boolean[TAB_COUNT];
    private              FullPath[] routes                 = new FullPath[TAB_COUNT];
    private              FullPath[] alternatives           = new FullPath[TAB_COUNT];
    private TabLayout      tabLayout;
    private Toolbar        toolbar;
    private ResultsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        adapter = new ResultsAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);

        // Give the TabLayout the ViewPager
        tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void insertRoute(FullPath route) {
        for (int i = 0; i < TAB_COUNT; i++) //duplicate check
            if (routes[i] != null && Utils.timesEqual(route.getTime(), routes[i].getTime()))
                return;

        for (int i = 0; i < TAB_COUNT; i++) { //placing it into appropriate position, if exists
            if (routes[i] == null || (route.getTime() < routes[i].getTime() && !visited[i])) {
                routes[i] = route;
                if(route != null)
                    adapter.refreshFragment(i);
                return;
            }
        }

        // TODO: 7.11.15. fix mess
        FullPath max=routes[0]; int maxi=0;
        for(int i=1; i<TAB_COUNT; i++) { //if not, setting it as an alternative (if suitable)
            if(routes[i].getTime() > max.getTime() && (alternatives[i]==null || alternatives[i].getTime() > route.getTime())) {
                max = routes[i];
                maxi = i;
            }
        }
        if(routes[maxi].getTime() > route.getTime() && (alternatives[maxi]==null || alternatives[maxi].getTime() > route.getTime())) {
            alternatives[maxi] = route;
            adapter.setRefreshable(maxi, route);
            Log.i("Results", "Found better alternative for " + maxi);
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(getIntent().getParcelableArrayExtra(EXTRA_ALL_ROUTES) == null) {
            List<FullPath> results = Pathfinder.getResults(this);
            for (FullPath res : results) {
                insertRoute(res);
            }
        } else {
            Log.i(TAG, "Got all routes. Size: " + getIntent().getParcelableArrayExtra(EXTRA_ALL_ROUTES).length);
            Parcelable[] allRoutesParc = getIntent().getParcelableArrayExtra(EXTRA_ALL_ROUTES);
            Parcelable[] allAlternativesParc = getIntent().getParcelableArrayExtra(EXTRA_ALL_ALTERNATIVES);
            FullPath[] allRoutes = new FullPath[allRoutesParc.length];
            FullPath[] allAlternatives = new FullPath[allAlternativesParc.length];
            Utils.castFromParcelableArray(allRoutesParc, allRoutes);
            Utils.castFromParcelableArray(allAlternativesParc, allAlternatives);
            routes = allRoutes;
            alternatives = allAlternatives;
            for(int i=0; i<TAB_COUNT; i++) {
                if(routes[i] != null)
                    adapter.refreshFragment(i);
                if(alternatives[i] != null)
                    adapter.setRefreshable(i, alternatives[i]);
            }
        }
    }

    public void addRoute(final FullPath res) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                insertRoute(res);
                Log.i("Results", "Adding route w/ est time " + res.getTime());
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Pathfinder.abort();
    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
    }

    //FragmentPagerAdapter is acting buggy (discards route when loses focus and keeps progress view spinning)
    private class ResultsAdapter extends FragmentStatePagerAdapter implements ResultFragment.ResultCallbacks {
        private SparseArray<ResultFragment> fragments = new SparseArray<>(TAB_COUNT);

        private final String genericTabTitle = getString(R.string.tab_title_searching);

        public ResultsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @Override
        public Fragment getItem(int position) {
            ResultsActivity.this.visited[position] = true;
            ResultFragment fragment = ResultFragment.newInstance(ResultsActivity.this.routes[position],
                                                                 ResultsActivity.this.alternatives[position]);
            fragment.setCallbacks(this);
            fragments.put(position, fragment);
            return fragment;
        }

        public void refreshFragment(int position) {
            if(fragments.get(position) != null)
                fragments.get(position).setRoute(ResultsActivity.this.routes[position]);
            tabLayout.getTabAt(position).setText(getString(R.string.tab_title_found, Math.round(routes[position].getTime())));
        }

        public void setRefreshable(int position, FullPath betterSol) {
            if(fragments.get(position) != null)
                fragments.get(position).setAlternativeRoute(betterSol);
            tabLayout.getTabAt(position).setIcon(R.drawable.ic_refresh);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // Generate title based on item position
            return genericTabTitle;
        }

        @Override
        public void notifyRouteRefreshed(ResultFragment caller) {
            for(int i=0; i<TAB_COUNT; i++) {
                if(fragments.get(i) == caller) {
                    ResultsActivity.this.routes[i] = ResultsActivity.this.alternatives[i];
                    tabLayout.getTabAt(i).setText(getString(R.string.tab_title_found, Math.round(routes[i].getTime())));
                    tabLayout.getTabAt(i).setIcon(null);
                    ResultsActivity.this.alternatives[i] = null;
                }
            }
        }

        @Override
        public void requestDirections(ResultFragment caller) {
            for(int i=0; i<TAB_COUNT; i++) {
                if(fragments.get(i) == caller) {
                    if(routes[i] == null) return;
                    Intent mapDirections = new Intent(ResultsActivity.this, MapsActivity.class);
                    mapDirections.setAction(MapsActivity.ACTION_DIRECTIONS);
                    mapDirections.putExtra(MapsActivity.EXTRA_ROUTE, routes[i]);
                    mapDirections.putExtra(EXTRA_ALL_ROUTES, routes);
                    mapDirections.putExtra(EXTRA_ALL_ALTERNATIVES, alternatives);
                    mapDirections.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(mapDirections);
                    return;
                }
            }
        }
    }
}
