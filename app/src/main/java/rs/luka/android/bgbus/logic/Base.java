package rs.luka.android.bgbus.logic;

import android.content.Context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import rs.luka.android.bgbus.io.BaseIO;
import rs.luka.android.bgbus.misc.Config;
import rs.luka.android.bgbus.model.LatLng;
import rs.luka.android.bgbus.model.Line;
import rs.luka.android.bgbus.model.Station;

/**
 * Singleton u kojem se nalaze svi podaci. S obzirom da bi trebalo da odavde poticu svi objekti, bezbedno je koristiti
 * == za poredjenje. Ja to nisam radio iz navike (negde nije overridovan equals, pa se svodi na isto).
 * Plan je bio da prosiruje SQLiteOpenHelper i ucitava podatke iz prave baze, a ne da cuva mape u memoriji. Zbog
 * toga se zove Base (a ne Holder) i singleton (a ne klasa sa static metodama). Zbog nedostatka vremena, ostavljeno
 * u ovom obliku, koji je bio namenjen racunarima (>1GB RAM), pre nego sto sam uopste nameravao da ideju primenim u
 * Android aplikaciji.
 * Created by luka on 14.9.15.
 */
public class Base {
    private static Base instance;
    private final Map<Integer, Station> stations = new HashMap<>();
    private final Map<String, Line> lines = new HashMap<>();
    private Station goal;
    private Station start;

    private Base() {
    }

    public static Base getInstance() {
        if(instance == null) {
            instance = new Base();
        }
        return instance;
    }

    public void addStation(Station s) {
        stations.put(s.getId(), s);
    }
    public void addLine(Line l) {
        lines.put(l.getId(), l);
    }

    /**
     * Najsporiji deo programa (za first run i {@link Config#USE_CACHE}=false)
     * @deprecated precalculated za aplikaciju, podaci se nalaze u fajlu
     */
    public void setWalkingPaths() {
        long start = System.currentTimeMillis();
        Collection<Station> allStations = stations.values();
        for(Station st : allStations) {
            st.registerStations(allStations);
        }
        long end = System.currentTimeMillis();
        if(Config.DEBUG) System.out.println("Time spent in setWalkingPaths: " + (end - start) / 1000.0);
    }

    /*
     * Dobijanje linija i stanica na osnovu id-a je O(1)
     */

    public Station getStation(int id) {
        return stations.get(id);
    }

    public Line getLine(String id) {
        if(Line.getSpecial(id) == null)
            return lines.get(id);
        return Line.getSpecial(id);
    }

    public Iterator<Station> getStationIterator() {
        return stations.values().iterator();
    }

    public Station getGoal() {
        return goal;
    }

    public void setGoal(int goalStationId) {
        goal = getStation(goalStationId);
        if(goal==null) throw new NullPointerException("Goal ne sme biti null");
        invalidateGoal();
    }

    private void invalidateGoal() {
        for(Station st : stations.values())
            st.invalidateBearings();
    }

    public Station getStart() {
        return start;
    }

    public void setStart(int startId) {
        this.start = getStation(startId);
        if (this.start == null) throw new NullPointerException("Invalid start id");
    }

    public void clearPoints() {start=null; goal=null; invalidateGoal();}
    public void clearStart() {start=null;}

    public void load(Context context) {
        if(isLoaded()) return;
        long start = System.currentTimeMillis();
        BaseIO.loadBase(this, context);
        long end = System.currentTimeMillis();
        if(Config.DEBUG) System.out.println("Loading time: " + (end-start)/1000.0);
    }

    public void resetStations() {
        for(Station st : stations.values())
            st.reset();
    }

    public int getStationCount() {
        return stations.size();
    }

    public boolean isLoaded() {
        return !stations.isEmpty() && !lines.isEmpty();
    }

    private List<Station> getPossibleWalkingDests(LatLng from) {
        List<Station> res = new LinkedList<>();
        for(Station st : stations.values()) {
            if(from.distanceBetween(st.getLocation()) < AlgorithmParameters.getMaxInitialWalkingDistance())
                res.add(st);
        }
        if(res.isEmpty()) {
            Station min = null;
            double dist = Double.POSITIVE_INFINITY, curr;
            for(Station st : stations.values())
                if((curr=from.distanceBetween(st.getLocation())) < dist) {
                    min = st;
                    dist = curr;
                }
            res.add(min);
        }
        return res;
    }

    private int nextAvailableId = -1;

    public Station addArtificialStation(String name, LatLng point) {
        Station art = new Station(nextAvailableId, name, point, getPossibleWalkingDests(point));
        stations.put(nextAvailableId, art);
        nextAvailableId--;
        return art;
    }
}
