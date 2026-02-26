package bounce;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.sound.midi.*;
import javax.swing.SwingUtilities;

/**
 * Main game screen — rendering, key input, HUD, audio, game logic per frame.
 * Originally: Class_14b (obfuscated), extends J2ME Canvas.
 * Ported to Java SE 1.8 — extends GameCanvas (JPanel).
 */
public class GameScreen extends GameCanvas implements KeyListener {

    private static final long serialVersionUID = 1L;

    // ---- Splash screen ----
    public int  splashIndex;       // 0=nokia logo, 1=bounce splash, -1=done (was var_57)
    public BufferedImage splashImage; // current splash image (was var_6c)
    private int splashHoldFrames;  // frames shown so far (was var_b7)

    // ---- Off-screen buffers ----
    private BufferedImage screenBuffer;  // 132×176 full-screen buffer (was var_4ca)
    public  Graphics2D    screenG;       // Graphics for screenBuffer (was var_52d)
    private BufferedImage headImage;     // header background 132×48 (was var_cf)

    // ---- Font ----
    private Font gameFont; // small mono font for HUD (was var_4b8)

    // ---- Audio players ----
    private Sequencer soundUp;     // gem collect (was var_11b)
    private Sequencer soundPickup; // powerup     (was var_189)
    private Sequencer soundPop;    // death/pop   (was var_1af)

    // ---- Game-specific state ----
    public int  score;           // current score (was var_353)
    public int  levelNameTimer;  // frames to show level name (was var_3e8)
    public int  powerBarValue;   // sidebar power indicator (was var_3c7)
    public boolean nextLevel;    // advance to next level flag (was var_426)
    public boolean allGemsGot;   // all gems collected (was var_44a)
    public boolean needsClear;   // blank screen before draw (was var_537)

    // ---- Cheat code state ----
    private boolean cheatDevMode;  // level-skip enabled (was var_56e, set by cheat code)
    private int cheatStep;       // (was var_5e7)

    // ---- Splash image list ----
    private static final String[] SPLASH_IMAGES = {
        "/bounce/icons/nokiagames.png",
        "/bounce/icons/bouncesplash.png"
    };

    // ---- Reference to controller ----
    private GameController controller;

    // ---- Transition guard — prevents modal dialogs stacking ----
    // volatile + package-private so GameController can reset it after level complete.
    volatile boolean transitioning = false;

    // ---- UI flag ----
    public boolean menuVisible = true; // prevents hiding notify loop

    // ---- Constructor -------------------------------------------------------

    public GameScreen(GameController ctrl) {
        super();
        this.controller = ctrl;

        // Fonts
        gameFont = new Font(Font.MONOSPACED, Font.PLAIN, 10);

        // Off-screen buffers
        screenBuffer = ImageUtils.createImage(132, 176);
        screenG      = screenBuffer.createGraphics();
        screenG.setFont(gameFont);

        // Load header background
        headImage = ImageUtils.loadImage("/bounce/icons/head.png");

        // Load audio
        soundUp     = loadMidi("/bounce/sounds/up.mid");
        soundPickup = loadMidi("/bounce/sounds/pickup.mid");
        soundPop    = loadMidi("/bounce/sounds/pop.mid");

        // Splash
        splashIndex = 1; // start at index 1 (bounce splash)
        try {
            splashImage = ImageUtils.loadImage(SPLASH_IMAGES[splashIndex]);
        } catch (Exception e) {
            splashImage = ImageUtils.createImage(1, 1);
        }

        // Start timer / listen for keys
        addKeyListener(this);
        startTimer();
    }

    // ---- Level management --------------------------------------------------

    /**
     * Start a new game at the given level.
     * (Originally: sub_71)
     */
    public void startLevel(int levelNum) {
        transitioning = false;   // clear any leftover guard from previous run
        level      = levelNum;
        gems       = 0;
        lives      = 3;
        score      = 0;
        nextLevel  = false;
        allGemsGot = false;
        needsClear = false;
        reloadCurrentLevel();
    }

    /**
     * Reload (or initial-load) the current level number.
     * (Originally: sub_d3)
     */
    public void reloadCurrentLevel() {
        ball = null;
        freeLevel();
        loadLevel(level);
        gems          = 0;
        levelNameTimer = 120;
        hudDirty      = true;
        spawnBall(startCol, startRow);
    }

    /** Backing reference to the ball. */
    public Ball ball; // was var_1f7

    /**
     * Spawn/respawn the ball at tile (col, row).
     * (Originally: sub_11a)
     */
    public void spawnBall(int col, int row) {
        System.gc();
        int halfSize = (ballSizeIndex == 1) ? 8 : 6;

        // Set scroll/camera based on ball start position
        if (col * 12 + 6 < 64) {
            autoScroll    = false;
            cameraX       = 64;
            leadingEdge   = 132;
            trailingEdge  = 0;
            scrollLeft    = 0;
        } else if (col * 12 + 6 > tileCols * 12 - 64) {
            autoScroll   = false;
            cameraX      = 92;
            leadingEdge  = 156;
            trailingEdge = 28;
            scrollLeft   = tileCols - 13;
        } else {
            autoScroll   = true;
            cameraX      = 0;
            leadingEdge  = 143;
            trailingEdge = 15;
            scrollLeft   = col - 6;
        }

        wrapBoundary   = 156;
        scrollTop      = row / 7 * 7;
        scrollRight    = scrollLeft + 13;
        scrollTopRight = scrollTop;

        ball = new Ball(
            (col - scrollLeft) * 12 + halfSize,
            (row - scrollTop)  * 12 + halfSize,
            ballSizeIndex,
            this
        );
        ball.setTeleportDestination(col, row);

        // If large ball, nudge into free space
        if (ballSizeIndex == 1 && !ball.checkCollision(ball.x, ball.y)) {
            int nudge = 4;
            if      (ball.checkCollision(ball.x - nudge, ball.y)) ball.x -= nudge;
            else if (ball.checkCollision(ball.x, ball.y - nudge)) ball.y -= nudge;
            else if (ball.checkCollision(ball.x - nudge, ball.y - nudge)) { ball.x -= nudge; ball.y -= nudge; }
        }
        redrawAllTiles();
    }

    // ---- Score / HUD -------------------------------------------------------

    @Override
    public void addScore(int points) {
        score    += points;
        hudDirty  = true;
    }

    /** Save high-score record and notify controller. (Originally: sub_16e) */
    public void checkHighScore() {
        if (level > controller.highestLevel) {
            controller.highestLevel = level;
        }
        if (score > controller.highScore) {
            controller.highScore = score;
            controller.newRecord = true;
        }
        controller.lastScore = score;
        // Always write both records atomically
        controller.saveAll();
    }

    /**
     * Save progress at the end of every level (even without a new high score).
     * Uses one atomic write for both records — mirrors original sub_1e3 behavior.
     */
    public void saveProgress() {
        if (level >= controller.highestLevel) {
            controller.highestLevel = level;
        }
        if (score >= controller.highScore) {
            controller.highScore = score;
        }
        controller.lastScore = score;
        controller.saveAll();
    }

    // ---- Screen wrapping (vertical) ----------------------------------------

    /**
     * Adjust scrollTop/ball.y when ball exits vertical bounds.
     * (Originally: sub_189)
     */
    private void handleVerticalWrap() {
        int ballCol    = ball.x / 12;
        int ballOffset = ball.x - ballCol * 12 - 6;

        redrawDirtyTiles(false);

        if (ball.y < 0) {
            scrollTop -= 7; scrollTopRight -= 7;
            ball.y += 84;
        } else if (ball.y > 96) {
            scrollTop += 7; scrollTopRight += 7;
            ball.y -= 84;
        }

        // Auto-scroll / wrap reset
        if (!autoScroll && scrollLeft - (13 - wrapBoundary / 12) == 0) {
            if (wrapBoundary < ball.x) ball.x -= wrapBoundary;
            else                       ball.x  = ball.x - wrapBoundary + 156;
            scrollLeft    = 0;
            trailingEdge  = 0;
            leadingEdge   = 132;
            cameraX       = 64;
        } else if (!autoScroll) {
            scrollLeft    = tileCols - 13;
            trailingEdge  = 28;
            leadingEdge   = 156;
            if (ball.x > wrapBoundary) ball.x = 156 - (wrapBoundary + 156 - ball.x);
            else                        ball.x = 156 - (wrapBoundary - ball.x);
            cameraX = 92;
        } else {
            if (ball.x > wrapBoundary) scrollLeft = scrollLeft - 13 + ballCol - 6;
            else                        scrollLeft = scrollLeft + ballCol - 6;

            if (scrollLeft < 0) {
                ballOffset += scrollLeft * 12; scrollLeft = 0;
            } else if (scrollLeft > tileCols - 13 - 1) {
                ballOffset += (scrollLeft - tileCols - 13 - 1) * 12;
                scrollLeft  = tileCols - 13 - 1;
            }
            trailingEdge = 14 + ballOffset;
            leadingEdge  = 142 + ballOffset;
            ball.x       = 78 + ballOffset;
        }

        scrollRight    = scrollLeft + 13;
        wrapBoundary   = 156;
        redrawAllTiles();
    }

    // ---- Rendering ---------------------------------------------------------

    /**
     * Build the full frame into screenBuffer.
     * (Originally: sub_227)
     */
    public void drawScene() {
        if (screenG == null) screenG = screenBuffer.createGraphics();

        // Draw header
        if (headImage != null) {
            ImageUtils.drawImage(screenG, headImage, 0, 0, 0);
        }

        // Draw ball
        if (ball != null) {
            ball.draw(levelGraphics != null ? levelGraphics : screenBuffer.createGraphics());
        }

        // Draw deferred overlays
        while (!deferredImages.isEmpty()) {
            BufferedImage img = deferredImages.remove(0);
            int dx = deferredX.remove(0);
            int dy = deferredY.remove(0);
            if (levelGraphics != null) {
                ImageUtils.drawImage(levelGraphics, img, dx, dy, 20);
            }
        }

        // Blit level buffer to screen at y=48.
        //
        // The levelBuffer is a 156px-wide ring buffer (13 tiles x 12px).
        // trailingEdge = the pixel column in levelBuffer that maps to screen x=0.
        // Drawing at x=-trailingEdge aligns that column with the left edge of the screen.
        //
        // When the 132px viewport overflows the 156px ring (trailingEdge + 132 > 156),
        // a second copy is needed, offset by +156, to fill the remainder.
        // Equivalently: wrap is needed when leadingEdge has cycled past 0 (leadingEdge < trailingEdge).
        if (levelBuffer != null) {
            if (trailingEdge + SCREEN_W <= LEVEL_W_PX) {
                // Normal: viewport sits fully within the ring buffer
                ImageUtils.drawImage(screenG, levelBuffer, -trailingEdge, 48, 20);
            } else {
                // Wrap: viewport crosses the 0/156 ring boundary
                ImageUtils.drawImage(screenG, levelBuffer, -trailingEdge,              48, 20);
                ImageUtils.drawImage(screenG, levelBuffer, LEVEL_W_PX - trailingEdge,  48, 20);
            }
        }

        // Level name banner
        if (levelNameTimer != 0) {
            screenG.setColor(ImageUtils.toColor(GameConstants.COLOR_SCORE));
            screenG.setFont(gameFont);
            ImageUtils.drawString(screenG, levelName, 44, 132, ImageUtils.ANCHOR_TOP | ImageUtils.ANCHOR_LEFT);
        }

        // HUD (status bar at y=144)
        if (hudDirty) {
            screenG.setColor(ImageUtils.toColor(GameConstants.COLOR_STATUS_BG));
            screenG.fillRect(0, 144, 132, 32);

            // Draw lives (heart sprites)
            for (int i = 0; i < lives; i++) {
                ImageUtils.drawImage(screenG, lifeSprite,
                    5 + i * (lifeSprite != null ? lifeSprite.getWidth() - 1 : 11), 147, 20);
            }

            // Draw gem indicators
            int gemsLeft = Math.min(totalGems - gems, 11);
            for (int i = 0; i < gemsLeft; i++) {
                ImageUtils.drawImage(screenG, gemSprite,
                    5 + i * (gemSprite != null ? gemSprite.getWidth() - 1 : 11), 160, 20);
            }

            // Arrow
            ImageUtils.drawImage(screenG, arrowSprite, 124, 169, 0);

            // Score
            screenG.setColor(ImageUtils.toColor(GameConstants.COLOR_SCORE));
            ImageUtils.drawString(screenG, formatScore(score), 68, 148, 20);

            hudDirty = false;
        }

        // Power bar (side indicator)
        if (powerBarValue != 0) {
            screenG.setColor(ImageUtils.toColor(GameConstants.COLOR_ORANGE));
            screenG.fillRect(1, 176 - 3 * powerBarValue / 30, 5, 176);
        }
    }

    // ---- Swing painting ----------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (splashIndex != -1) {
            // Show splash screen
            if (splashImage != null) {
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                int cx = getWidth()  >> 1;
                int cy = getHeight() >> 1;
                ImageUtils.drawImage(g2, splashImage, cx, cy, ImageUtils.ANCHOR_HCENTER | ImageUtils.ANCHOR_VCENTER);
            }
        } else if (needsClear) {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            needsClear = false;
        } else {
            // Scale screen buffer to panel size
            int sw = getWidth(), sh = getHeight();
            if (sw != SCREEN_W || sh != SCREEN_H) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.drawImage(screenBuffer, 0, 0, sw, sh, null);
            } else {
                g2.drawImage(screenBuffer, 0, 0, null);
            }
        }
    }

    // ---- Game loop (tick) --------------------------------------------------

    @Override
    protected void onTick() {
        if (levelLoaded) {
            reloadCurrentLevel();
            repaintSafe();
            return;
        }

        // ---- Splash screen ----
        if (splashIndex != -1) {
            if (splashImage != null) {
                if (splashHoldFrames > 30) {
                    splashImage = null;
                    System.gc();
                    switch (splashIndex) {
                        case 0:
                            splashIndex = 1;
                            splashImage = ImageUtils.loadImage(SPLASH_IMAGES[splashIndex]);
                            break;
                        case 1:
                            splashIndex = -1;
                            menuVisible = false;
                            SwingUtilities.invokeLater(() -> controller.showMenu());
                            break;
                    }
                    splashHoldFrames = 0;
                } else {
                    splashHoldFrames++;
                }
            } else {
                menuVisible = false;
                SwingUtilities.invokeLater(() -> controller.showMenu());
            }
            repaintSafe();
            return;
        }

        // ---- In-game ----
        if (levelNameTimer > 0) levelNameTimer--;

        if (ball != null && ball.y >= 0 && ball.y <= 96) {
            redrawDirtyTiles(true);
            ball.update();
            checkAutoScrollStop(ball.x, ball.deltaX);
        } else if (ball != null) {
            handleVerticalWrap();
        }

        // Ball respawn after death
        if (ball != null && ball.onGroundDir == 1) {
            if (lives < 0) {
                if (!transitioning) {
                    transitioning = true;
                    checkHighScore();
                    stopTimer();
                    SwingUtilities.invokeLater(() -> controller.showGameOver());
                }
                return;
            }
            spawnBall(ball.teleportCol, ball.teleportRow);
        }

        // Moving objects
        if (movingObjectCount > 0) updateMovingObjects();

        // Check all gems collected
        if (gems >= totalGems && totalGems > 0) allGemsGot = true;

        // Portal animation: once allGemsGot, animate every frame until fully open.
        // Do NOT gate this on viewport visibility — the original bug was that the
        // portal would only animate when the tile was visible on screen.
        if (allGemsGot && !exitOpen) {
            animatePortal();
            // Mark portal tile dirty so it gets redrawn immediately
            if (portalRow >= 0 && portalRow < tileRows &&
                portalCol >= 0 && portalCol < tileCols) {
                tiles[portalRow][portalCol] |= 128;
            }
        }

        // Check level exit reached (ball enters open portal)
        if (allGemsGot && exitOpen && levelWidth != -1) {
            int v1 = leadingEdge, v2 = trailingEdge;
            if (levelWidth <= wrapBoundary) {
                if (leadingEdge  > wrapBoundary) v1 = leadingEdge  - 156;
                if (trailingEdge > wrapBoundary) v2 = trailingEdge - 156;
            } else {
                if (leadingEdge  < wrapBoundary) v1 = leadingEdge  + 156;
                if (trailingEdge < wrapBoundary) v2 = trailingEdge + 156;
            }
            if (levelWidth >= v1 && levelWidth <= v2) {
                levelWidth = -1;
                allGemsGot = false;
            }
        }

        // Power bar
        powerBarValue = 0;
        if (ball != null) {
            if (ball.waveTimer    > powerBarValue) powerBarValue = ball.waveTimer;
            if (ball.reverseTimer > powerBarValue) powerBarValue = ball.reverseTimer;
            if (ball.stickyTimer  > powerBarValue) powerBarValue = ball.stickyTimer;
            if (powerBarValue % 30 == 0 || powerBarValue == 1) hudDirty = true;
        }

        updateScroll(ball != null ? ball.x : 0, ball != null ? ball.deltaX : 0, 16);
        drawScene();
        repaintSafe();

        // Level complete / next-level transition
        if (nextLevelFlag && !transitioning) {
            transitioning = true;
            nextLevelFlag = false;
            allGemsGot    = false;
            level++;
            addScore(5000);
            saveProgress();       // always save on level complete
            checkHighScore();     // also check for new high score record
            stopTimer();
            if (level >= 11) {
                SwingUtilities.invokeLater(() -> controller.showGameOver());
            } else {
                SwingUtilities.invokeLater(() -> controller.showLevelComplete());
                needsClear = true;
                repaintSafe();
            }
        }
    }

    /** Thread-safe repaint. */
    private void repaintSafe() {
        SwingUtilities.invokeLater(this::repaint);
    }

    // ---- Key handling -------------------------------------------------------

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Splash skip
        if (splashIndex != -1) {
            splashHoldFrames = 31;
            return;
        }
        if (ball == null) return;

        // Menu trigger
        if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_F1) {
            menuVisible = false;
            stopTimer();
            SwingUtilities.invokeLater(() -> controller.showMenu());
            return;
        }

        // Cheat code: 7,7,8,9,9 → dev mode; etc.
        handleCheat(code);

        // Level skip (dev mode only: Page Up / Page Down or numpad 1/3)
        if (cheatDevMode) {
            if (code == KeyEvent.VK_PAGE_UP || code == KeyEvent.VK_NUMPAD1) {
                level = Math.max(1, level - 1);
                levelLoaded = true;
                return;
            } else if (code == KeyEvent.VK_PAGE_DOWN || code == KeyEvent.VK_NUMPAD3) {
                level = Math.min(10, level + 1);
                levelLoaded = true;
                return;
            }
        }

        // Directional input
        switch (getGameAction(code)) {
            case 1: ball.keyPressed(8); break;  // UP → jump
            case 2: ball.keyPressed(1); break;  // LEFT
            case 5: ball.keyPressed(2); break;  // RIGHT
            case 6: ball.keyPressed(4); break;  // DOWN
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (ball == null) return;
        switch (getGameAction(e.getKeyCode())) {
            case 1: ball.keyReleased(8); break;
            case 2: ball.keyReleased(1); break;
            case 5: ball.keyReleased(2); break;
            case 6: ball.keyReleased(4); break;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    /**
     * Map Java KeyEvent code → game action (1=UP, 2=LEFT, 5=RIGHT, 6=DOWN).
     * Also supports WASD and numpad.
     */
    private int getGameAction(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:    case KeyEvent.VK_W: case KeyEvent.VK_NUMPAD8: return 1; // UP
            case KeyEvent.VK_LEFT:  case KeyEvent.VK_A: case KeyEvent.VK_NUMPAD4: return 2; // LEFT
            case KeyEvent.VK_DOWN:  case KeyEvent.VK_S: case KeyEvent.VK_NUMPAD2: return 6; // DOWN
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: case KeyEvent.VK_NUMPAD6: return 5; // RIGHT
            case KeyEvent.VK_SPACE: case KeyEvent.VK_ENTER:                        return 8; // FIRE
            default: return 0;
        }
    }

    /**
     * Handle cheat code sequence.
     * Sequence: numpad 7,7,8,9,9 → enable dev (level-skip) mode.
     * Sequence: 7,8,9,9,9        → enable god mode.
     */
    private void handleCheat(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_NUMPAD7: case KeyEvent.VK_7:
                if (cheatStep != 0 && cheatStep != 2) cheatStep = 0; else cheatStep++; break;
            case KeyEvent.VK_NUMPAD8: case KeyEvent.VK_8:
                if (cheatStep != 1 && cheatStep != 3) { if (cheatStep == 5) { playSoundUp(); godMode = true; cheatStep = 0; } else cheatStep = 0; } else cheatStep++; break;
            case KeyEvent.VK_NUMPAD9: case KeyEvent.VK_9:
                if (cheatStep == 4) { cheatStep++; } else if (cheatStep == 5) { playSoundPop(); cheatDevMode = true; cheatStep = 0; } else cheatStep = 0; break;
            default: cheatStep = 0; break;
        }
    }

    // ---- Audio -------------------------------------------------------------

    /** Load a MIDI file from classpath resources. (Originally: sub_2da) */
    private Sequencer loadMidi(String path) {
        try {
            InputStream in = getClass().getResourceAsStream(path);
            if (in == null) return null;
            Sequence  seq = MidiSystem.getSequence(in);
            Sequencer seq2 = MidiSystem.getSequencer();
            seq2.open();
            seq2.setSequence(seq);
            return seq2;
        } catch (Exception e) {
            System.err.println("Can't load sound: " + path);
            return null;
        }
    }

    private void playSound(Sequencer s) {
        if (s == null) return;
        try { if (s.isRunning()) s.stop(); s.setTickPosition(0); s.start(); }
        catch (Exception ignored) {}
    }

    @Override public void playSoundUp()     { playSound(soundUp); }
    @Override public void playSoundPickup() { playSound(soundPickup); }
    @Override public void playSoundPop()    { playSound(soundPop); }
    @Override public void vibrate(int ms)   { /* desktop: no vibration */ }

    // ---- Cleanup ------------------------------------------------------------

    public void dispose() {
        stopTimer();
        if (soundUp     != null) { soundUp.close();     soundUp     = null; }
        if (soundPickup != null) { soundPickup.close();  soundPickup = null; }
        if (soundPop    != null) { soundPop.close();     soundPop    = null; }
    }
}
