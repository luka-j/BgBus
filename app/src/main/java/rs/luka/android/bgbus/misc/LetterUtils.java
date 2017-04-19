package rs.luka.android.bgbus.misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

 /*
  * Planirana namena mu je bila transformisanje imena u ASCII za pretragu, ali sam ipak to generisao i ubacio
  * među podatke
  */

/**
 * Unused
 * Created by luka on 23.10.15..
 */
public class LetterUtils {

    /*
     * Mape su izvučene iz https://github.com/luq-0/AskWords
     */

    private static final Map<Character, String> toLatin         = new HashMap<>();
    private static final Map<Character, String> stripGuillemets = new HashMap<>();
    private static final Set<Character>         symbols         = new HashSet<>();

    static {
        toLatin.put('љ', "lj");
        toLatin.put('њ', "nj");
        toLatin.put('е', "e");
        toLatin.put('р', "r");
        toLatin.put('т', "t");
        toLatin.put('з', "z");
        toLatin.put('у', "u");
        toLatin.put('и', "i");
        toLatin.put('о', "o");
        toLatin.put('п', "p");
        toLatin.put('ш', "š");
        toLatin.put('ђ', "đ");
        toLatin.put('а', "a");
        toLatin.put('с', "s");
        toLatin.put('д', "d");
        toLatin.put('ф', "f");
        toLatin.put('г', "g");
        toLatin.put('х', "h");
        toLatin.put('ј', "j");
        toLatin.put('к', "k");
        toLatin.put('л', "l");
        toLatin.put('ч', "č");
        toLatin.put('ћ', "ć");
        toLatin.put('ж', "ž");
        toLatin.put('џ', "dž");
        toLatin.put('ц', "c");
        toLatin.put('в', "v");
        toLatin.put('б', "b");
        toLatin.put('н', "n");
        toLatin.put('м', "m");

        stripGuillemets.put('š', "s");
        stripGuillemets.put('đ', "dj");
        stripGuillemets.put('č', "c");
        stripGuillemets.put('ć', "c");
        stripGuillemets.put('ž', "z");

        symbols.add(',');
        symbols.add('.');
        symbols.add('!');
        symbols.add('?');
        symbols.add('-');
        symbols.add('_');
        symbols.add('"');
        symbols.add('\'');
        symbols.add(':');
        symbols.add(';');
        symbols.add(')');
        symbols.add('(');
        symbols.add('<');
        symbols.add('3');
        symbols.add('>');
        symbols.add('♥');
        symbols.add('♡');
        symbols.add('❤');
        symbols.add('^');
        symbols.add('#');
        symbols.add('&');
        symbols.add('{');
        symbols.add('}');
        symbols.add('\\');
        symbols.add('=');
        symbols.add('%');
        symbols.add('*');
        symbols.add('░');
        symbols.add('█');
        symbols.add('▄');
        symbols.add('▀');

    }

    public static String removeSpecialChars(String word) {
        StringBuilder builder = new StringBuilder(word);
        for(int i=0; i<builder.length(); i++) {
            if(toLatin.containsKey(builder.charAt(i)))
                builder.replace(i, i+1, toLatin.get(builder.charAt(i)));
            else if(stripGuillemets.containsKey(builder.charAt(i)))
                builder.replace(i, i+1, stripGuillemets.get(builder.charAt(i)));
            else if(symbols.contains(builder.charAt(i)))
                builder.deleteCharAt(i);
        }
        return builder.toString();
    }
}
