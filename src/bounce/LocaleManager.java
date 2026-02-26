package bounce;

import java.io.DataInputStream;
import java.io.InputStream;

/**
 * Loads localized UI strings from a binary language resource file.
 * Originally: Class_17a (obfuscated).
 *
 * The resource is at /bounce/lang.xx (language code, e.g., "en") and contains
 * an array of UTF strings encoded with length offsets.
 *
 * Ported to Java SE 1.8.
 */
public class LocaleManager {

    // --- String index constants ---
    public static final short STR_ABOUT_0         = 0;
    public static final short STR_ABOUT_1         = 1;
    public static final short STR_ABOUT_2         = 2;
    public static final short STR_ABOUT_3         = 3;
    public static final short STR_ABOUT_4         = 4;
    public static final short STR_ABOUT_5         = 5;
    public static final short STR_BACK            = 6;
    public static final short STR_MENU_TITLE      = 7;
    public static final short STR_MENU_RESUME     = 8;
    public static final short STR_MENU_EXIT       = 9;
    public static final short STR_MENU_COLORS     = 10;
    public static final short STR_GAME_OVER       = 11;
    public static final short STR_HIGH_SCORE      = 12;
    public static final short STR_SETTINGS        = 13;
    public static final short STR_LEVEL_COMPLETE  = 14;
    public static final short STR_SCORE_LABEL     = 15;
    public static final short STR_FONT_FACE       = 16;
    public static final short STR_NEW_RECORD      = 17;
    public static final short STR_SOUND_ON        = 18;
    public static final short STR_OK              = 19;
    public static final short STR_PAUSE           = 20;

    /** Resolved locale string, e.g., "en_US", "fi", etc. */
    public static final String LOCALE = System.getProperty("user.language", "xx");

    private static LocaleManager instance = null;

    // ---- Public API --------------------------------------------------------

    /**
     * Get a localized string by index (no substitution parameters).
     */
    public static synchronized String getString(int index) {
        return getString(index, null);
    }

    /**
     * Get a localized string by index with optional %U or %0U/%1U substitutions.
     */
    public static synchronized String getString(int index, String[] params) {
        try {
            if (instance == null) {
                instance = new LocaleManager();
            }

            InputStream stream = instance.getClass().getResourceAsStream("/bounce/lang." + LOCALE);
            if (stream == null) {
                stream = instance.getClass().getResourceAsStream("/bounce/lang.xx");
            }
            if (stream == null) {
                return "NoLang";
            }

            DataInputStream dis = new DataInputStream(stream);
            // Each entry has a 2-byte offset; skip to the entry at 'index'
            dis.skipBytes(index * 2);
            short offset = dis.readShort();
            dis.skipBytes(offset - index * 2 - 2);
            String text = dis.readUTF();
            dis.close();

            if (params != null) {
                if (params.length == 1) {
                    text = replaceFirst(text, "%U", params[0]);
                } else {
                    for (int i = 0; i < params.length; i++) {
                        text = replaceFirst(text, "%" + i + "U", params[i]);
                    }
                }
            }
            return text;

        } catch (Exception e) {
            return "Err";
        }
    }

    // ---- Private helpers ---------------------------------------------------

    /** Replace the first occurrence of 'token' in 'source' with 'replacement'. */
    private static String replaceFirst(String source, String token, String replacement) {
        int idx = source.indexOf(token);
        if (idx < 0) return source;
        return source.substring(0, idx) + replacement + source.substring(idx + token.length());
    }
}
