package rs.luka.android.bgbus.logic;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import rs.luka.android.bgbus.model.FullPath;
import rs.luka.android.bgbus.model.Line;
import rs.luka.android.bgbus.model.Station;

/**
 * v4
 * Jednostavniji, ne nuzno bolji. Nastao kao 'stabilnija' alternativa (nedovršenom) Paths-u. Dobijeni rezultati su slični
 * {@link rs.luka.belgradeunderground.Main.PathConfigs#ALWAYS_TEST_EFFICIENCY} parametrima za Paths, što nije uvek
 * optimalno. Generalno, manje se oslanja na procene iz {@link Station#getNextBestGuesses(Line, int, int)} nego druga klasa.
 * Dodaje node-ove u Queue u 'talasima' koje dobija iz {@link Station#getNextBestGuesses(Line, int, int)} i uvek vraća Node
 * koji se nalazi na vrhu, tj. onaj s najvećom efikasnošću i tako dok ne dođe do cilja. Jedini način da se neki Node
 * 'skloni' s vrha je da ga neki drugi 'prestigne' u efikasnosti ili da Node ne može da ima više dece (kad {@link
 * Station#getNextBestGuesses(Line, int)} počne da vraća prazne liste), kada se na vrh postavlja drugi po efikasnosti.
 * 'Queue' potiče iz naziva klase koju proširuje; inače je heap kao struktura podataka (kao i PriorityQueue. Ko uopšte
 * bira ovakva imena?)
 * Created by luka on 8.10.15.
 */
public class PathQueue extends PriorityQueue<PathQueue.Node> implements Routing {

    private static final int INITIAL_SIZE = 769; //todo benchmark

    /**
     * Vraca Queue efikasnosti za datu stanicu
     * @param start pocetna stanica, koren
     */
    public PathQueue(Station start) {
        super(INITIAL_SIZE);
        Node initial = new Node(null, start, Line.getInitial(), 0.01, 0); //0.01 jer izbegavam deljenje sa nulom
        this.addAll(initial.getNext()); //svejedno da li se dodaje initial ili initial#getNext(), posto initial ionako
        //pada na poslednje mesto sa efficiency==0
    }

    /**
     *
     * @return best route
     */
    public Node findRoute() {
        Node current;
        do {
            Set<Node> children;
            while ((children = peek().getNext()).size() == 0) poll();
            addAll(children);
            current = peek();
        } while (!current.isGoal());
        return current;
    }

    /**
     * Ekvivalent Paths-u ili PathTree-u
     */
    public static class Node implements Comparable<Node>, Routing.Path {
        private final Station currentStation;
        private final Line currentLine;
        private final double efficiency;
        private final double cost;
        private final double time;
        private final Node parent;

        private Node(Node parent, Station station, Line line, double cost, double time) {
            this.parent = parent;
            this.currentStation = station;
            this.currentLine = line;
            this.cost = cost;
            this.efficiency = Base.getInstance().getStart().getLocation().distanceBetween(currentStation.getLocation()) / cost;
            this.time = time;
        }

        private Set<Node> getNext() {
            List<Station.Link> guesses = currentStation.getNextBestGuesses(currentLine,
                                                                           Pathfinder.getCurrentTime() >= 0 ? (int)(Pathfinder.getCurrentTime()+time) : -1,
                                                                           0);
            Set<Node> next = new HashSet<>();
            for(Station.Link l : guesses) {
                next.add(new Node(this, l.getToStation(), l.getUsingLine(), cost + l.calculateCostFor(currentLine),
                                  time + l.getTimeFor(currentLine)));
            }
            return next;
        }

        public boolean isGoal() {
            return currentStation.equals(Base.getInstance().getGoal());
        }

        public FullPath reconstruct() {
            List<FullPath.LineStationPair> path = new LinkedList<>();
            Node current = this;
            while (current != null) {
                path.add(new FullPath.LineStationPair(current.currentLine, current.currentStation));
                current = current.parent;
            }
            Collections.reverse(path);
            return new FullPath(path, time);
        }

        @Override
        public int compareTo(@NonNull Node o) {
            if (efficiency > o.efficiency)
                return -1;
            else if (efficiency < o.efficiency)
                return 1;
            else return 0;
        }
    }
}
