package rs.luka.android.bgbus.misc;

import android.os.Build;

/*
 * Namenjen prvenstveno verziji ovoga za desktop. Ne znam gde se i kako tačno upotrebljava sve ovo, a nemam vremena
 * da opet sve proveravam, pa ću ostaviti i rizikovati koji System.out.println(String) viška.
 */

/**
 * Odredjuje globalnu konfiguraciju programa
 * Created by luka on 18.9.15.
 */
public class Config {

    //<LEGACY>
    public static boolean DEBUG = false; //logging, default home, neki ispisi na System.out i sl.

    public static final boolean WALKING_ENABLED                = true;
    /**@deprecated */
    public static final boolean PRECALCULATE_DIRECTIONS        = false; //up to 1.5min @ first run, ne koristi se
    /**@deprecated */
    public static final boolean USE_PRECISE_PARALLEL_RADIUS    = true; //takodje, ostalo od ranije
    public static final boolean USE_CACHE                      = true; //see BaseIO
    public static final boolean USE_PRECISE_DIRECTIONS         = true;
    //false oznacava parcijalno racunanje smera, see Line
    public static final boolean USE_AVERAGE_BEARING_DIRECTIONS = true;
    //za ovo nisam siguran, treba vise testirati, see Line

    //</LEGACY>


    private static int HARDWARE_ID = -1; //stats reporting
    public static boolean REPORT_STATS = false; //nedovrseno


    public static int getHardwareId() {
        if (HARDWARE_ID == -1) {
            HARDWARE_ID = Utils.concatStrings(Build.BOARD, Build.BOOTLOADER, Build.BRAND, Build.DEVICE,
                                              Build.HARDWARE, Build.MODEL, Build.PRODUCT, Build.SERIAL)
                               .hashCode();
        }
        return HARDWARE_ID;
    }
}
