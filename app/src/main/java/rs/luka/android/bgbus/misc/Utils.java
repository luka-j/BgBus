package rs.luka.android.bgbus.misc;

import android.os.Parcelable;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import rs.luka.android.bgbus.logic.StatsReporting;
import rs.luka.android.bgbus.model.FullPath;

/**
 * Provides utility methods
 * Created by luka on 13.10.15.
 */
public final class Utils {
    private Utils() {throw new AssertionError("One does not simply instantiate Utils");}

    public static boolean equals(rs.luka.android.bgbus.model.Line a, rs.luka.android.bgbus.model.Line b) {
        return (a == null) ? (b == null) : a.equals(b);
    }
    public static int hash(Object... o) {
        return Arrays.hashCode(o);
    }
    public static void copySolutions(List<StatsReporting.SolutionStat> from, List<FullPath> to) {
        for (StatsReporting.SolutionStat el : from) if(el!=null) to.add(el.getPath());
    }
    public static boolean timesEqual(double cost1, double cost2) {
        if(Math.abs(cost1-cost2) < 0.1) {
            Log.i("Utils", "Costs equal (" + cost1 + " " + cost2 + ")");
            return true;
        }
        return false;
    }
    public static <T> T[] concatArrays(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] castFromParcelableArray(Parcelable[] source, T[] dest) {
        for(int i=0; i<dest.length; i++) {
            dest[i] = (T)source[i];
        }
        return dest;
    }

    public static String concatStrings(String... strs) {
        StringBuilder ret = new StringBuilder();
        for (String str : strs) ret.append(str);
        return ret.toString();
    }
}
