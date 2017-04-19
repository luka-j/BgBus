package rs.luka.android.bgbus.logic;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
 * Plan je bio da šaljem rezultate na server (see StatsReporting, StatsBase), koji posle nekog vremena određuje
 * šta se ponaša najbolje u određenim uslovima i vraća te podatke aplikaciji. Međutim, kako se rok približavao,
 * shvatio sam da neću stići da ispišem ceo kod za server i da od toga neće biti ništa.
 */

/**
 * Groups all parameters used all across the app. Provides only getters.
 * Created by luka on 7.11.15..
 */
public class AlgorithmParameters {
    private static final double[] WAITING_COEFFICIENTS = {0.7, 1, 3, 8};

    private static int     COST_TO_MIN_RATIO             = 700; //pretty random
    private static boolean USE_COST_FOR_WEIGHT           = false;
    private static int     DELTA_WEIGHT                  = USE_COST_FOR_WEIGHT ? 1300 : 800;
    private static int     NEARBY_EXPENSIVE_SPOTS_WEIGHT = 500;
    private static double  WALK_EXPENSIVE_SPOTS_APPROX   = 1.5;
    private static int     DEFAULT_SWITCH_COST           = 1350;
    private static int     STOP_COST                     = 100;
    private static double  WALK_BEARING_COEFFICIENT      = 1.5;
    //smer setanja do stanice je uvek tacan, dok se linije oslanjaju na unapred izracunate smerove
    private static int[]   BEARING_LEVELS                = new int[]{10, 15,   20,   25,   30,   40,   50,    60,    80,    100,   120};
    private static int[]   BEARING_WEIGHTS               = new int[]{0,  800,  1500, 2000, 3000, 6000, 10000, 15000, 25000, 40000, 60000, 100000};
    private static double  COST_INITIAL                  = 0.5;
    private static double  COST_TRAM                     = 1;
    private static double  COST_TROLLEY                  = 1.2;
    private static double  COST_BUS                      = 1.2;
    private static double  COST_WALK                     = 4;
    private static int     EXPENSIVE_STATIONS_LOOKAHEAD  = 6;
    private static int     DEFAULT_WAITING_TIME          = 150;
    private static int     WALK_INTERVAL                 = 8; //balancing purposes (incl. weight) only, needs thorough testing
    private static int     MAX_INITIAL_WALKING_DISTANCE  = 320; //when using custom location
    private static double  CONSECUTIVE_WALK_COEFFICIENT  = 0.2;
    private static double  WAITING_TIME_COEFFICIENT      = WAITING_COEFFICIENTS[1];


    static Pathfinder.PathConfigs[] PATH_CONFIGS = new Pathfinder.PathConfigs[]{
            Pathfinder.PathConfigs.NORMAL,
            Pathfinder.PathConfigs.PATHQUEUE,
            Pathfinder.PathConfigs.MAX_START_EFFICIENCY,
            Pathfinder.PathConfigs.STRICT,
            Pathfinder.PathConfigs.NO_REDUCTIONS,
            Pathfinder.PathConfigs.TOLERATE_FAILS,
            Pathfinder.PathConfigs.ALWAYS_TEST_EFFICIENCY,
            Pathfinder.PathConfigs.LENIENT};


    private static final String KEY_COST_TO_MIN_RATIO             = "costToMin";
    private static final String KEY_USE_COST_FOR_WEIGHT           = "useCostForWeight";
    private static final String KEY_DELTA_WEIGHT                  = "dWeight";
    private static final String KEY_NEARBY_EXPENSIVE_SPOTS_WEIGHT = "nearbyExpSpotsWeight";
    private static final String KEY_WALK_EXPENSIVE_SPOTS_APPROX   = "walkNearbyExpSpots";
    private static final String KEY_DEFAULT_SWITCH_COST           = "switchCost";
    private static final String KEY_STOP_COST                     = "stopCost";
    private static final String KEY_WALK_BEARING_COEFFICIENT      = "walkBearingCoefficient";
    private static final String KEY_BEARING_LEVELS                = "bearingLevels";
    private static final String KEY_BEARING_WEIGHTS               = "bearingWeights";
    private static final String KEY_COST_TRAM                     = "costTram";
    private static final String KEY_COST_TROLLEY                  = "costTrolley";
    private static final String KEY_COST_BUS                      = "costBus";
    private static final String KEY_COST_WALK                     = "costWalk";
    private static final String KEY_COST_INITIAL                  = "costInitial";
    private static final String KEY_EXPENSIVE_STATIONS_LOOKAHEAD  = "expStationsLookahead";
    private static final String KEY_CONFIGS                       = "algoParams";
    private static final String KEY_CONFIG_MIN_EFFICIENCY         = "minEfficiency";
    private static final String KEY_CONFIG_MAX_FAILS              = "maxFails";
    private static final String KEY_CONFIG_REDUCE_FACTOR          = "reduceFor";
    private static final String KEY_DEFAULT_WAITING_TIME          = "defaultInterval";
    private static final String KEY_WALK_INTERVAL                 = "walkInterval";
    private static final String KEY_MAX_INITIAL_WALKING_DISTANCE  = "locationMaxWalk";

    public static double getCostTram() {
        return COST_TRAM;
    }

    public static double getCostTrolley() {
        return COST_TROLLEY;
    }

    public static double getCostBus() {
        return COST_BUS;
    }

    public static double getCostWalk() {
        return COST_WALK;
    }

    public static double getConsecutiveWalkCoefficient(int consecWalks) {
        return 1+consecWalks*CONSECUTIVE_WALK_COEFFICIENT;
    }

    public static boolean useCostForWeight() {
        return USE_COST_FOR_WEIGHT;
    }

    public static double getDeltaWeight() {
        return DELTA_WEIGHT;
    }

    public static int getNearbyExpensiveSpotsWeight() {
        return NEARBY_EXPENSIVE_SPOTS_WEIGHT;
    }

    public static double getWalkExpensiveSpotsApprox() {
        return WALK_EXPENSIVE_SPOTS_APPROX;
    }

    public static int getDefaultSwitchCost() {
        return DEFAULT_SWITCH_COST;
    }

    public static int getStopCost() {
        return STOP_COST;
    }

    public static double getWalkBearingCoefficient() {
        return WALK_BEARING_COEFFICIENT;
    }

    public static int getBearingWeight(double bearing) {
        if(bearing<10) return 0;
        return (int)Math.pow(bearing, 2.37 + (bearing/1000));
    }

    public static int getCostToMinRatio() {
        return COST_TO_MIN_RATIO;
    }

    public static double getCostInitial() {
        return COST_INITIAL;
    }

    public static int getExpensiveStationsLookahead() {
        return EXPENSIVE_STATIONS_LOOKAHEAD;
    }

    public static int getWalkInterval() {
        return WALK_INTERVAL;
    }

    public static int getDefaultWaitingTime() {
        return DEFAULT_WAITING_TIME;
    }

    public static int getMaxInitialWalkingDistance() {
        return MAX_INITIAL_WALKING_DISTANCE;
    }

    public static double getWaitingTimeCoefficient() {
        return WAITING_TIME_COEFFICIENT;
    }

    public static void setWaitingTimeCoefficient(int mode) {
        WAITING_TIME_COEFFICIENT = WAITING_COEFFICIENTS[mode];
        Log.d("bgbus.AlgoParams", "Waiting coefficient: " + WAITING_TIME_COEFFICIENT);
    }

    /**
     * Parses the given json string and extracts all present valid parameters.
     * @param jsonString string in json format, describing parameters using appropriate KEYs
     */
    protected static void setParams(String jsonString) {
        try {
            JSONObject root = new JSONObject(jsonString);
            COST_TO_MIN_RATIO = root.optInt(KEY_COST_TO_MIN_RATIO, COST_TO_MIN_RATIO);
            USE_COST_FOR_WEIGHT = root.optBoolean(KEY_USE_COST_FOR_WEIGHT, USE_COST_FOR_WEIGHT);
            DELTA_WEIGHT = root.optInt(KEY_DELTA_WEIGHT, DELTA_WEIGHT);
            NEARBY_EXPENSIVE_SPOTS_WEIGHT = root.optInt(KEY_NEARBY_EXPENSIVE_SPOTS_WEIGHT,
                                                        NEARBY_EXPENSIVE_SPOTS_WEIGHT);
            WALK_EXPENSIVE_SPOTS_APPROX = root.optDouble(KEY_WALK_EXPENSIVE_SPOTS_APPROX,
                                                         WALK_EXPENSIVE_SPOTS_APPROX);
            DEFAULT_SWITCH_COST = root.optInt(KEY_DEFAULT_SWITCH_COST, DEFAULT_SWITCH_COST);
            STOP_COST = root.optInt(KEY_STOP_COST, STOP_COST);
            WALK_BEARING_COEFFICIENT = root.optDouble(KEY_WALK_BEARING_COEFFICIENT, WALK_BEARING_COEFFICIENT);
            BEARING_LEVELS = parseJsonIntArray(root.optJSONArray(KEY_BEARING_LEVELS), BEARING_LEVELS);
            BEARING_WEIGHTS = parseJsonIntArray(root.optJSONArray(KEY_BEARING_WEIGHTS), BEARING_WEIGHTS);
            COST_TRAM = root.optDouble(KEY_COST_TRAM, COST_TRAM);
            COST_TROLLEY = root.optDouble(KEY_COST_TROLLEY, COST_TROLLEY);
            COST_BUS = root.optDouble(KEY_COST_BUS, COST_BUS);
            COST_WALK = root.optDouble(KEY_COST_WALK, COST_WALK);
            COST_INITIAL = root.optDouble(KEY_COST_INITIAL, COST_INITIAL);
            EXPENSIVE_STATIONS_LOOKAHEAD = root.optInt(KEY_EXPENSIVE_STATIONS_LOOKAHEAD,
                                                       EXPENSIVE_STATIONS_LOOKAHEAD);
            PATH_CONFIGS = parseConfigs(root.optJSONArray(KEY_CONFIGS), PATH_CONFIGS);
            DEFAULT_WAITING_TIME = root.optInt(KEY_DEFAULT_WAITING_TIME, DEFAULT_WAITING_TIME);
            WALK_INTERVAL = root.optInt(KEY_WALK_INTERVAL, WALK_INTERVAL);
            MAX_INITIAL_WALKING_DISTANCE = root.optInt(KEY_MAX_INITIAL_WALKING_DISTANCE, MAX_INITIAL_WALKING_DISTANCE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static int[] parseJsonIntArray(JSONArray jsonArray, int[] fallback) {
        if(jsonArray == null) return fallback;
        int[] array = new int[jsonArray.length()];
        for(int i=0; i<array.length; i++)
            array[i] = jsonArray.optInt(i, fallback[i]);
        return array;
    }

    private static Pathfinder.PathConfigs[] parseConfigs(JSONArray jsonArray, Pathfinder.PathConfigs[] fallback) {
        if(jsonArray == null) return fallback;
        Pathfinder.PathConfigs[] configs = new Pathfinder.PathConfigs[jsonArray.length()];
        double minEff, reduceFor; int maxFails;
        for(int i=0; i<configs.length; i++) {
            JSONObject config = jsonArray.optJSONObject(i);
            if(config == null)
                configs[i] = fallback.length < i ? fallback[i] : null;
            else {
                try {
                    minEff = config.getDouble(KEY_CONFIG_MIN_EFFICIENCY);
                    reduceFor = config.getDouble(KEY_CONFIG_REDUCE_FACTOR);
                    maxFails = config.getInt(KEY_CONFIG_MAX_FAILS);
                    configs[i] = new Pathfinder.PathConfigs(minEff, maxFails, reduceFor);
                } catch (JSONException ex) {
                    configs[i] = fallback.length < i ? fallback[i] : null;
                }
            }
        }
        return configs;
    }
}
