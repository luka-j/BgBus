package rs.luka.android.bgbus.misc;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import rs.luka.android.bgbus.logic.Base;
import rs.luka.android.bgbus.model.Station;

/**
 * Not thoroughly tested, but should work. Provides utility methods for searching for stops (string algorithms).
 * Created by luka on 23.10.15..
 */
public class LocationFinder { // TODO: 3.11.15. find better name

    public static List<Station> find(String query) {
        Log.d("LFinder", "Starting search for " +query);
        //return incompleteBinarySearch(LetterUtils.removeSpecialChars(query));
        return stupidSearch(query);
    }

    private static class StationIntPair implements Comparable<StationIntPair> {
        private final Station station;
        private final int num;

        private StationIntPair(Station station, int num) {
            this.station = station;
            this.num = num;
        }

        @Override
        public int compareTo(@NonNull StationIntPair another) {
            if(num < another.num)
                return -1;
            if(num > another.num)
                return 1;
            return 0;
        }
    }
    private static List<Station> stupidSearch(String query) {
        Iterator<Station> stationIterator = Base.getInstance().getStationIterator();
        List<StationIntPair> results = new LinkedList<>();
        Station next;
        while(stationIterator.hasNext()) {
            next= stationIterator.next();
            String genericName = next.getAsciiName().toLowerCase();
            int dist = optimalStringAlignmentDistance(query, genericName);
            if (dist < Math.max(genericName.length() - query.length(), 0) + query.length()/6 + 1) {
                Log.v("bgbus.LocationFinder", "Found; distance: " + dist);
                results.add(new StationIntPair(next, dist));
            }
        }
        Collections.sort(results);
        List<Station> stations = new LinkedList<>();
        for(StationIntPair pair : results)
            stations.add(pair.station);
        return stations;
    }

    //Wikipedia copy paste, direktno preveden u Javu

    private static int optimalStringAlignmentDistance(String  s, String t) {
        // Determine the "optimal" string-alignment distance between s and t
        int m = s.length();
        int n = t.length();

  /* For all i and j, d[i][j] holds the string-alignment distance
   * between the first i characters of s and the first j characters of t.
   * Note that the array has (m+1)x(n+1) values.
   */
        int[][] d = new int[m+1][];
        for (int i = 0; i <= m; i++) {
            d[i] = new int[n+1];
            d[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            d[0][j] = j;
        }

        // Determine substring distances
        int cost;
        for (int j = 1; j <= n; j++) {
            for (int i = 1; i <= m; i++) {
                cost = (s.charAt(i-1) == t.charAt(j-1)) ? 0 : 1;   // Subtract one to start at strings' index zero instead of index one
                d[i][j] = Math.min(d[i][j-1] + 1,                  // insertion
                                   Math.min(d[i-1][j] + 1,         // deletion
                                            d[i-1][j-1] + cost));  // substitution

                if(i > 1 && j > 1 && s.charAt(i-1) == t.charAt(j-2) && s.charAt(i-2) == t.charAt(j-1)) {
                    d[i][j] = Math.min(d[i][j], d[i-2][j-2] + cost); // transposition
                }
            }
        }

        // Return the strings' distance
        return d[m][n];
    }
}
