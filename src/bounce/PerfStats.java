package bounce;

/**
 * Performance and timing statistics counters.
 * Originally: Class_bc (obfuscated)
 * Ported to Java SE 1.8.
 */
public class PerfStats {

    public static final boolean ENABLED = false;

    // --- Render timing ---
    public static long renderStartTime    = 0L;
    public static long renderEndTime      = 0L;
    public static int  renderFrameCount   = 0;

    // --- Physics timing ---
    public static long physicsStartTime   = 0L;
    public static long physicsEndTime     = 0L;
    public static int  physicsFrameCount  = 0;

    // --- Input timing ---
    public static long inputStartTime     = 0L;
    public static long inputEndTime       = 0L;
    public static int  inputEventCount    = 0;

    // --- Level load timing ---
    public static long levelLoadStart     = 0L;
    public static long levelLoadEnd       = 0L;
    public static int  levelLoadCount     = 0;

    // --- Sound timing ---
    public static long soundStartTime     = 0L;
    public static long soundEndTime       = 0L;
    public static int  soundPlayCount     = 0;

    // --- Utility ---
    public static void reset() {
        renderStartTime   = 0L; renderEndTime   = 0L; renderFrameCount  = 0;
        physicsStartTime  = 0L; physicsEndTime  = 0L; physicsFrameCount = 0;
        inputStartTime    = 0L; inputEndTime    = 0L; inputEventCount   = 0;
        levelLoadStart    = 0L; levelLoadEnd    = 0L; levelLoadCount    = 0;
        soundStartTime    = 0L; soundEndTime    = 0L; soundPlayCount    = 0;
    }
}
