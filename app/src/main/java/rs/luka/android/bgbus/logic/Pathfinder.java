package rs.luka.android.bgbus.logic;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import rs.luka.android.bgbus.misc.Utils;
import rs.luka.android.bgbus.model.FullPath;
import rs.luka.android.bgbus.model.Station;

//NE MENJATI OVU KLASU NITI BILO ŠTA U NJOJ
//Da pojasnim, OVO SADA RADI. Bilo kakva izmena može da dovede da to tad neće raditi.
//Ako misliš da je neuredna, neka je, ne gledaj. Da ti nije palo na pamet da menjaš raspored naredbi.
//Ne menjaj način da koji se pozivaju i izvršavaju callbackovi, ne menjaj imena promenljivih,
//NIKADA ne menjaj Executor. Nemoj da se praviš pametan i sređuješ ovu klasu na bilo koji način.
//Ako misliš da postoji neka greška, pregledaj sve ostalo. Svaku drugu liniju koda dobro osmotri,
//pa se vrati na ovo, pogledaj šta piše, i opet prekontroliši sve ostalo. Tek onda se usudi da
//promeniš neki karakter, dobro pazeći gde se on sve upotrebljava. Odvoji minimum par sati.
//
//Sincerely,
//Past self

/**
 * Entering point for interacting with the algorithm. Manages multithreading and all that complex stuff.
 * Created by luka on 12.10.15..
 */
public class Pathfinder {

    public interface FinderCallbacks {
        void addRoute(FullPath path);
    }
    private static class Finder implements Runnable {
        private final PathConfigs cfg;
        Finder(PathConfigs cfg) {
            this.cfg = cfg;
        }

        @Override
        public void run() {
            FullPath sol = cfg.initRouting(Base.getInstance().getStart()).findRoute().reconstruct();
            registerSolution(sol);
        }
    }

    static final Comparator<FullPath> pathComparator = new Comparator<FullPath>() {
        @Override
        public int compare(FullPath lhs, FullPath rhs) {
            if (lhs == null) return 1;
            if (rhs == null) return -1;
            if (lhs.getTime() < rhs.getTime())
                return -1;
            if (lhs.getTime() > rhs.getTime())
                return 1;
            return 0;
        }
    };

    private static int                               configsIterator = 0;
    private static boolean                           isRunning       = false;
    private static boolean                           isOnScreen      = false;
    private static FinderCallbacks                   callbacks       = null;
    private static List<StatsReporting.SolutionStat> solutions       = new LinkedList<>();
    private static int                               currentTime     = -1;
    private static int                               currentDay      = -1;
    private static Context context;

    private static final int NUM_OF_THREADS = 1;
    private static ExecutorService findersExecutor;
    private static List<Future> finders = new LinkedList<>();

    /**
     * Set goal and start the search
     * @param id id of the goal station. null denotes algorithm should rely on Base for start and goal
     * @param context context used for accessing resources. It'll be kidnapped, but I'll do by best
     *                to release it asap
     */
    public static void setGoal(String id, Context context) {
        cleanUp();
        AlgorithmParameters.setWaitingTimeCoefficient(
                Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
                                                  .getString("pref_switch_tendency", "1")));
        Pathfinder.context = context;
        if (id != null) {
            if (id.isEmpty()) return;
            Base.getInstance().setGoal(Integer.parseInt(id));
        }
        isOnScreen = false;
        Base.getInstance().resetStations(); //napraviti Line-threadId pair kao key za iteratore kod stanica
        prevSol = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_use_time", true))
            currentTime = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        else
            currentTime = 720; //podne, todo fix bug with incorrect handling off time handling (i.e. time==-1 issues)
        currentDay = cal.get(Calendar.DAY_OF_WEEK);
        Log.i("Pathfinder",
              "Searching for route from " + Base.getInstance().getStart() + " to " + Base.getInstance().getGoal());
        if (isRunning) {
            findersExecutor.shutdownNow();
            for (Future f : finders) f.cancel(true);
        }
        findersExecutor = Executors.newFixedThreadPool(NUM_OF_THREADS);
        finders.add(findersExecutor.submit(new Finder(AlgorithmParameters.PATH_CONFIGS[0])));
        isRunning = true;
    }

    private static long prevSol = 0;

    private static void registerSolution(FullPath solution) {
        long currSol = System.currentTimeMillis();
        Log.d("PFinder", "Found solution " + configsIterator + ". Estimated: " + solution.getTime() +
                         " Time: " + (currSol - prevSol));
        solutions.add(new StatsReporting.SolutionStat(solution, AlgorithmParameters.PATH_CONFIGS[configsIterator],
                                                      currSol-prevSol));
        configsIterator++;
        if (configsIterator < AlgorithmParameters.PATH_CONFIGS.length
            && AlgorithmParameters.PATH_CONFIGS[configsIterator] != null) { //zaustavlja se kod prvog nulla (pretpostavlja da su i svi posle nullovi)
            Base.getInstance().resetStations();
            finders.add(findersExecutor.submit(new Finder(AlgorithmParameters.PATH_CONFIGS[configsIterator])));
        } else {
            StatsReporting.reportSolutionStats(new ArrayList<>(solutions), context);
            if(isOnScreen) {cleanUp(); isOnScreen=true;
                Base.getInstance().clearPoints();} //clears solutions list
            isRunning = false;
        }
        prevSol = System.currentTimeMillis();
        if(isOnScreen)
            callbacks.addRoute(solution);
    }

    public static int getCurrentTime() {
        return currentTime;
    }
    public static int getCurrentDay() {return currentDay;}

    public static List<FullPath> getResults(FinderCallbacks callbacks) {
        Pathfinder.callbacks = callbacks;
        isOnScreen = true;
        List<FullPath> copy = new ArrayList<>(solutions.size());
        Utils.copySolutions(solutions, copy); //index out of bounds for LinkedList: only in Java
        Collections.sort(copy, pathComparator);
        for(int i=1; i<copy.size(); i++)
            if(copy.get(i) == null || Utils.timesEqual(copy.get(i).getTime(), copy.get(i - 1).getTime())) {
                copy.remove(i);
                i--;
            }
        if(!isRunning) {cleanUp(); isOnScreen=true;}
        return copy;
    }

    public static void abort() {
        if(findersExecutor != null)
            findersExecutor.shutdownNow();
        if(!solutions.isEmpty())
            StatsReporting.reportSolutionStats(new ArrayList<>(solutions), context);
        cleanUp(); //clears solutions
        currentTime=-2;
        Base.getInstance().clearPoints();
    }

    private static void cleanUp() {
        configsIterator = 0;
        isRunning = false;
        isOnScreen = false;
        callbacks = null;
        context=null;
        solutions.clear();
        finders.clear();
    }

    @Nullable
    public static Context getCurrentContext() {
        return context;
    }

    protected static class PathConfigs {
        static final PathConfigs PATHQUEUE = new PathConfigs(-1, -1, -1);
        static final PathConfigs NO_REDUCTIONS = new PathConfigs(0.5,2,1);
        static final PathConfigs NORMAL = new PathConfigs(0.5,2,1.2);
        static final PathConfigs STRICT = new PathConfigs(0.8,2,1.1);
        static final PathConfigs LENIENT = new PathConfigs(0.35,2,1.2);
        static final PathConfigs TOLERATE_FAILS = new PathConfigs(0.6,4,1.1);
        static final PathConfigs MAX_START_EFFICIENCY = new PathConfigs(1,3,1.2);
        static final PathConfigs ALWAYS_TEST_EFFICIENCY = new PathConfigs(0.5,1,1.1);

        final double minEfficiency, reduceFactor;
        final int maxFails;

        PathConfigs(double minEfficiency, int maxFails, double reduceFactor) {
            this.minEfficiency = minEfficiency;
            this.maxFails = maxFails;
            this.reduceFactor = reduceFactor;
        }

        public Routing initRouting(Station start) {
            if (this.equals(PATHQUEUE))
                return new PathQueue(start);
            return Paths.init(start, minEfficiency, maxFails, reduceFactor);
        }

        @Override
        public String toString() {
            return minEfficiency+","+maxFails+","+reduceFactor;
        }

        public JsonWriter toJsonString(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("minEfficiency").value(minEfficiency);
            writer.name("maxFails").value(maxFails);
            writer.name("reduceFactor").value(reduceFactor);
            writer.endObject();
            return writer;
        }
    }
}
