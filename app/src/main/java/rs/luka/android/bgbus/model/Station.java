package rs.luka.android.bgbus.model;

import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import rs.luka.android.bgbus.BuildConfig;
import rs.luka.android.bgbus.io.BaseIO;
import rs.luka.android.bgbus.logic.AlgorithmParameters;
import rs.luka.android.bgbus.logic.Base;
import rs.luka.android.bgbus.logic.Pathfinder;
import rs.luka.android.bgbus.misc.Config;
import rs.luka.android.bgbus.misc.Utils;

/**
 * Predstavlja jednu stanicu i metode za odredjivanje najbolje sledece. Zajedno sa Paths/PathQueue odredjuje najbolju
 * rutu. Sprecava ponavljanje istog puta (tj. ne dozvoljava da se ide od ove do druge stanice koristeci istu liniju vise
 * puta) koriscenjem lista i iteratora za svaku liniju.
 * Videti Link za terminologiju.
 * Created by luka on 14.9.15.
 * @see Link
 */
public class Station {

    public static final  int     MAX_WALKING_DISTANCE = 500;
    private static final Pattern SPECIAL_CHARS        = Pattern.compile("\"|/");

    private final int        id;
    private final String     name;
    private       String     asciiName;
    private       LatLng     location;
    private final List<Link> links; //lista linkova in no particular order
    /**@deprecated */
    private boolean areLinksSorted = false;
    private boolean bearingsSet    = false;
    private int     currentTime    = -1;

    /**
     * Cuva linkove sortirane po tezini za svaku liniju
     */
    private final Map<Line, List<Link>> sortedLinks = new HashMap<>(); //nisam ovo bas precizno osmislio na pocetku
    /**
     * Cuva iteratore (indekse) za svaku liniju koji oznacavaju vec isprobane puteve
     */
    private final Map<Line, Integer>    iterators   = new HashMap<>(); //oh well, trebalo bi da funkcionise i ovako
    //looking back, i nije tako lose

    /**
     * Linkove i lokaciju je potrebno naknadno podesiti (nakon ucitavanja linija)
     * @param id identifikacioni broj stanice
     * @param name user-friendly ime stanice
     */
    public Station(int id, String name) {
        this.id = id;
        this.name = name;
        links = new ArrayList<>();
    }

    public Station(String machineString) {
        StringTokenizer mainTokenizer = new StringTokenizer(machineString, BaseIO.FIELD_SEPARATOR);
        id = Integer.parseInt(mainTokenizer.nextToken());
        name = mainTokenizer.nextToken();
        asciiName = SPECIAL_CHARS.matcher(mainTokenizer.nextToken()).replaceAll("");
        location = new LatLng(Double.parseDouble(mainTokenizer.nextToken()),
                              Double.parseDouble(mainTokenizer.nextToken()));
        if (mainTokenizer.hasMoreTokens()) {
            StringTokenizer arrayTokenizer = new StringTokenizer(mainTokenizer.nextToken(), BaseIO.ARRAY_SEPARATOR);
            links = new ArrayList<>();
            while(arrayTokenizer.hasMoreTokens())
                links.add(new Link(arrayTokenizer.nextToken()));
        } else links = new ArrayList<>(0);

        /*String[] fields = machineString.split(BaseIO.FIELD_SEPARATOR);
        id = Integer.parseInt(fields[0]);
        name = fields[1];
        asciiName = fields[2];
        location = new LatLng(Double.parseDouble(fields[3]), Double.parseDouble(fields[4]));
        if(fields.length > 5) {
            String[] linkStrs = fields[5].split(BaseIO.ARRAY_SEPARATOR);
            links = new ArrayList<>(linkStrs.length);
            for (String linkStr : linkStrs) links.add(new Link(linkStr));
        } else links = new ArrayList<>(0);*/
    }

    public Station(int id, String name, LatLng location, List<Station> links) {
        this.id=id;
        this.name = name;
        this.location = location;
        this.links = new ArrayList<>(links.size());
        for(Station st: links) {
            this.links.add(new Link(st, Line.getWalking()));
            st.addWalkingLinkTo(this);
        }
    }

    public void addWalkingLinkTo(Station st) {
        links.add(new Link(st, Line.getWalking()));
    }

    public Station(int id, String name, String asciiName, double lat, double lng, String links) {
        this.id = id;
        this.name = name;
        this.asciiName = asciiName;
        this.location = new LatLng(lat, lng);
        if(links.isEmpty())
            this.links = new ArrayList<>(0);
        else {
            String[] linkStrs = links.split(BaseIO.ARRAY_SEPARATOR);
            this.links = new ArrayList<>(linkStrs.length);
            for (String linkStr : linkStrs) this.links.add(new Link(linkStr));
        }
    }

    /**
     * Koristi se pri deserijalizaciji
     */
    public void loadLinks() {
        for(Link l : links) {
            l.loadDataFromString();
        }
    }

    /**
     * Pravi linkove za setnju izmedju stanica, ako su na odgovarajucoj udaljenosti i setanje je ukljuceno u Configu
     * @param stationSet kolekcija stanica
     */
    public void registerStations(Collection<Station> stationSet) { //valjda je originalno bio Set, pa je ostalo ime, nebitno
        for(Station st : stationSet) {
            if(Config.WALKING_ENABLED && !this.equals(st) && location.distanceBetween(st.getLocation()) < MAX_WALKING_DISTANCE) {
                links.add(new Link(st, Line.getWalking()));
            }
        }
    }

    /**
     * Postavlja razliku smera linkova i cilja i odredjuje im tezinu (BEARING_WEIGHT)
     */
    private void initStationForGoal(int time) {
        double routeBearing = location.bearingTo(Base.getInstance().getGoal().getLocation());
        for (Link l : links) {
            double direction;
            if (l.usingLine == null || l.usingLine.isInitial() || l.usingLine.isWalking())
                direction = getLocation().bearingTo(l.toStation.getLocation());
            else
                direction = l.usingLine.getDirectionAt(this);
            l.setBearingDiff(direction - routeBearing);
        }
        this.currentTime = time; //used by Link#calculateWeightFor(Line), Link#getCostFor(Line)
        bearingsSet = true;
    }

    /**
     * Sortira linkove (mapa sortedLinks) u odnosu na tezinu koristeci datu liniju
     * @param line dataold linija
     */
    private void setEstimates(Line line, final int time) {
        if (!bearingsSet) //smer se ne menja u zavisnosti od linije
            initStationForGoal(time);
        final Line nonNullLine = line == null ?
                                 Line.getInitial() :
                                 line; //pisano pre nego što sam se setio da uvedem initial i walking kao specijalne slučajeve
        // nisam siguran gde sam sve koristio ovo, a nemam vremena za refactoring

        List<Link> sortedLinks = new ArrayList<>(this.links);
        Collections.sort(sortedLinks, new Comparator<Link>() {
            @Override
            public int compare(Link o1, Link o2) {
                double weight1 = o1.calculateWeightFor(nonNullLine);
                double weight2 = o2.calculateWeightFor(nonNullLine);
                if (weight1 < weight2)
                    return -1;
                if (weight1 > weight2)
                    return 1;
                return 0;
            }
        });
        for(int i=sortedLinks.size()-1; i>=0; i--) {
            if(!sortedLinks.get(i).getUsingLine().isValid(Pathfinder.getCurrentContext())) {
                sortedLinks.remove(i);
            }
        }

        //ako postoje dva puta do jedne stanice, moze da se desi da walking bude pre nekog prevoza zbog nacina racunanja smera
        //nisam siguran da li je ovo i dalje opravdano, posto je ovaj deo pisan pre PathQueue i veceg dela Paths
        /*for (int i = 0;
             i < links.size();
             i++) { //n^2, ali n je u vecini slucajeva <20, pa mislim da nece previse uticati
            for (int j = 0; j < i; j++)
                if (links.get(i).toStation.equals(links.get(j).toStation) && links.get(j).usingLine.isWalking())
                    Collections.swap(links, i, j); //correction, linije racunaju smer nekoliko stanica unapred
        }*/
        iterators.put(line, 0);
        this.sortedLinks.put(line, sortedLinks);
    }

    public void invalidateBearings() {
        bearingsSet = false;
        currentTime = -1;
    }

    /**
     * @return true ako je linija poznata stanici i njen iterator je manji od broja linkova
     */
    public boolean hasNext(Line line) {
        return !iterators.containsKey(line) || iterators.get(line) < sortedLinks.get(line).size();
    }

    /**
     * Vraca najmanju tezinu do sledece stanice ili {@link Double#POSITIVE_INFINITY} ako takva ne postoji, ne
     * povecavajuci iterator
     */
    public double peekNextWeight(Line line, int time) {
        if (!iterators.containsKey(line)) setEstimates(line, time);

        int iterator = iterators.get(line);
        if (iterator < sortedLinks.get(line).size()) {
            Link next = sortedLinks.get(line).get(iterator);
            return next.calculateWeightFor(line);
        } else {
            return Double.POSITIVE_INFINITY;
        }
    }

    private int lastAjdustmentWalks = 0;
    private void adjustEsimatesForWalking(final int consecWalks, List<Link> sortedLinks, final Line l) { //todo figure out what to do with iterators
        if(consecWalks == lastAjdustmentWalks) return;
        Collections.sort(sortedLinks, new Comparator<Link>() {
            @Override
            public int compare(Link lhs, Link rhs) {
                double lhsWeight = lhs.calculateWeightFor(l), rhsWeight = rhs.calculateWeightFor(l);
                if(lhsWeight*AlgorithmParameters.getConsecutiveWalkCoefficient(consecWalks) <
                   rhsWeight*AlgorithmParameters.getConsecutiveWalkCoefficient(consecWalks)) return -1;
                else if(lhsWeight*AlgorithmParameters.getConsecutiveWalkCoefficient(consecWalks) >
                        rhsWeight*AlgorithmParameters.getConsecutiveWalkCoefficient(consecWalks)) return 1;
                else return 0;
            }
        });
        lastAjdustmentWalks = consecWalks;
    }

    /**
     * Vraca sledeci link s najmanjom tezinom za datu liniju (koristi se zbog presedanja i koeficijenta za tip prevoza)
     * @param line dataold linija
     */
    public Link getNextBestGuess(Line line, int time, int consecWalks) {
        if (!iterators.containsKey(line) || !bearingsSet) setEstimates(line, time);

        List<Link> apprLinks = sortedLinks.get(line);
        int iterator = iterators.get(line);
        if (iterator < apprLinks.size()) {
            Link theChosenOne = apprLinks.get(iterator);
            iterators.put(line, iterator + 1);
            return theChosenOne;
        } else {
            return null;
        }
    }

    public void reset() {
        for(Map.Entry<Line, Integer> e : iterators.entrySet()) {
            e.setValue(0);
        }
    }

    /**
     * Vraca listu linkova, pocinjuci od onoga sa najmanjom tezinom, cije tezine su dovoljno bliske da budu dobri putevi.
     * "Dovoljno bliske" je definisano sa {@link AlgorithmParameters#DELTA_WEIGHT}
     * @see AlgorithmParameters#DELTA_WEIGHT
     */
    public List<Link> getNextBestGuesses(Line line, int time, int consecWalks) {
        List<Link> guesses = new LinkedList<>();
        if (line.isInitial()) { //ako je pocetna linija, procene nemaju toliko smisla, tako da vracam sve moguce
            Link l; //ima veze s nacinom na koji Paths radi (root nema efikasnost), pa ovo mozda nije optimalna metoda
            while((l=getNextBestGuess(line, time, consecWalks))!=null) //za slucaj da opet promenim stablo (ili zamenim s nekom drugom strukturom)
                guesses.add(l); //racunam da necu imati vremena da menjam ponovo, tako da ovo ostaje samo mali coupling issue (u teoriji)
            return guesses;
        }
        double nextWeight = peekNextWeight(line, time);
        if(Double.isInfinite(nextWeight)) return guesses;
        guesses.add(getNextBestGuess(line, time, consecWalks));
        Link next;
        while(peekNextWeight(line, time) - AlgorithmParameters.getDeltaWeight() < nextWeight && (next=getNextBestGuess(line, time, consecWalks))!=null) {
            guesses.add(next);
        }
        return guesses;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Station && id == ((Station)obj).id)
                || obj instanceof Integer && obj.equals(id); //prihvatam i Integer, zbog mape
    }

    @Override
    public int hashCode() {
        return id;
    }

    public LatLng getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }
    public String getAsciiName() {return asciiName==null ? name.toLowerCase() : asciiName;}

    public String getLineList() {
        if(links.isEmpty()) return "";
        StringBuilder buffer = new StringBuilder();
        for(Link l : links) {
            if(l == null || l.getUsingLine() == null) {
                //Log.e("Station", "Null at wrong place");
            } else if(!l.getUsingLine().isInitial() && !l.getUsingLine().isWalking()) {
                buffer.append(l.getUsingLine()).append(", ");
            }
        }
        if(buffer.length() > 1)
            buffer.deleteCharAt(buffer.length() - 2);
        return buffer.toString();
    }

    /**
     * @return ime stanice
     */
    @Override
    public String toString() {
        return name;
    }

    public String toExtraString() {
        return id + "#" + location;
    }

    /*
     * Note to self: Line-agnostic, station-specific
     */
    /**
     * Predstavlja vezu izmedju dve stanice. Odredjen je stanicom i linijom.
     * Terminologija: cost ili trosak grubo odgovara utrosenom vremenu, uzima u obzir udaljenost, tip prevoza i presedanja
     *                weight ili tezina odredjuje optimalnu liniju 'na duze staze', uzima u obzir smer kretanja i
     *                obliznje 'skupe' stanice
     */
    public class Link {

        //oslanjaju na smer u odnosu nekoliko stanica unapred

        private Station toStation;
        private Line    usingLine;
        private double  initialWeight;
        private double  cost;
        private double  bearingDiff;
        private double  bearingWeight;
        private String  machineStringForProcessing;
                //Stanice se ucitavaju pre linije, pa linije ne postoje u trenutku
        //pozivanja konstruktora za Link. Ako se ucitava iz fajla, podaci za Linkove se moraju ucitati na kraju
        //(tj. kada Linije vec postoje u Bazi)

        private Link(String machineString) {
            machineStringForProcessing = machineString;
        }

        /**
         * Pozvati tek nakon sto se linije ucitaju
         */
        private void loadDataFromString() {
            StringTokenizer tokenizer = new StringTokenizer(machineStringForProcessing, BaseIO.MINOR_SEPARATOR);
            toStation = Base.getInstance().getStation(Integer.parseInt(tokenizer.nextToken()));
            usingLine = Base.getInstance().getLine(tokenizer.nextToken());
            initialWeight = 0;
            tokenizer.nextToken();
            cost = Double.parseDouble(tokenizer.nextToken());
            machineStringForProcessing = null;
        }

        private Link(Station destination, Line usingLine) {
            this.toStation = destination;
            if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 19)
                Objects.requireNonNull(usingLine,
                                       "Using line is null for station " + Station.this + ", dest " + destination);
            this.usingLine = usingLine;
            cost = Station.this.getLocation().costTo(toStation.getLocation(), usingLine);
            if (!usingLine.isWalking())
                initialWeight = (AlgorithmParameters.useCostForWeight() ? cost : 0)
                                + usingLine.getNumberOfNearbyExpensiveStations(Station.this)
                                  * AlgorithmParameters.getNearbyExpensiveSpotsWeight();
            else if (toStation.getLocation().isNearBusySpot())
                initialWeight = (AlgorithmParameters.useCostForWeight() ? cost : 0)
                                + AlgorithmParameters.getWalkExpensiveSpotsApprox()
                                  * AlgorithmParameters.getNearbyExpensiveSpotsWeight();
            else
                initialWeight = (AlgorithmParameters.useCostForWeight() ? cost : 0);
        }

        public Station getToStation() {
            return toStation;
        }

        public Line getUsingLine() {
            return usingLine;
        }

        /**
         * Svodi ugao na vrednost izmedju 0 i 180 stepeni i postavlja odgovarajuce polje
         * @param bearingDiff ugao izmedju pravca linije koja povezuje pocetak i cilj i pravca ove transportne linije
         */
        private void setBearingDiff(double bearingDiff) {
            bearingDiff = Math.abs(bearingDiff);
            if (bearingDiff > 180)
                bearingDiff = 360 - bearingDiff;
            this.bearingDiff = bearingDiff;
            this.bearingWeight = AlgorithmParameters.getBearingWeight(bearingDiff);
        }

        /**
         * Utility, vraca 'tezinu' za dati ugao
         */
        /*private double selectBearingWeight() {
            double distanceToGoal = toStation.getLocation()
                                             .distanceBetween(Base.getInstance().getGoal().getLocation());
            for (int i = 0; i < AlgorithmParameters.getBearingLevels().length; i++)
                if (bearingDiff < AlgorithmParameters.getBearingLevels()[i])
                    return AlgorithmParameters.getBearingWeights()[i] * (distanceToGoal / 1000);
            return AlgorithmParameters.getBearingWeights()[AlgorithmParameters.getBearingWeights().length - 1] * (
                    distanceToGoal / 1000);
        }*/

        private double calculateWeightFor(Line l) {
            return initialWeight + bearingWeight * (l.isWalking() ?
                                                    AlgorithmParameters.getWalkBearingCoefficient() : 1)
                   + getStationCost(l) * AlgorithmParameters.getWaitingTimeCoefficient();
        }

        /**
         * Vraca koliko stanica 'kosta', tj presedanje ili default vrednost (STOP_COST), pomnozen koeficijentom tipa
         * @param l linija kojom se dolazi do ove stanice
         * @see AlgorithmParameters#DEFAULT_SWITCH_COST
         * @see AlgorithmParameters#STOP_COST
         */
        private double getStationCost(Line l) {
            double stationCost;
            if (usingLine.equals(l) && !l.isWalking()) { //ako nastavlja istom
                stationCost = AlgorithmParameters.getStopCost();
            } else { //ako preseda (ili seta)
                stationCost = usingLine.getSwitchCost(l, currentTime>1440?currentTime-1440:currentTime,
                                                      Pathfinder.getCurrentDay()+(currentTime>1440?1:0),
                                                      Station.this, toStation);
            }
            if (stationCost < 0) { Log.e("bgbus.Station" , "cost < 0: " + stationCost); }
            return stationCost;
        }

        /**
         * Vraca trosak (cenu) od trenutne stanice (spoljne klase) do stanice do koje vodi ova veza
         * @param l linija kojom se dolazi do trenutne stanice
         * @return cena ove veze koristeci datu liniju
         */
        public double calculateCostFor(Line l) {
            return cost + getStationCost(l) * AlgorithmParameters.getWaitingTimeCoefficient();
            // TODO: 19.11.15. naci nacin da coefficient ne utice na procenu vremena
        }

        public double getTimeFor(Line l) {
        return (cost + getStationCost(l))/AlgorithmParameters.getCostToMinRatio();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Link) {
                Link other = (Link) obj;
                return toStation.equals(other.toStation) && Utils.equals(usingLine, other.usingLine);
            } else return false;
        }

        @Override
        public int hashCode() {
            return Utils.hash(toStation, usingLine);
        }

        private String toMachineString() {
            return toStation.getId() + BaseIO.MINOR_SEPARATOR + usingLine.getId() + BaseIO.MINOR_SEPARATOR
                   + initialWeight + BaseIO.MINOR_SEPARATOR + cost;
        }
    }
}
