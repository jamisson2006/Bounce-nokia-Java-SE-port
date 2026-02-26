package bounce;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Game loop timer — fires every 30 ms and calls the canvas tick method.
 * Originally: Class_f3 (obfuscated).
 * Ported to Java SE 1.8.
 */
public class GameTimer extends TimerTask {

    /** The canvas whose tick() method is called each frame. */
    GameCanvas target;

    /** Underlying Java timer object. */
    Timer timer;

    /** Frame interval in milliseconds (≈33 fps). */
    private static final long FRAME_MS = 30L;

    // -------------------------------------------------------------------------

    /**
     * Create and immediately start the game loop.
     *
     * @param owner  the canvas that created this timer
     * @param target the canvas that receives tick() calls (usually same as owner)
     */
    public GameTimer(GameCanvas owner, GameCanvas target) {
        this.target = target;
        this.timer  = new Timer("GameLoop", true); // daemon thread
        this.timer.schedule(this, 0L, FRAME_MS);
    }

    /**
     * Called every frame by the Timer thread.
     * Delegates to the canvas tick() method.
     */
    @Override
    public void run() {
        target.tick();
    }

    /**
     * Stop and clean up this timer.
     */
    public void stopTimer() {
        if (timer != null) {
            cancel();
            timer.cancel();
            timer = null;
        }
    }
}
