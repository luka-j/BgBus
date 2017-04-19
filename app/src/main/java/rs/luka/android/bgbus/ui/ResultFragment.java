package rs.luka.android.bgbus.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import rs.luka.android.bgbus.R;
import rs.luka.android.bgbus.model.FullPath;

public class ResultFragment extends Fragment {

    public static final int ID_REFRESH = 0xB00B; //trying to make it unique

    private static final String ARG_ROUTE       = "aRoute";
    private static final String ARG_ALTERNATIVE = "aAltRoute";
    private LinearLayout         result;
    private CircularProgressView progress;
    private FullPath             route;
    private FullPath             alternative;
    private ResultCallbacks      callbacks;

    public static ResultFragment newInstance(FullPath route, FullPath alternative) {
        ResultFragment fragment = new ResultFragment();
        Bundle         args     = new Bundle(1);
        args.putParcelable(ARG_ROUTE, route);
        args.putParcelable(ARG_ALTERNATIVE, alternative);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //DO NOT ATTACH TO ROOT!!! Causes OOM
        View view = inflater.inflate(R.layout.fragment_result, container, false);

        result = (LinearLayout) view.findViewById(R.id.container_route);
        progress = (CircularProgressView) view.findViewById(R.id.progress_view);
        route = getArguments().getParcelable(ARG_ROUTE);
        alternative = getArguments().getParcelable(ARG_ALTERNATIVE);
        if(alternative != null && !alternative.isEmpty()) {
            getActivity().supportInvalidateOptionsMenu();
            Log.i("ResFragment", "Has alternative");
        }
        if(route != null && !route.isEmpty()) {
            displayRoute(route);
            progress.setVisibility(View.GONE);
        }

        return view;
    }

    private void displayRoute(FullPath route) {
        Context context = getContext();
        if(context == null) return;
        result.removeAllViews();
        FullPath reducedRoute = route.reduce();
        for(FullPath.LineStationPair pair : reducedRoute.getPath()) {
            int drawableResId = pair.getLine().getDrawableResId();
            TextView tv = new TextView(context);
            tv.setPadding(0, 5, 0, 5);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setCompoundDrawablesWithIntrinsicBounds(drawableResId, 0, 0, 0);
            tv.setCompoundDrawablePadding(5);
            tv.setText(pair.toDisplayString(context));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            result.addView(tv);
        }
        TextView tv = new TextView(context);
        tv.setPadding(0, 10, 0, 5);
        tv.setText(getString(R.string.est_time, Math.round(route.getTime())));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        result.addView(tv);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(alternative!=null && !alternative.isEmpty())
            getActivity().supportInvalidateOptionsMenu(); //I doubt this will ever get executed (FSPA destroys fragments)
    }

    public void setRoute(FullPath route) {
        this.route = route;
        displayRoute(this.route);
        progress.setVisibility(View.GONE);
    }

    public void setAlternativeRoute(FullPath route) {
        alternative = route;
        if(getActivity() != null)
            getActivity().supportInvalidateOptionsMenu();
    }

    public void setCallbacks(ResultCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(alternative != null && !"null".equals(alternative)) {
            Log.i("ResFragment", "Adding refresh to menu");
            menu.add(Menu.NONE, ID_REFRESH, 10, R.string.refresh).setIcon(R.drawable.ic_refresh)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        inflater.inflate(R.menu.menu_result, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id) {
            case R.id.item_directions:
                callbacks.requestDirections(this);
                return true;
            case ID_REFRESH:
                route = alternative;
                alternative = null;
                displayRoute(route);
                if(callbacks != null) callbacks.notifyRouteRefreshed(this);
                getActivity().supportInvalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public interface ResultCallbacks {
        void notifyRouteRefreshed(ResultFragment caller);
        void requestDirections(ResultFragment caller);
    }
}
