package rs.luka.android.bgbus.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.JsonWriter;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import rs.luka.android.bgbus.R;
import rs.luka.android.bgbus.logic.Base;
import rs.luka.android.bgbus.logic.Paths;

/**
 * Reporesents line-station pairs and estimated time for the given path.
 */
public class FullPath implements Parcelable {

    private final List<LineStationPair> path;
    private final double                time;

    /**
     * Uses provided list (does NOT copy it)
     * @param path
     * @param time
     */
    public FullPath(List<LineStationPair> path, double time) {
        this.path = path;
        this.time = time;
    }

    protected FullPath(Parcel in) {
        time = in.readDouble();
        path = new LinkedList<>();
        Parcelable[] pathParcelArray = in.readParcelableArray(Paths.class.getClassLoader());
        for (Parcelable pathParcel : pathParcelArray) { //ugly
            path.add((LineStationPair) pathParcel);
        }
    }

    public static final Creator<FullPath> CREATOR = new Creator<FullPath>() {
        @Override
        public FullPath createFromParcel(Parcel in) {
            return new FullPath(in);
        }

        @Override
        public FullPath[] newArray(int size) {
            return new FullPath[size];
        }
    };

    public List<LineStationPair> getPath() {
        return path;
    }

    public double getTime() {
        return time;
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    /**
     * Returns extreme points of the route
     * @return {@link LatLngBounds} representing south- north- east- and west-most points
     */
    @NonNull
    public LatLngBounds getBounds() {
        double north = -200, south = 200, east = -200, west = 200;
        LatLng location; double lat, lng;
        for (int i = 0; i < path.size(); i++) {
            location = path.get(i).station.getLocation();
            lat=location.getLatitudeInDegrees();
            lng = location.getLongitudeInDegrees();
            if (lat > north)
                north = lat;
            if (lat < south)
                south = lat;
            if (lng > east)
                east = lng;
            if (lng < west)
                west = lng;
        }
        Log.d("Paths", "Bounds: N: " + north + ", S: " + south + ", E: " + east + ", W: " + west);
        return new LatLngBounds(new com.google.android.gms.maps.model.LatLng(south, west),
                                new com.google.android.gms.maps.model.LatLng(north, east));
    }

    public JsonWriter toJsonString(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("path").beginArray();
        for(LineStationPair stop : path)
            stop.toJsonString(writer);
        writer.endArray();
        writer.name("cost").value(time);
        writer.endObject();
        return writer;

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(time);
        LineStationPair[] pathArray = new LineStationPair[path.size()];
        for(int i=0; i<path.size(); i++)
            pathArray[i] = path.get(i);
        dest.writeParcelableArray(pathArray, 0);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Ruta:\n");
        int brojStanica = 0;
        buffer.append(path.get(0)).append("\n");
        LineStationPair prev = path.get(1);
        for (int i = 2; i < path.size(); i++) {
            if (path.get(i).line.equals(prev.line)) {
                brojStanica++;
            } else {
                if (prev.line.isInitial() || prev.line.isWalking()) {
                    buffer.append(path.get(i - 1)).append("\n");
                } else {
                    buffer.append(path.get(i - 1))
                          .append(" (broj stanica: ")
                          .append(brojStanica + 1)
                          .append(")")
                          .append("\n");
                }
                prev = path.get(i);
                brojStanica = 0;
            }
        }
        if (prev.line.isInitial() || prev.line.isWalking()) {
            buffer.append(path.get(path.size() - 1)).append("\n");
        } else {
            buffer.append(path.get(path.size() - 1))
                  .append(" (broj stanica: ")
                  .append(brojStanica + 1)
                  .append(")")
                  .append("\n");
        }
        String time = "Procenjeno vreme: " + Math.round(this.time) + "min";
        buffer.append(time);
        return buffer.toString();
    }

    /**
     * Returns new FullPath which only contains points where user should switch line, and with multiple walking
     * links merged together. This path should be used for display to the user.
     * @return new {@link FullPath} representing user-friendly route
     */
    public FullPath reduce() {
        FullPath reduced     = new FullPath(new LinkedList<LineStationPair>(), time);
        int      brojStanica = 0;
        reduced.path.add(path.get(0));
        LineStationPair prev = path.get(1);
        for (int i = 2; i < path.size(); i++) {
            if (path.get(i).line.equals(prev.line)) {
                brojStanica++;
            } else {
                if (prev.line.isInitial() || prev.line.isWalking()) {
                    reduced.path.add(path.get(i - 1));
                } else {
                    reduced.path.add(path.get(i - 1));
                    reduced.path.get(reduced.path.size() - 1).brojStanica = brojStanica + 1;
                }
                prev = path.get(i);
                brojStanica = 0;
            }
        }
        if (prev.line.isInitial() || prev.line.isWalking()) {
            reduced.path.add(path.get(path.size() - 1));
        } else {
            reduced.path.add(path.get(path.size() - 1));
            reduced.path.get(reduced.path.size() - 1).brojStanica = brojStanica + 1;
        }
        return reduced;
    }

    public static class LineStationPair implements Parcelable { //fml
        private final Line    line;
        private final Station station;
        private       int     brojStanica;

        public LineStationPair(Line line, Station station) {
            this.line = line;
            this.station = station;
            this.brojStanica = 1;
        }

        protected LineStationPair(Parcel in) {
            line = Base.getInstance().getLine(in.readString());
            station = Base.getInstance().getStation(in.readInt());
            brojStanica = in.readInt();
        }

        public static final Creator<LineStationPair> CREATOR = new Creator<LineStationPair>() {
            @Override
            public LineStationPair createFromParcel(Parcel in) {
                return new LineStationPair(in);
            }

            @Override
            public LineStationPair[] newArray(int size) {
                return new LineStationPair[size];
            }
        };

        @Override
        public String toString() {
            if (line.isInitial()) {
                return "Početna stanica: " + station;
            }
            if (line.isWalking()) {
                return "Šetajte do stanice " + station;
            }
            return "Vozite se linijom " + line + " do " + station;
        }

        public String toDisplayString(Context c) {
            if (line.isInitial())
                return c.getString(R.string.directions_initial, station);
            if (line.isWalking())
                return c.getString(R.string.directions_walk, station);
            return c.getResources().getQuantityString(R.plurals.directions_normal, brojStanica, line, station, brojStanica);
        }

        public JsonWriter toJsonString(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("line").value(line.getId());
            writer.name("station").value(station.getId());
            writer.endObject();
            return writer;
        }

        public Line getLine() {
            return line;
        }

        public Station getStation() {
            return station;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(line.getId());
            dest.writeInt(station.getId());
            dest.writeInt(brojStanica);
        }
    }
}
