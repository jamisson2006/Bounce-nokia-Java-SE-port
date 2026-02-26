package bounce;

/**
 * Game constants for Bounce (Nokia 2005).
 * Originally: Class_66 (obfuscated)
 * Ported to Java SE 1.8 by open-source contributors.
 */
public class GameConstants {

    // --- Debug flags ---
    public static final boolean DEBUG          = false;
    public static final boolean DEBUG_VERBOSE  = false;

    // --- Ball size codes ---
    public static final int BALL_SIZE_LARGE    = 8;
    public static final int BALL_SIZE_MEDIUM   = 4;
    public static final int BALL_SIZE_SMALL    = 2;
    public static final int BALL_SIZE_TINY     = 1;
    public static final int BALL_SIZE_NONE     = 0;

    // --- Tile pixel sizes ---
    public static final int TILE_PX            = 12;   // tile side in pixels
    public static final int HALF_TILE          = 6;
    public static final int DOUBLE_TILE        = 12;   // same (legacy)
    public static final int QUAD_TILE          = 6;    // legacy quarter

    // --- Screen dimensions (Nokia 6600) ---
    public static final int SCREEN_WIDTH       = 132;
    public static final int SCREEN_HEIGHT      = 176;
    public static final int LEVEL_WIDTH_PX     = 156;  // scrolling viewport
    public static final int LEVEL_HEIGHT_PX    = 96;   // visible level area
    public static final int HUD_HEIGHT         = 48;   // header bar height
    public static final int STATUS_HEIGHT      = 32;   // status bar height

    // --- Scroll/wrap widths ---
    public static final int SCROLL_COLS        = 13;   // visible columns
    public static final int SCROLL_WRAP        = 156;  // wrap boundary in px

    // --- Physics ---
    public static final int GRAVITY_NORMAL     = 80;   // downward accel
    public static final int GRAVITY_STEP       = 4;
    public static final int GRAVITY_MOON       = -30;  // anti-gravity tile
    public static final int GRAVITY_MOON_STEP  = -2;
    public static final int VEL_MAX            = 150;  // max velocity (fixed-point /10)
    public static final int VEL_JUMP           = -67;  // initial jump velocity
    public static final int VEL_BOUNCE_MIN     = 10;   // min bounce speed
    public static final int VEL_LATERAL_STEP   = 6;    // horizontal accel
    public static final int VEL_LATERAL_DECAY  = 4;    // horizontal friction
    public static final int VEL_LATERAL_MAX_NORMAL = 50;
    public static final int VEL_LATERAL_MAX_BOOST   = 100;

    // --- Score values ---
    public static final int SCORE_GEM          = 200;
    public static final int SCORE_TUBE         = 500;
    public static final int SCORE_BALL         = 1000;
    public static final int SCORE_LIFE         = 1000; // same as ball pickup
    public static final int SCORE_NEXT_LEVEL   = 5000;

    // --- Timing ---
    public static final int SPLASH_FRAMES      = 30;   // splash screen hold frames
    public static final int LEVEL_NAME_FRAMES  = 120;  // frames to show level name

    // --- Lives ---
    public static final int LIVES_START        = 3;
    public static final int LIVES_MAX          = 5;

    // --- Image / spritesheet layout ---
    public static final int SPRITE_SIZE        = 12;   // sprite tile pixel size
    public static final int BALL_RADIUS_SMALL  = 6;
    public static final int BALL_RADIUS_LARGE  = 8;
    public static final int BALL_DIAM_SMALL    = 12;
    public static final int BALL_DIAM_LARGE    = 16;

    // --- Color palette (0xRRGGBB) ---
    public static final int COLOR_BLACK        = 0x000000;   // -16777216
    public static final int COLOR_TILE_LIGHT   = 0xB0D0F0;  // 11591920
    public static final int COLOR_TILE_DARK    = 0x106070;  // 1073328
    public static final int COLOR_SCORE        = 0xFFFFFE;  // 16777214
    public static final int COLOR_ORANGE       = 0xFFB013;  // 16750611
    public static final int COLOR_STATUS_BG    = 0x08512A;  // 545706

    // --- Menu item indices ---
    public static final int MENU_RESUME        = 0;
    public static final int MENU_NEW_GAME      = 1;
    public static final int MENU_HIGH_SCORE    = 2;
    public static final int MENU_SETTINGS      = 3;
    public static final int MENU_ABOUT         = 4;
    public static final int MENU_EXIT          = 5;
    public static final int MENU_COUNT         = 6;

    // --- Game mode codes ---
    public static final int MODE_IN_GAME       = 1;
    public static final int MODE_NEW_GAME      = 2;
    public static final int MODE_GAME_OVER     = 3;
    public static final int MODE_NEW_GAME_FLAG = 4;

    // --- Tile type IDs (level data values) ---
    public static final int TILE_EMPTY         = 0;
    public static final int TILE_WALL          = 1;
    public static final int TILE_SPIKED_WALL   = 2;
    public static final int TILE_SPIKE_R       = 3;
    public static final int TILE_SPIKE_L       = 4;
    public static final int TILE_SPIKE_U       = 5;
    public static final int TILE_SPIKE_D       = 6;
    public static final int TILE_TELEPORT      = 7;
    public static final int TILE_SOLID         = 8;
    public static final int TILE_PORTAL        = 9;
    public static final int TILE_MOVING_ENEMY  = 10;
    public static final int TILE_LIFE_PICKUP   = 13;
    public static final int TILE_LIFE_R        = 14;
    public static final int TILE_LIFE_U        = 15;
    public static final int TILE_LIFE_D        = 16;
    public static final int TILE_ANTIGRAV_CEIL = 17;
    public static final int TILE_ANTIGRAV_CEIL2= 18;
    public static final int TILE_SCORE_L       = 19;
    public static final int TILE_SCORE_R       = 20;
    public static final int TILE_SCORE_U       = 21;
    public static final int TILE_SCORE_D       = 22; // up-left corner score
    public static final int TILE_GROW_R        = 23;
    public static final int TILE_GROW_L        = 24;
    public static final int TILE_GROW_U        = 25;
    public static final int TILE_GROW_D        = 26; // grow-right corner
    public static final int TILE_SHRINK_U      = 27;
    public static final int TILE_SHRINK_D      = 28;
    public static final int TILE_EXTRA_LIFE    = 29;
    public static final int TILE_BOUNCE_R      = 30;
    public static final int TILE_BOUNCE_L      = 31;
    public static final int TILE_BOUNCE_U      = 32;
    public static final int TILE_BOUNCE_D      = 33;
    public static final int TILE_BOUNCE_DR     = 34;
    public static final int TILE_BOUNCE_DL     = 35;
    public static final int TILE_BOUNCE_UR     = 36;
    public static final int TILE_BOUNCE_UL     = 37;
    public static final int TILE_SPEED_BOOST   = 38;
    public static final int TILE_SLOWDOWN_R    = 39;
    public static final int TILE_SLOWDOWN_L    = 40;
    public static final int TILE_SLOWDOWN_U    = 41;
    public static final int TILE_SLOWDOWN_D    = 42;
    public static final int TILE_STICKY_R      = 43;
    public static final int TILE_STICKY_L      = 44;
    public static final int TILE_STICKY_U      = 45;
    public static final int TILE_STICKY_D      = 46;
    public static final int TILE_SHRINK_ITEM1  = 47;
    public static final int TILE_SHRINK_ITEM2  = 48;
    public static final int TILE_SHRINK_ITEM3  = 49;
    public static final int TILE_SHRINK_ITEM4  = 50;
    public static final int TILE_GROW_ITEM1    = 51;
    public static final int TILE_GROW_ITEM2    = 52;
    public static final int TILE_GROW_ITEM3    = 53;
    public static final int TILE_GROW_ITEM4    = 54;
    public static final int TILE_MAX_ID        = 64;   // highest tile ID

    // --- Resource paths ---
    public static final String RES_NOKIA_LOGO  = "/bounce/icons/nokiagames.png";
    public static final String RES_SPLASH      = "/bounce/icons/bouncesplash.png";
    public static final String RES_SND_UP      = "/bounce/sounds/up.mid";
    public static final String RES_SND_PICKUP  = "/bounce/sounds/pickup.mid";
    public static final String RES_SND_POP     = "/bounce/sounds/pop.mid";

    // --- Save slot IDs ---
    public static final int SAVE_LEVEL         = 1;
    public static final int SAVE_HIGHSCORE     = 2;

    // --- High score digit encoding ---
    public static final int HS_DIGITS          = 8;
}
