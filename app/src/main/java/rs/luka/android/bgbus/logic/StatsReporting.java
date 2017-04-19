package rs.luka.android.bgbus.logic;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rs.luka.android.bgbus.io.StatsBase;
import rs.luka.android.bgbus.misc.Config;
import rs.luka.android.bgbus.model.FullPath;

/*
 * Ideja je bila da se podaci o rezultatima šalju na server na dalju obradu, kako bih mogao da dobijem
 * preciznije metode za pronalaženje rute (see AlgorithmParameters). Međutim, nedostatak vremena me je
 * sprečio da to i realizujem.
 */

/**
 * Unfinished (!)
 * Created by luka on 8.11.15..
 */
public class StatsReporting {
    private static final String          URL_STATS        = "http://androidapp1-lukaj.rhcloud.com/stats.php";
    private static       ExecutorService reporterExecutor = Executors.newSingleThreadExecutor();

    public static void reportSolutionStats(final List<SolutionStat> stats, final Context c) {
        if(Config.REPORT_STATS) {
            reporterExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Collections.sort(stats);
                    if (isOnline(c))
                        loadAndUploadStats(stats, c);
                    else
                        saveStats(stats, c);
                }
            });
        }
    }

    public static boolean isOnline(Context c) {
        ConnectivityManager cm      = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo         netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private static String serializeStats(List<SolutionStat> stats) throws IOException {
        StringWriter strWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(strWriter);
        writer.beginObject();
        writer.name("userId").value(Config.getHardwareId());
        writer.name("stats").beginArray();
        for(SolutionStat stat : stats)
            stat.toJsonString(writer);
        writer.endArray();
        writer.endObject();
        return strWriter.toString();
    }

    private static void saveStats(List<SolutionStat> stats, Context context) {
        try {
            StatsBase.getInstance(context).insertRecord(serializeStats(stats));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadAndUploadStats(List<SolutionStat> current, Context context) {
        StatsBase base = StatsBase.getInstance(context);
        List<String> prev = base.queryRecords();
        try {
            prev.add(serializeStats(current));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(String stat : prev)
            uploadStats(stat);
        base.clearRecords();
    }

    private static void uploadStats(String stats) {
        try {
            Log.i("bgbus.StatsReporting", "uploading stats");
            Log.d("bgbus.StatsReporting", stats);
            URL url = new URL(URL_STATS);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type","application/json");
            byte[] outputInBytes = stats.getBytes("UTF-8");
            OutputStream os = conn.getOutputStream();
            os.write(outputInBytes);
            os.close();

            conn.connect();
            int resp = conn.getResponseCode();
            Log.i("bgbus.StatsReporting", "response: " + resp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class SolutionStat implements Comparable<SolutionStat> {
        private FullPath               path;
        private Pathfinder.PathConfigs method;
        private long                   time;

        public SolutionStat(FullPath path, Pathfinder.PathConfigs method, long time) {
            this.path = path;
            this.method = method;
            this.time = time;
        }

        @Override
        public int compareTo(@NonNull SolutionStat another) {
            return Pathfinder.pathComparator.compare(path, another.path);
        }

        public FullPath getPath() {
            return path;
        }

        public JsonWriter toJsonString(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("route");
            path.toJsonString(writer);
            writer.name("method");
            method.toJsonString(writer);
            writer.name("time").value(time);
            writer.endObject();
            return writer;
        }
    }
}
