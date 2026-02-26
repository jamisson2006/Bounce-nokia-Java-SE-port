package bounce;

import javax.swing.SwingUtilities;

/**
 * Main entry point for Bounce (Nokia 2005) – Java SE 1.8 open-source port.
 *
 * Originally: Bounce.java (MIDlet), com.nokia.mid.appl.boun
 *
 * HOW TO RUN:
 *   Ensure the game resources (levels/, icons/, sounds/, lang.*) are on the classpath.
 *   java -cp bounce-se.jar bounce.BounceMain
 *
 * Ported to Java SE 1.8, open-source.
 */
public class BounceMain {

    /** Application-level controller (replaces MIDlet lifecycle). */
    private GameController controller;

    // ---- Constructor -------------------------------------------------------

    public BounceMain() {
        controller = new GameController(this);
    }

    // ---- Cleanup -----------------------------------------------------------

    /**
     * Shut down cleanly (replaces MIDlet.destroyApp).
     */
    public void destroy() {
        if (controller != null) {
            GameScreen gs = controller.getGameScreen();
            if (gs != null) gs.stopTimer();
        }
        controller = null;
        System.gc();
    }

    // ---- Entry point -------------------------------------------------------

    public static void main(String[] args) {
        // All Swing operations must happen on the Event Dispatch Thread
        SwingUtilities.invokeLater(BounceMain::new);
    }
}
