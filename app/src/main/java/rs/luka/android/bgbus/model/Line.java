package rs.luka.android.bgbus.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import rs.luka.android.bgbus.R;
import rs.luka.android.bgbus.io.BaseIO;
import rs.luka.android.bgbus.logic.AlgorithmParameters;
import rs.luka.android.bgbus.logic.Base;
import rs.luka.android.bgbus.misc.Config;

/**
 *
 * Created by luka on 14.9.15.
 * Serialization: id/type/smerA0,smerA1,.../smerB0,smerB1,.../directions0,directions1,...
 */
public class Line {
    public static final boolean SMER_A = true;
    public static final boolean SMER_B = false;

    //specijalni slucajevi, lakse je raditi s njima nego s nullovima:
    private static final Line initial = new Line("initial", "initial", new LinkedList<Integer>(), new LinkedList<Integer>());
    private static final Line walking = new Line("walk", "walk", new LinkedList<Integer>(), new LinkedList<Integer>());
    //how to screw up diamond operator 101

    private static final int TYPE_BUS = 0;
    private static final int TYPE_TRAM = 1;
    private static final int TYPE_TROLLEY = 2;
    private static final int TYPE_WALK = 3;
    private static final int TYPE_INITIAL = -1;

    private static final int FIELD_EMPTY = -1;
    public static final int DIRECTION_LOOKAHEAD = 5;  //ako je Config.USE_PRECISE_DIRECTIONS true
    public static final int DIRECTION_ESTIMATES = 12; //ako je Config.USE_PRECISE_DIRECTIONS false
    private double[] directionsA;
    private int[] arrivalsA;
    private double[] directionsB;
    private int[] arrivalsB;

    private final String id;
    private final int type;
    private int firstA=360, lastA=1380, firstB=360, lastB=1200;
    private int interval, intervalSubota, intervalNedelja;
    private final List<Station> smerA = new ArrayList<>();
    private final List<Station> smerB = new ArrayList<>();

    public Line(String id, int type, int firstA, int lastA, int firstB, int lastB, int interval, int intervalSubota,
                int intervalNedelja, String[] smerA, String[] smerB) {
        this.id = id;
        this.type = type;
        this.firstA = firstA == FIELD_EMPTY ? this.firstA : firstA;
        this.lastA = lastA == FIELD_EMPTY ? this.lastA : lastA;
        this.firstB = firstB == FIELD_EMPTY ? this.firstB : firstB;
        this.lastB = lastB == FIELD_EMPTY ? this.lastB : lastB;
        this.interval = interval < 1 ? FIELD_EMPTY : interval;
        this.intervalSubota = intervalSubota < 1 ? FIELD_EMPTY : intervalSubota;
        this.intervalNedelja = intervalNedelja < 1 ? FIELD_EMPTY : intervalNedelja;
        for(String aId : smerA)
            this.smerA.add(Base.getInstance().getStation(Integer.parseInt(aId)));
        for(String bId : smerB)
            if(bId.trim().isEmpty())
                Log.w("Line", "bId empty, line: " + id);
            else
                this.smerB.add(Base.getInstance().getStation(Integer.parseInt(bId)));
        //this.arrivalsA = arrivalsA;
        this.arrivalsA = null;
        //this.arrivalsB = arrivalsB;
        this.arrivalsB = null;
    }
    public void setDirections(String[] directionsA, String[] directionsB) {
        this.directionsA = new double[directionsA.length];
        this.directionsB = new double[directionsB.length];
        for(int i=0; i<directionsA.length; i++)
            this.directionsA[i] = Double.parseDouble(directionsA[i]);
        for(int i=0; i<directionsB.length; i++)
            this.directionsB[i] = Double.parseDouble(directionsB[i]);
    }


    public Line(String id, String type, List<Integer> smerAIds, List<Integer> smerBIds) {
        this.id = id;
        switch (type) {
            case "bus": this.type = TYPE_BUS;
                break;
            case "tram": this.type = TYPE_TRAM;
                break;
            case "trolleybus": this.type = TYPE_TROLLEY;
                break;
            case "walk": this.type = TYPE_WALK;
                break; //koristi se samo za specijalnu liniju walking
            case "initial": this.type = TYPE_INITIAL;
                break; //koristi se samo za specijalnu liniju initial
            default: throw new IllegalArgumentException("Invalid type: " + type);
        }

        if(smerAIds!=null && smerBIds != null && (smerAIds.size() > 0 || smerBIds.size() > 0)) {
            for(int aid : smerAIds)
                smerA.add(Base.getInstance().getStation(aid));
            for(int bid : smerBIds)
                smerB.add(Base.getInstance().getStation(bid));

            if(Config.USE_PRECISE_DIRECTIONS) { //ako koristim 'precizne' smerove, tj. za svaku stanicu gledam
                                                //DIRECTION_LOOKAHEAD stanica unapred
                directionsA = new double[smerA.size()];
                directionsB = new double[smerB.size()];
                //if(Config.USE_AVERAGE_BEARING_DIRECTIONS) {
                  //  setDirectionsAverage();
                //} else {
                    setDirectionsSimple();
                //}
            } else {
                directionsA = new double[DIRECTION_ESTIMATES];
                directionsB = new double[DIRECTION_ESTIMATES];
                setDirectionsPartial();
            }
        } else {
            directionsA = new double[0];
            directionsB = new double[0];
        }
        arrivalsA = new int[directionsA.length];
        arrivalsB = new int[directionsB.length];
        Arrays.fill(arrivalsA, FIELD_EMPTY);
        Arrays.fill(arrivalsB, FIELD_EMPTY);
    }

    /**
     * Deli liniju na DIRECTION_ESTIMATES delova i racuna smer za svaki slicno kao u setDirectionsSimple
     */
    private void setDirectionsPartial() {
        for (int i = 0; i < DIRECTION_ESTIMATES; i++) {
            directionsA[i] = smerA.get((i / DIRECTION_ESTIMATES) * smerA.size()).getLocation()
                    .bearingTo(smerA.get(((i + 1) / DIRECTION_ESTIMATES)).getLocation());
            if (smerB.size() > 0)
                directionsB[i] = smerB.get((i / DIRECTION_ESTIMATES) * smerB.size()).getLocation()
                        .bearingTo(smerB.get(((i + 1) / DIRECTION_ESTIMATES)).getLocation());
        }
    }

    /**
     * Odredjuje smer linije koja povezuje i-tu i (i+DIRECTION_LOOKAHEAD)-tu stanicu
     */
    private void setDirectionsSimple() {
        //uzimam ovu i (ovu+DIRECTION_LOOKAHEAD)tu stanicu i odredjujem ugao prema toj liniji
        for (int i = 0; i < smerA.size() - DIRECTION_LOOKAHEAD; i++)
            directionsA[i] = smerA.get(i).getLocation().bearingTo(smerA.get(i + DIRECTION_LOOKAHEAD).getLocation());
        for (int i = (smerA.size() < DIRECTION_LOOKAHEAD ? 0 : smerA.size() - DIRECTION_LOOKAHEAD); i < smerA.size(); i++)
            //jer postoje linije poput 38 sa izuzetno malim brojem stajalista
            directionsA[i] = smerA.get(i).getLocation().bearingTo(smerA.get(smerA.size() - 1).getLocation());
        for (int i = 0; i < smerB.size() - DIRECTION_LOOKAHEAD; i++)
            directionsB[i] = smerB.get(i).getLocation().bearingTo(smerB.get(i + DIRECTION_LOOKAHEAD).getLocation());
        for (int i = (smerB.size() < DIRECTION_LOOKAHEAD ? 0 : smerB.size() - DIRECTION_LOOKAHEAD); i < smerB.size(); i++)
            directionsB[i] = smerB.get(i).getLocation().bearingTo(smerB.get(smerB.size() - 1).getLocation());
    }

    /*
     * Potrošio sam celo popodne (i veče) na ovo što sledi, tako da mislim da zahteva malo istorije.
     * Cilj mi je bio optimizovati parsiranje Stringova, pošto je učitavanje trajalo oko 60s. S ovim
     * sam uspeo da smanjim deo koji se odnosi na linije za maltene trećinu, ali to opet nije
     * bilo dovoljno. Profajler je pokazivao da najviše Excl Real Time (ili tako nešto) koriste metode
     * iz String klase. Prva verzija, s duplo manjim datasetom se učitavala za 3-4s. Probao sam da umesto
     * fajlova učitavam podatke iz baze na sličan način, bez značajnog napretka.
     * Ne znajući šta dalje, logovao sam sve, dok nisam primetio da u petlji gde se poziva Scanner#nextLine
     * postoje predugačke pauze. Ponadao sam se da je to kraj, i da umesto #nextLine treba lepo da sve učitam
     * odjednom u neki niz. Kad sam to uradio, dobijao sam OutOfBounds greške u ovom konstruktoru, i logično
     * krivio sebe. Previše vremena mi je trebalo da se setim da uključim debugger i video da String koji
     * šaljem konstruktoru nije dobar i zapazio da je uvek dugačak 1024 karaktera. Pretpostavio sam da je neki
     * buffer, ali zašto bi \\Z, aka read until the end of input bufferovao input? Za početak, zato što
     * \\z i \\Z nisu stvarno until the end of input, već until the end of dokle mi stream dozvoli (lepo piše
     * u dokumentaciji...).
     * Verovatno sat vremena sam proveo tražeći rešenje, kako bih saznao da se assets/ kompresuje. Rešenje:
     * premestiti u res/raw/. Malo sutra, i to se kompresuje. Ovo ponašanje, za razliku od Scannera, nisam
     * pronašao u dokumentaciji. Ali, postoji i uslov za to kompresovanje: ekstenzija. Ako se fajl završava
     * na nešto što ne prepozna kao format koji je već kompresovan, on to učini. Super, promeniću nazive iz
     * lines.flat i stations.flat u samo lines i stations. A-a. Opet isto. OK, vratiću sve kao što je
     * originalno bilo, u jedan fajl "data".
     * Gle čuda, radi.
     * Onaj ogroman komentar u konstruktoru mi je svedok (znam, ne radi kako treba, npr. ne prepoznaje -1).
     *
     * -- rant mode: on --
     * GUGLE, RETARDE, ZAŠTO AUTOMATSKI KOMPRESUJEŠ MOJE RESURSE, I TO PREMA NAZIVU FAJLA? Uostalom, zar nisu
     * ekstenzije Windows thing, koliko znam magic numbers su makar malo pouzdaniji? I drugo, zašto
     * to nije dokumentovano, na nekom vidnom mestu, umesto po Google Product Forums i sl? Ako mislim
     * da mi je program preveliki, sam ću naći rešenje, NE TREBA MI NIKO KO ĆE DA SE NAĐE PAMETAN DA
     * DIRA MOJE RESURSE BEZ PITANJA.
     * Drugo, OD KOGA JE POTEKLA LAŽ DA SU ALGORITMI NAJBITNIJA STVAR? Svi poznati algoritmi ovde (osim Routing
     * interfejsa, koji je u potpunosti improvizovan) su direktno preuzeti sa Vikipedije ili Rozete i to je
     * bio bukvalno najkraći i najlakši deo, i svaki debil bi mogao da skapira kako da dođe do rešenja. Jeste,
     * budući programeri treba da znaju kako funkcioniše binarna pretraga, quicksort i Dijkstra, ali to se uvek
     * može naučiti, KOJI ĆE TI TO AKO TE NIKO NE UČI DA ČITAŠ I PIŠEŠ DOKUMENTACIJU, KORISTIŠ DEBUGGER I
     * PROFILER? KOJI JE SMISAO "PROGRAMIRANJA" U SVESCI NA ČASU, KONTROLNIMA I PISMENIMA, NA KOJIMA SE SKIDAJU
     * BODOVI ZA KOMPAJLERSKE GREŠKE FFS?
     * Takođe, DABOGDA CRKLI SVI KOJI SVOJOM VOLJOM KORISTE DEV-C++ (a oni koji ga preporučuju, u najgorim mukama)
     * -- rant mode: off --
     *
     * (Eto, bio sam blag. Background: pismeni iz informatike istog dana. Inače, nijedan deo koda ovde nema
     * apsolutno nikakvog dodira s bilo kojim školskim gradivom, niti vidim ikakvu praktičnu svrhu časova
     * informatike, osim u nekom usko-specijalizovanom polju koje se tiče naučnih istraživanja ili sl)
     */

    /**
     * Konstruktor koji prima string generisan od strane downloadera (poseban program)
     * @param machineReadableString
     */
    public Line(String machineReadableString) {
        Base base = Base.getInstance();
/*
        int i=0, j=0;
        while(machineReadableString.charAt(j) != '#') j++; //>>>>>>>>>>>>
        id = machineReadableString.substring(i, j);        //>>>> ID <<<<
        j++; i=j;                                          //>>>>>>>>>>>>
        while(machineReadableString.charAt(j) != '#') j++;              //>>>>>>>>>>>>
        type = Integer.parseInt(machineReadableString.substring(i, j)); //>>> TYPE <<<
        j++;                                                            //>>>>>>>>>>>>

        int sId;
        while(machineReadableString.charAt(j) != '#') {                            //----------------
            sId=0;                                                                 //================
            while (machineReadableString.charAt(j) != ',') {                       //>>>>>>>>>>>>>>>>
                sId = sId*10 + (machineReadableString.charAt(j)-'0');              //>>>>>>>>>>>>>>>>
                                                                                   //>>>> SMER A <<<<
                j++;                                                               //>>>>>>>>>>>>>>>>
            }                                                                      //>>>>>>>>>>>>>>>>
            j++;                                                                   //================
            smerA.add(base.getStation(sId));                                       //----------------
        }
        j++;
        while(machineReadableString.charAt(j) != '#') {                            //----------------
            sId=0;                                                                 //================
            while (machineReadableString.charAt(j) != ',') {                       //>>>>>>>>>>>>>>>>
                sId = sId*10 + (machineReadableString.charAt(j)-'0');              //>>>>>>>>>>>>>>>>
                //place++;                                                         //>>>> SMER B <<<<
                j++;                                                               //>>>>>>>>>>>>>>>>
            }                                                                      //>>>>>>>>>>>>>>>>
            j++;                                                                   //================
            smerB.add(base.getStation(sId));                                       //----------------
        }
        j++;

        directionsA = new double[smerA.size()+1];
        directionsB = new double[smerB.size()+1];
        int dirI, arrayIt=0, firstDecimal, secondDecimal;                           //-----------------------
        while(machineReadableString.charAt(j) != '#') {                             //-----------------------
            dirI=0;                                                                 //-----------------------
            while(machineReadableString.charAt(j) != '.') {                         //-----------------------
                dirI = dirI*10 + (machineReadableString.charAt(j)-'0');             //~~~~~~~~~~~~~~~~~~~~~~~
                                                                                    //~~~~~~~~~~~~~~~~~~~~~~~
                j++;                                                                //=======================
            }                                                                       //=======================
            j++;                                                                    //>>>>>>>>>>>>>>>>>>>>>>>
            firstDecimal=machineReadableString.charAt(j);                           //>>>>>>>>>>>>>>>>>>>>>>>
            secondDecimal=machineReadableString.charAt(j+1);                        //>>>  DIRECTIONS A   <<<
            if(secondDecimal==',') {                                                //>>>>>>>>>>>>>>>>>>>>>>>
                directionsA[arrayIt] = 0;                                           //>>>>>>>>>>>>>>>>>>>>>>>
                arrayIt++;                                                          //=======================
            } else {                                                                //=======================
                directionsA[arrayIt] = dirI + firstDecimal;                         //~~~~~~~~~~~~~~~~~~~~~~~
                if(secondDecimal < 0.5) directionsA[arrayIt]+=0.1;                  //~~~~~~~~~~~~~~~~~~~~~~~
                arrayIt++;                                                          //-----------------------
            }                                                                       //-----------------------
            while(machineReadableString.charAt(j) != ',') j++;                      //-----------------------
            j++;                                                                    //-----------------------
        }
        j++;
        arrayIt=0;
        while(machineReadableString.charAt(j) != '#') {                             //-----------------------
            dirI=0;                                                                 //-----------------------
            while(machineReadableString.charAt(j) != '.') {                         //-----------------------
                dirI = dirI*10 + (machineReadableString.charAt(j)-'0');             //~~~~~~~~~~~~~~~~~~~~~~~
                //place++;                                                          //~~~~~~~~~~~~~~~~~~~~~~~
                j++;                                                                //=======================
            }                                                                       //=======================
            j++;                                                                    //>>>>>>>>>>>>>>>>>>>>>>>
            firstDecimal=machineReadableString.charAt(j);                           //>>>>>>>>>>>>>>>>>>>>>>>
            secondDecimal=machineReadableString.charAt(j+1);                        //>>>  DIRECTIONS B   <<<
            if(secondDecimal==',') {                                                //>>>>>>>>>>>>>>>>>>>>>>>
                directionsB[arrayIt] = 0;                                           //>>>>>>>>>>>>>>>>>>>>>>>
                arrayIt++;                                                          //=======================
            } else {                                                                //=======================
                directionsB[arrayIt] = dirI + firstDecimal;                         //~~~~~~~~~~~~~~~~~~~~~~~
                if(secondDecimal < 0.5) directionsB[arrayIt]+=0.1;                  //~~~~~~~~~~~~~~~~~~~~~~~
                arrayIt++;                                                          //-----------------------
            }                                                                       //-----------------------
            while(machineReadableString.charAt(j) != ',') j++;                      //-----------------------
            j++;
        }                                                                           //-----------------------
        j++; i=j;
        while(machineReadableString.charAt(j) != '#') j++;                //>>>>>>>>>>>>>
        firstA = Integer.parseInt(machineReadableString.substring(i, j)); //>> FIRST A <<
        j++; i=j;                                                         //>>>>>>>>>>>>>
        while(machineReadableString.charAt(j) != '#') j++;               //>>>>>>>>>>>>>>
        lastA = Integer.parseInt(machineReadableString.substring(i, j)); //>>> LAST A <<<
        j++; i=j;                                                        //>>>>>>>>>>>>>>
        while(machineReadableString.charAt(j) != '#') j++;                //>>>>>>>>>>>>>
        firstB = Integer.parseInt(machineReadableString.substring(i, j)); //>> FIRST B <<
        j++; i=j;                                                         //>>>>>>>>>>>>>
        while(machineReadableString.charAt(j) != '#') j++;               //>>>>>>>>>>>>>>
        lastB = Integer.parseInt(machineReadableString.substring(i, j)); //>>> LAST B >>>
        j++; i=j;                                                        //>>>>>>>>>>>>>>
        int intervalVr, intervalVv;
        while(machineReadableString.charAt(j) != '#') j++;                    //>>>>>>>>>>>>>
        intervalVr = Integer.parseInt(machineReadableString.substring(i, j)); //>>> VRSNO <<<
        j++; i=j;                                                             //>>>>>>>>>>>>>>
        while(machineReadableString.charAt(j) != '#') j++;                    //>>>>>>>>>>>>>>
        intervalVv = Integer.parseInt(machineReadableString.substring(i, j)); //>> VANVRSNO <<
        j++; i=j;                                                             //>>>>>>>>>>>>>>
        interval=((3*intervalVr)+intervalVv)/4;
        while(machineReadableString.charAt(j) != '#') j++;                        //>>>>>>>>>>>>>>
        intervalSubota = Integer.parseInt(machineReadableString.substring(i, j)); //>>> SUBOTA <<<
        j++; i=j;                                                                 //>>>>>>>>>>>>>>
        while(machineReadableString.charAt(j) != '#') j++;                         //>>>>>>>>>>>>>>>
        intervalNedelja = Integer.parseInt(machineReadableString.substring(i, j)); //>>> NEDELJA <<<
        j++; i=j;                                                                  //>>>>>>>>>>>>>>>

*/

        StringTokenizer mainTokenizer = new StringTokenizer(machineReadableString, BaseIO.FIELD_SEPARATOR);
        id = mainTokenizer.nextToken();
        type = Integer.parseInt(mainTokenizer.nextToken());

        StringTokenizer arrayTokenizer = new StringTokenizer(mainTokenizer.nextToken(), ",");
        while(arrayTokenizer.hasMoreTokens())
            smerA.add(base.getStation(Integer.parseInt(arrayTokenizer.nextToken())));
        arrayTokenizer = new StringTokenizer(mainTokenizer.nextToken(), ",");
        while(arrayTokenizer.hasMoreTokens())
            smerB.add(base.getStation(Integer.parseInt(arrayTokenizer.nextToken())));
        directionsA = new double[smerA.size()+1]; directionsB = new double[smerB.size()+1];
        arrayTokenizer = new StringTokenizer(mainTokenizer.nextToken(), ",");
        for(int i=0; arrayTokenizer.hasMoreTokens(); i++)
            directionsA[i] = Double.parseDouble(arrayTokenizer.nextToken());
        try {
            arrayTokenizer = new StringTokenizer(mainTokenizer.nextToken(), ",");
        } catch (NoSuchElementException e) {
            Log.e("Line", "NSEE: " + id);
        }
        for(int i=0; arrayTokenizer.hasMoreTokens(); i++)
            directionsB[i] = Double.parseDouble(arrayTokenizer.nextToken());

        firstA = Integer.parseInt(mainTokenizer.nextToken());
        lastA = Integer.parseInt(mainTokenizer.nextToken());
        if(lastA == 0) lastA=1440;
        firstB = Integer.parseInt(mainTokenizer.nextToken());
        lastB = Integer.parseInt(mainTokenizer.nextToken());
        if(lastB == 0) lastB=1440;
        int intervalVr = Integer.parseInt(mainTokenizer.nextToken());
        int intervalVv = Integer.parseInt(mainTokenizer.nextToken());
        if(intervalVv != FIELD_EMPTY)
            interval = (intervalVr*2 + intervalVv)/3;
        else
            interval = intervalVr;
        intervalSubota = Integer.parseInt(mainTokenizer.nextToken());
        intervalNedelja = Integer.parseInt(mainTokenizer.nextToken());

        if(id.toLowerCase().startsWith("ada")) {
            interval = intervalSubota = intervalNedelja = 40;
            firstA = firstB = 540;
            lastA = lastB = 1260;
        }

        arrivalsA = null; arrivalsB=null;
    }

    private FullPath getStations(List<Station> smer) {
        List<FullPath.LineStationPair> st = new ArrayList<>(smer.size());
        for(Station s : smer) {
            st.add(new FullPath.LineStationPair(this, s));
        }
        return new FullPath(st, 0);
    }

    public FullPath getStations(boolean smer) {
        if(smer == SMER_A)
            return getStations(smerA);
        else
            return getStations(smerB);
    }

    /*
     * Specijalni slucajevi
     */
    public static Line getInitial() {
        return initial;
    }
    public static Line getWalking() {
        return walking;
    }
    public boolean isInitial() {
        return this == initial;
    }
    public boolean isWalking() {
        return this == walking;
    }

    public static Line getSpecial(String id) {
        if(initial.getId().equals(id))
            return initial;
        if(walking.getId().equals(id))
            return walking;
        return null;
    }

    /**
     * Mapira tip prevoza u koeficijent
     */
    public double typeCostCoefficient() {
        switch (type) {
            case TYPE_BUS:
                return AlgorithmParameters.getCostBus();
            case TYPE_TRAM:
                return AlgorithmParameters.getCostTram();
            case TYPE_TROLLEY:
                return AlgorithmParameters.getCostTrolley();
            case TYPE_WALK:
                return AlgorithmParameters.getCostWalk();
            case TYPE_INITIAL:
                return AlgorithmParameters.getCostInitial();
            default: throw new IllegalArgumentException("Wrong type: " + type);
        }
    }

    @DrawableRes
    public int getDrawableResId() {
        switch (type) {
            case TYPE_BUS:
                return R.drawable.ic_directions_bus;
            case TYPE_TRAM:
                return R.drawable.ic_directions_tram;
            case TYPE_TROLLEY:
                return R.drawable.ic_directions_trolley;
            case TYPE_WALK:
                return R.drawable.ic_directions_walk;
            case TYPE_INITIAL:
                return R.drawable.ic_location;
            default:
                Log.e("bgbus.Line", "Invalid line type " + type + " for line " + id);
                return R.drawable.ic_directions_bus;
        }
    }

    private boolean getSmer(Station st) {
        if(smerA.contains(st))
            return SMER_A;
        else
            return SMER_B;
    }

    public boolean isValid(Context context) {
        if(isWalking() || isInitial()) return true;
        boolean ada = id.toLowerCase().startsWith("ada");
        boolean its2 = (interval>=60 || interval<0) && !ada;
        boolean nocni = id.toLowerCase().endsWith("n");
        if(ada || its2 || nocni) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            if(ada && !pref.getBoolean("pref_ada", false))
                return false;
            if(its2 && !pref.getBoolean("pref_its2", true))
                return false;
            if(nocni && (!pref.getBoolean("pref_nightlines", true) || !pref.getBoolean("pref_use_time", true)))
                return false;
        }
        return true;
    }

    //DO NOT CHANGE, lvl 2. See Pathfinder, apply same rules.
    private int lastDayA=-1001, lastTimeA=-1001, lastDayB=-1001, lastTimeB=-1001;
    private double lastResultA, lastResultB;
    /**
     * Vraća koliko košta presedanje, na osnovu intervala na koji ide ova linija i trenutnog vremena.
     * @param current linija kojom se do tog trenutka kretao
     * @param time vreme presedanja, HOUR_OF_THE_DAY*3600 + MINUTES*60. -1 ako ne treba uzeti u obzir prve i
     *             poslednje polaske
     * @param day dan, 0-6
     * @param from stanica na kojoj preseda
     * @param to   sledeća stanica
     * @return cena presedanja
     */
    //Station to can be inferred from current and is expected to be next one (or else caching may fail)
    public int getSwitchCost(Line current, int time, int day, Station from, Station to) {
        boolean smer          = getSmer(to);
        int     stationOffset = 0;
        //if(smer == SMER_A) stationOffset=(int)(smerA.get(0).getLocation().distanceBetween(station.getLocation()) / 420);
        //else               stationOffset=(int)(smerB.get(0).getLocation().distanceBetween(station.getLocation()) / 420);
        //uncommenting above lines caused sorting algorithm to loop. That makes no sense at all.
        double waitingTime;
        double dist = from.getLocation().distanceBetween(to.getLocation());

        if (current.isWalking() && isWalking()) {
            waitingTime = 2;
        } else if (isWalking() && dist < 10) {//prelazenje ulice
            waitingTime = 0.2;
        } else {
            if (current.isInitial() && isWalking()) {
                if (dist <= 40) // TODO: 19.11.15. test numbers
                    waitingTime = 0; // TODO: 19.11.15. test performance
                else if (dist <= 130)
                    waitingTime = 0.5;
                else
                    waitingTime = AlgorithmParameters.getWalkInterval();
            } else if (smer == SMER_A && lastDayA == day && Math.abs(lastTimeA - time) < 5) {
                waitingTime = lastResultA;
            } else if (smer == SMER_B && lastDayB == day && Math.abs(lastTimeB - time) < 5) {
                waitingTime = lastResultB;
            } else if (isWalking()) {
                waitingTime = AlgorithmParameters.getWalkInterval();
            } else if (time != FIELD_EMPTY && smer == SMER_A && (time < firstA + stationOffset
                                                               || time > lastA + stationOffset)) {
                waitingTime = Math.abs(firstA - time);
            } else if (time != FIELD_EMPTY && smer == SMER_B && (time < firstB + stationOffset
                                                                 || time > lastB + stationOffset)) {
                waitingTime = Math.abs(firstB - time);
            } else {
                int apprInterval;
                switch (day) {
                    case Calendar.SATURDAY:
                        apprInterval = intervalSubota;
                        break;
                    case Calendar.SUNDAY:
                        apprInterval = intervalNedelja;
                        break;
                    default:
                        apprInterval = interval;
                        break;
                }
                if (apprInterval != FIELD_EMPTY)
                    waitingTime = apprInterval;
                else
                    waitingTime = AlgorithmParameters.getDefaultWaitingTime();
            }
        }

        if(smer==SMER_A) {
            lastDayA = day;
            lastTimeA = time;
            lastResultA = waitingTime;
        } else {
            lastDayB = day;
            lastTimeB = time;
            lastResultB = waitingTime;
        }
        if(current.isInitial()) waitingTime/=2;
        return (int)(waitingTime*AlgorithmParameters.getCostToMinRatio());
    }

    private int getWaitingTime(int time, int[] arrivals) {
        if(arrivals == null) return interval+1;
        for (int arrival : arrivals) // FIXME: 10.11.15. arrivals treba da postoje za svaku stanicu ponaosob
            if (arrival != FIELD_EMPTY && time < arrival)
                return arrival - time;
        return interval+1;
    }

    /**
     * Vraca pravac u kojem se linija krece od date stanice. Rezultat zavisi od {@link Config#USE_PRECISE_DIRECTIONS}
     * @param s stanica za koju se traži pravac kretanja
     * @return pravac, u odnosu na paralelu griničkog meridijana u stepenima
     */
    public double getDirectionAt(Station s) {
        if(smerA == null || smerA.size() == 0) return 0;
        int index = smerA.indexOf(s);
        if (index >= 0) {
            return directionsA[Config.USE_PRECISE_DIRECTIONS ?
                                index :
                                ((int) (((float) index / smerA.size()) * DIRECTION_ESTIMATES))];
        } else {
            index = smerB.indexOf(s);
            if(index == -1) {
                Log.e("Line", "Invalid station " + s + " (id " + s.getId() + ") for line " + this);
                Log.e("Line", "Stations A: " + smerA);
                Log.e("Line", "Stations B: " + smerB);
                throw new ArrayIndexOutOfBoundsException(-1);
            }
            return directionsB[Config.USE_PRECISE_DIRECTIONS ?
                                index :
                                (int) (((float) index / smerB.size()) * DIRECTION_ESTIMATES)];
        }
    }

    public String getId() {
        return id;
    }

    /**
     * Returns station after the given one, using current line, in the appropriate direction(s)
     * @param s
     * @return
     */
    //Lista, jer neki autobusi koriste istu stanicu kao okretnicu. Ocekujem da su takvi slucajevi retki.
    public List<Station> getStationAfter(Station s) {
        List<Station> ret = new LinkedList<>();
        if(smerA == null || smerA.size() == 0) return null;
        int indexA = smerA.indexOf(s);
        int indexB = smerB.indexOf(s);
        if(indexA >= 0) {
            if(indexA + 1 < smerA.size())
                ret.add(smerA.get(indexA + 1));
            else if(isCircular())
                ret.add(smerA.get(0));
        }
        if(indexB >= 0) {
            if(indexB + 1 < smerB.size())
                ret.add(smerB.get(indexB + 1));
        }
        return ret;
    }

    /**
     * Vraca broj obliznjih 'skupih' stanica (stanica koje su {@link LatLng#isExpensive}, tj. nalaze se u blizini
     * mesta na kojima je poznato da ce se prevoz kretati sporije nego obicno).
     * @param s
     * @return
     */
    public int getNumberOfNearbyExpensiveStations(Station s) {
        if(smerA == null || smerA.size() == 0) return 0;
        int index = smerA.indexOf(s), expensive=0;
        if(index >= 0) {
            for(int i=0; i< AlgorithmParameters.getExpensiveStationsLookahead() && index + i < smerA.size(); i++)
                if(smerA.get(index+i).getLocation().isNearBusySpot())
                    expensive++;
            return expensive;
        } else {
            index = smerB.indexOf(s);
            for(int i=0; i< AlgorithmParameters.getExpensiveStationsLookahead() && index + i < smerB.size(); i++)
                if(smerB.get(index+i).getLocation().isNearBusySpot())
                    expensive++;
            return expensive;
        }
    }

    public boolean isCircular() {
        return smerB.isEmpty() && !isWalking() && !isInitial();
        //pretpostavljam da su linije koje nemaju drugi smer kruzne (tom logikom 2 nije kruzna,
        //ali racunam da su slucajevi kada je prelazenje 'preko' okretnice optimalni retki).
    }
    //----------------overrideovi----------------------------------

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Line && id.equals(((Line) obj).id))
                || (obj instanceof String && id.equals(obj)); //prihvatam i String, kako bih iz mape u bazi mogao da
                                                              //izvucem liniju samo po njenom id-u
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
