package rs.luka.android.bgbus.io;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Scanner;

import rs.luka.android.bgbus.R;
import rs.luka.android.bgbus.logic.Base;
import rs.luka.android.bgbus.model.Line;
import rs.luka.android.bgbus.model.Station;

/**
 *
 * Created by luka on 17.9.15.
 */
public class BaseIO {
    public static final char TYPE_SEPARATOR   = '$';
    public static final String OBJECT_SEPARATOR = "\n";
    public static final String FIELD_SEPARATOR  = "#"; //jer je GSP bio toliko ljubazan da mi uzme '/'
    public static final String ARRAY_SEPARATOR  = ",";
    public static final String MINOR_SEPARATOR  = ":";

    /**
     * Loads all data from res/raw/data file into the given object
     * @param base where to load data
     * @param c context used for accessing resources
     * @throws IOException
     */
    public static void loadBase(Base base, Context c) {
        //DO NOT CHANGE THE FILENAME
        InputStream in = c.getResources().openRawResource(R.raw.data);
        //I REPEAT, DO NOT CHANGE THE FILENAME
        Scanner scan = new Scanner(in);
        scan.useDelimiter("\\$");
        String[] stationStrs = scan.next().split(OBJECT_SEPARATOR);
        for(String station : stationStrs)
            base.addStation(new Station(station));
        scan.useDelimiter("\\Z");
        String[] lines = scan.next().split(OBJECT_SEPARATOR);
        lines[0] = lines[0].substring(1); //prvi karakter je TYPE_SEPARATOR
        scan.close();
        for(String line : lines)
            base.addLine(new Line(line));
        Iterator<Station> stations = base.getStationIterator();
        while(stations.hasNext()) stations.next().loadLinks();
    }

    /*
    public static void loadBaseLowMemory(Base base, Context c) throws IOException {
        //DO NOT CHANGE THE FILENAME
        InputStream in = c.getResources().openRawResource(R.raw.data);
        //I REPEAT, DO NOT CHANGE THE FILENAME
        Scanner scan = new Scanner(in);
        String nextLine=null;
        while(scan.hasNextLine()) {
            nextLine = scan.nextLine();
            if(nextLine.charAt(0)==TYPE_SEPARATOR) break;
            base.addStation(new Station(nextLine));
        }
        base.addLine(new Line(nextLine.substring(1)));
        while(scan.hasNextLine())
            base.addLine(new Line(scan.nextLine()));
        Iterator<Station> stations = base.getStationIterator();
        while(stations.hasNext()) stations.next().loadLinks();
    }*/ //too slow
}
