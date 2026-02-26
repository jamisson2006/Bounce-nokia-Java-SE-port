package bounce;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/**
 * Game controller — menus, save/load, screen transitions.
 * Originally: Class_1bb (J2ME, uses RecordStore "bounceRMS").
 *
 * ── Save format (binary, mirrors original RecordStore exactly) ──────────────
 *
 * File: <user.home>/.bounce/bounceRMS.dat
 *
 * Offset  Size  Content
 * ------  ----  ---------------------------------------------------
 *   0       1   highestLevel  (1 byte, 0 = no progress)
 *   1       8   highScore BCD (each byte = one decimal digit 0-9,
 *                              byte[0]=ten-millions … byte[7]=units)
 *
 * Total: 9 bytes — compatible with original Nokia J2ME RecordStore.
 *
 * ── Resume logic (mirrors original commandAction) ───────────────────────────
 *
 * "Resume" is shown when:   gameMode == 1  OR  highestLevel > 1
 *   • gameMode == 1, startNew=false → just restart the timer (game already loaded)
 *   • gameMode == 3 (game over)     → startNew=true, lvl = gameScreen.level
 *   • gameMode == 2 (fresh launch)  → startNew=true, lvl = highestLevel
 */
public class GameController {

    // ---- Save file location (replaces J2ME RecordStore "bounceRMS") --------
    private static final String SAVE_DIR  = System.getProperty("user.home") + File.separator + ".bounce";
    private static final String SAVE_FILE = SAVE_DIR + File.separator + "bounceRMS.dat";

    /** Record index for level slot (mirrors sub_1e3 slot 1). */
    public static final int SLOT_LEVEL = 1;
    /** Record index for score slot (mirrors sub_1e3 slot 2). */
    public static final int SLOT_SCORE = 2;

    // ---- Persistent data (mirrors Class_1bb fields) ------------------------
    /** Highest level ever reached. 0 = no save; 1+ = progress. (was var_39a) */
    public int     highestLevel = 0;
    /** All-time high score. (was var_3f2) */
    public int     highScore    = 0;
    /** Score from the last completed game. (was var_448) */
    public int     lastScore    = 0;
    /** True if last game set a new high score. (was var_408) */
    public boolean newRecord    = false;

    // ---- Game mode (was var_37d) -------------------------------------------
    // 1 = in-game, 2 = menu (no game loaded), 3 = game-over, 4 = new-game
    public int gameMode = 2;

    // ---- Menu item IDs (order = display order) ------------------------------
    private static final int ITEM_RESUME     = 0;
    private static final int ITEM_NEW_GAME   = 1;
    private static final int ITEM_HIGH_SCORE = 2;
    private static final int ITEM_SETTINGS   = 3;
    private static final int ITEM_ABOUT      = 4;
    private static final int ITEM_EXIT       = 5;

    private static final String[] ITEM_LABELS = {
        "Resume", "New Game", "High Score", "Settings", "About", "Exit"
    };

    // ---- Swing components ---------------------------------------------------
    private JFrame frame;
    public  GameScreen gameScreen;
    private int menuSelectedIndex = 0;

    // ---- Constructor --------------------------------------------------------

    public GameController(BounceMain app) {
        loadData();   // always load before building window
        buildWindow();
    }

    // ---- Window setup -------------------------------------------------------

    private void buildWindow() {
        frame = new JFrame("Bounce (Nokia 2005) \u2013 Java SE Port");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { exitGame(); }
        });

        gameScreen = new GameScreen(this);
        gameScreen.setPreferredSize(
            new Dimension(GameCanvas.SCREEN_W * 3, GameCanvas.SCREEN_H * 3));
        frame.setContentPane(gameScreen);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        gameScreen.requestFocusInWindow();
    }

    // ---- Menu ---------------------------------------------------------------

    /**
     * Build and show the game menu.
     *
     * "Resume" visibility mirrors original (Class_1bb line 96):
     *   show if (gameMode == 1) OR (highestLevel > 1)
     *   i.e., either we are mid-game, OR we have a save worth resuming.
     */
    public void showMenu() {
        // Mirror original: var_37d == 1 || var_39a > 1
        boolean showResume = (gameMode == 1 || highestLevel > 1);

        java.util.List<String>  labels = new java.util.ArrayList<>();
        java.util.List<Integer> ids    = new java.util.ArrayList<>();

        if (showResume) { labels.add(ITEM_LABELS[ITEM_RESUME]);     ids.add(ITEM_RESUME);     }
        labels.add(ITEM_LABELS[ITEM_NEW_GAME]);   ids.add(ITEM_NEW_GAME);
        labels.add(ITEM_LABELS[ITEM_HIGH_SCORE]); ids.add(ITEM_HIGH_SCORE);
        labels.add(ITEM_LABELS[ITEM_SETTINGS]);   ids.add(ITEM_SETTINGS);
        labels.add(ITEM_LABELS[ITEM_ABOUT]);      ids.add(ITEM_ABOUT);
        labels.add(ITEM_LABELS[ITEM_EXIT]);        ids.add(ITEM_EXIT);

        String[] labelArr = labels.toArray(new String[0]);
        int[]    idArr    = ids.stream().mapToInt(Integer::intValue).toArray();
        if (menuSelectedIndex >= labelArr.length) menuSelectedIndex = 0;

        // ---- Build dialog ---------------------------------------------------
        JDialog dialog = new JDialog(frame, "Bounce", true);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getContentPane().setBackground(Color.BLACK);

        // Title + saved-progress hint
        String titleText = "\u2605  BOUNCE  \u2605";
        if (highestLevel > 1) {
            titleText += "    [Lv." + highestLevel + "  Hi:" + formatScore(highScore) + "]";
        }
        JLabel title = new JLabel(titleText, JLabel.CENTER);
        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        title.setForeground(new Color(255, 160, 0));
        title.setOpaque(true);
        title.setBackground(Color.BLACK);
        title.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        dialog.add(title, BorderLayout.NORTH);

        JList<String> list = new JList<>(labelArr);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(menuSelectedIndex);
        list.setBackground(Color.BLACK);
        list.setForeground(Color.WHITE);
        list.setSelectionBackground(new Color(0, 110, 0));
        list.setSelectionForeground(Color.WHITE);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        list.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.BLACK);
        dialog.add(scroll, BorderLayout.CENTER);

        JButton ok = new JButton("  OK  ");
        ok.setBackground(new Color(0, 70, 0));
        ok.setForeground(Color.WHITE);
        ok.setFocusPainted(false);
        ok.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));

        ok.addActionListener(e -> {
            int sel = list.getSelectedIndex();
            if (sel >= 0) {
                menuSelectedIndex = sel;
                dialog.dispose();
                handleMenuAction(idArr[sel]);
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) ok.doClick();
            }
        });

        list.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)  ok.doClick();
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dialog.dispose();
                    if (gameMode == 1) resumeGame(false, 0);
                }
            }
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        south.setBackground(Color.BLACK);
        south.add(ok);
        dialog.add(south, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setMinimumSize(new Dimension(260, 300));
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    /**
     * Handle a menu action by item ID.
     *
     * Resume logic mirrors original commandAction exactly:
     *   gameMode == 1 → sub_d0(false, 0)          just restart timer, no reload
     *   gameMode == 3 → sub_d0(true, currentLevel) restart at level where died
     *   otherwise     → sub_d0(true, highestLevel) start at saved highest level
     */
    private void handleMenuAction(int itemId) {
        switch (itemId) {

            case ITEM_RESUME: {
                boolean startNew = (gameMode != 1);  // mirrors: var4 = var_25a(false)
                int lvl = 0;
                if (startNew) {
                    // gameMode==3: var5 = gameScreen.var_412 (current level)
                    // otherwise:   var5 = var_39a (highestLevel saved)
                    lvl = (gameMode == 3)
                        ? Math.max(1, gameScreen.level)
                        : Math.max(1, highestLevel);
                }
                resumeGame(startNew, lvl);
                break;
            }

            case ITEM_NEW_GAME:
                gameMode  = 4;
                newRecord = false;
                resumeGame(true, 1);
                break;

            case ITEM_HIGH_SCORE:
                showHighScore();
                SwingUtilities.invokeLater(this::showMenu);
                break;

            case ITEM_SETTINGS:
                showSettings();
                SwingUtilities.invokeLater(this::showMenu);
                break;

            case ITEM_ABOUT:
                showAbout();
                SwingUtilities.invokeLater(this::showMenu);
                break;

            case ITEM_EXIT:
                exitGame();
                break;
        }
    }

    // ---- Screen transitions -------------------------------------------------

    /** Return to the game canvas, optionally loading a level. (was sub_d0) */
    public void resumeGame(boolean startNew, int lvl) {
        showCanvas();
        if (startNew) {
            gameScreen.transitioning = false;
            gameScreen.startLevel(Math.max(1, lvl));
        }
        gameScreen.startTimer();
        gameMode = 1;
    }

    private void showCanvas() {
        frame.setContentPane(gameScreen);
        frame.revalidate();
        gameScreen.requestFocusInWindow();
    }

    /** Show game-over dialog then menu. (was sub_158) */
    public void showGameOver() {
        if (gameMode == 3) return;
        gameMode = 3;

        String msg = "<html><center>"
            + "<b>GAME OVER</b><br><br>"
            + (newRecord ? "<font color='orange'>\u2605 NEW HIGH SCORE! \u2605</font><br><br>" : "")
            + "Score: <b>" + lastScore + "</b><br>"
            + "Level: " + gameScreen.level
            + "</center></html>";

        JOptionPane.showMessageDialog(frame, msg, "Game Over",
            JOptionPane.INFORMATION_MESSAGE);

        SwingUtilities.invokeLater(this::showMenu);
    }

    /** Show level-complete dialog then load next level. (was sub_17d) */
    public void showLevelComplete() {
        String msg = "Level " + (gameScreen.level - 1) + " complete!\n\n"
            + "Score: " + gameScreen.score;
        JOptionPane.showMessageDialog(frame, msg, "Level Complete",
            JOptionPane.INFORMATION_MESSAGE);
        gameScreen.transitioning = false;
        gameScreen.reloadCurrentLevel();
        gameScreen.startTimer();
        showCanvas();
        gameScreen.requestFocusInWindow();
    }

    /** Show high-score dialog. (was sub_dd) */
    public void showHighScore() {
        JOptionPane.showMessageDialog(frame,
            "Best level : " + Math.max(1, highestLevel) + "\n"
          + "High score : " + formatScore(highScore),
            "High Score", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showSettings() {
        JOptionPane.showMessageDialog(frame,
            "Controls\n"
          + "  Arrow keys / WASD  \u2013  move & jump\n"
          + "  ESC / F1           \u2013  open menu\n"
          + "  PageUp / PageDown  \u2013  skip level (dev mode)\n\n"
          + "Cheat: numpad 7 7 8 9 9  \u2192  dev mode\n"
          + "Cheat: numpad 7 8 9 9 9  \u2192  god mode",
            "Settings", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showAbout() {
        JOptionPane.showMessageDialog(frame,
            "Game Bounce (Nokia vendor)\n"
          + "Original J2ME port by iceman345 (c) 2005\n"
          + "iceman345@narod.ru\n"
          + "\n"
          + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n"
          + "Java SE 1.8 open-source port\n"
          + "Ported by Jamisson Tavares\n"
          + "J2ME Fans\n"
          + "\n"
          + "YouTube : youtube.com/@jamissontavares7671\n"
          + "Discord : discord.gg/HuaJCVuRF",
            "About", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---- Save / Load --------------------------------------------------------

    /**
     * Load save data from ~/.bounce/bounceRMS.dat.
     *
     * Binary format mirrors original RecordStore:
     *   byte[0]     = highestLevel  (Record 1, 1 byte)
     *   byte[1..8]  = highScore BCD (Record 2, 8 bytes: each = one decimal digit)
     *
     * (Originally: sub_194)
     */
    public void loadData() {
        highestLevel = 0;
        highScore    = 0;

        File f = new File(SAVE_FILE);
        if (!f.exists()) return;

        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            // Record 1: 1 byte = highestLevel
            highestLevel = in.readUnsignedByte();   // unsigned so levels 0-255

            // Record 2: 8 BCD digits = high score
            highScore = 0;
            int[] multipliers = {10000000, 1000000, 100000, 10000, 1000, 100, 10, 1};
            for (int i = 0; i < 8; i++) {
                int digit = in.readUnsignedByte();  // 0-9 per digit
                highScore += digit * multipliers[i];
            }

        } catch (IOException e) {
            System.err.println("[Save] Load failed: " + e.getMessage());
            // Corrupt file — reset to defaults
            highestLevel = 0;
            highScore    = 0;
        }

        System.out.println("[Save] Loaded: level=" + highestLevel + " score=" + highScore);
    }

    /**
     * Write save data to ~/.bounce/bounceRMS.dat.
     *
     * Always writes BOTH records atomically in one pass to avoid partial saves.
     * Pass slot = SLOT_LEVEL or SLOT_SCORE (both are always flushed together).
     *
     * (Originally: sub_1e3)
     */
    public void saveData(int slot) {
        // Clamp highestLevel to valid byte range
        int lvlToSave = Math.max(0, Math.min(255, highestLevel));

        try {
            // Ensure save directory exists
            new File(SAVE_DIR).mkdirs();

            try (DataOutputStream out = new DataOutputStream(new FileOutputStream(SAVE_FILE))) {
                // Record 1: 1 byte = highestLevel
                out.writeByte(lvlToSave);

                // Record 2: 8 BCD digits = highScore
                // Each byte stores one decimal digit (0-9), most significant first
                int[] multipliers = {10000000, 1000000, 100000, 10000, 1000, 100, 10, 1};
                int score = Math.max(0, highScore);
                for (int i = 0; i < 8; i++) {
                    out.writeByte((score / multipliers[i]) % 10);
                }

                out.flush();
            }

            System.out.println("[Save] Saved: level=" + lvlToSave + " score=" + highScore);

        } catch (IOException e) {
            System.err.println("[Save] Write failed: " + e.getMessage());
        }
    }

    /** Save both records in one call — convenience wrapper. */
    public void saveAll() {
        saveData(SLOT_LEVEL); // internally saves both
    }

    // ---- Utilities ----------------------------------------------------------

    /** Format score as 8-digit zero-padded string (matches original HUD). */
    public static String formatScore(int score) {
        return String.format("%08d", Math.max(0, score));
    }

    // ---- Exit ---------------------------------------------------------------

    public void exitGame() {
        if (gameScreen != null) {
            gameScreen.stopTimer();
            gameScreen.dispose();
        }
        frame.dispose();
        System.exit(0);
    }

    public GameScreen getGameScreen() { return gameScreen; }
    public JFrame     getFrame()      { return frame; }
}
