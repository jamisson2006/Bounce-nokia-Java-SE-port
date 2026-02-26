package bounce;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.swing.JPanel;

/**
 * Abstract game canvas — handles tile data, sprite loading, rendering, scrolling.
 * Originally: Class_4b (obfuscated) extends J2ME Canvas.
 * Ported to Java SE 1.8 using Swing JPanel.
 */
public abstract class GameCanvas extends JPanel {

    private static final long serialVersionUID = 1L;

    // ---- Screen & viewport constants ----
    public static final int SCREEN_W    = 132;
    public static final int SCREEN_H    = 176;
    public static final int LEVEL_W_PX  = 156; // scrolling viewport width
    public static final int LEVEL_H_PX  = 96;  // visible level height
    public static final int TILE_PX     = 12;
    public static final int VISIBLE_COLS = 13;

    // ---- Scrolling state ----
    public int scrollLeft;         // leftmost visible tile column (was var_34)
    public int scrollTop;          // topmost visible tile row    (was var_8e)
    public int scrollRight;        // rightmost visible column    (was var_b3)
    public int scrollTopRight;     // var_f8
    public int wrapBoundary;       // pixels before wrap (was var_118)
    public int leadingEdge;        // leading edge px (was var_135)
    public int trailingEdge;       // trailing edge px (was var_168)
    public boolean autoScroll;     // (was var_1a9)
    public int cameraX;            // camera target x (was var_1d1)
    public int levelWidth;         // total scrolling width in px (var_221 ≈ door position)
    public int ballSizeIndex;      // 0=small 1=large (was var_521)

    // ---- Level tile data ----
    public short[][] tiles;        // [row][col] (was var_5b6)
    public int tileRows;           // (was var_62d)
    public int tileCols;           // (was var_612)
    public int movingObjectCount;  // (was var_6bd)

    // ---- Moving objects (platforms/enemies) ----
    public short[][] movObjStart;  // top-left tile [obj][0=col,1=row] (was var_6e6)
    public short[][] movObjEnd;    // bottom-right tile (was var_710)
    public short[][] movObjSpeed;  // speed vector (was var_72a)
    public short[][] movObjOffset; // pixel offset (was var_77e)
    public BufferedImage[] movObjImage;
    public Graphics2D[]    movObjGraphics;

    // ---- Sprites / images ----
    protected BufferedImage[] sprites;       // 67-slot sprite array (was var_299)
    protected BufferedImage   smallTile;     // 12×12 tile (was var_2b0)
    protected Graphics2D      smallTileG;    // Graphics for smallTile (was var_307)
    public static BufferedImage arrowSprite; // small arrow (was var_356, 8×5)
    public BufferedImage lifeSprite;         // life heart (was var_7fc)
    public BufferedImage gemSprite;          // gem dot (was var_80b)

    // ---- Level render buffer ----
    protected BufferedImage levelBuffer;     // 156×96 off-screen (was var_254)
    public    Graphics2D    levelGraphics;   // graphics for levelBuffer (was var_260)

    // ---- Level entrance/exit (portal) ----
    public int  portalCol, portalRow;        // was var_827, var_861
    public int  portalEndCol, portalEndRow;  // was var_89e, var_8cd
    public BufferedImage portalFrameImg;     // animated portal (was var_929)
    public BufferedImage portalImage;        // composite (was var_90e)
    public int  portalAnim;                  // animation counter (was var_984)
    public boolean exitOpen;                 // portal fully open (was var_9cd)

    // ---- Game state (shared with Ball/GameScreen) ----
    public int     level         = 1; // current level number; NEVER 0 (was var_412)
    public String  levelName;     // (was var_440)
    public String  levelNameFull; // (was var_457)
    public boolean levelLoaded;   // (was var_49d)
    public int     startCol, startRow; // ball start tile (was var_4a7, var_4fa)
    public int     totalGems;     // total gems in level (was var_679)
    public int     gems;          // collected gems (was var_237)
    public int     lives;         // remaining lives (was var_2ce in GameScreen)
    public boolean gameOverFlag;  // (was var_426)
    public boolean levelComplete; // (was var_44a)
    public boolean hudDirty;      // HUD needs redraw (was var_48d)
    public boolean godMode;       // debug invincibility (was var_5c6 in GameScreen)
    public boolean nextLevelFlag; // triggers next level load (was var_9cd context)

    // ---- Pending deferred-draw list (for animated tile overlays) ----
    protected ArrayList<BufferedImage> deferredImages = new ArrayList<>();
    protected ArrayList<Integer>       deferredX      = new ArrayList<>();
    protected ArrayList<Integer>       deferredY      = new ArrayList<>();

    // ---- Input (handled through KeyListener in GameScreen) ----
    protected int screenW, screenH;

    // ---- Game loop ----
    private GameTimer gameTimer;
    protected int movObjX[][];    // alias for readability

    // ---- Constructor -------------------------------------------------------

    public GameCanvas() {
        setPreferredSize(new Dimension(SCREEN_W, SCREEN_H));
        setBackground(Color.BLACK);
        setFocusable(true);

        screenW       = SCREEN_W;
        screenH       = SCREEN_H;
        autoScroll    = true;
        wrapBoundary  = 156;
        leadingEdge   = 142;
        trailingEdge  = 14;
        levelBuffer   = ImageUtils.createImage(156, 96);
        smallTile     = ImageUtils.createImage(12, 12);
        smallTileG    = smallTile.createGraphics();
        loadAllSprites();
        levelLoaded   = false;
        scrollLeft    = 0;
        scrollTop     = 0;
        levelWidth    = -1;
        scrollRight   = scrollLeft + 13;
        scrollTopRight = scrollTop;
        tiles         = null;

        deferredImages = new ArrayList<>();
        deferredX      = new ArrayList<>();
        deferredY      = new ArrayList<>();
    }

    // ---- Level loading -----------------------------------------------------

    /**
     * Load a level from /levels/J2MElvl.NNN binary resource.
     * (Originally: sub_23)
     */
    public void loadLevel(int levelNum) {
        levelLoaded = false;
        String[] params = { Integer.toString(levelNum) };
        levelName     = LocaleManager.getString(LocaleManager.STR_LEVEL_COMPLETE, params);
        levelNameFull = LocaleManager.getString(LocaleManager.STR_SCORE_LABEL, params);
        params = null;

        String suffix = (levelNum < 10) ? "00" + levelNum
                      : (levelNum < 100 ? "0"  + levelNum : "" + levelNum);
        String resourcePath = "/bounce/levels/J2MElvl." + suffix;

        InputStream raw = getClass().getResourceAsStream(resourcePath);

        // ---- Resource not found: show a clear error and load a blank stub level ----
        if (raw == null) {
            System.err.println(
                "Level resource not found: " + resourcePath + "\n" +
                "Make sure the original Nokia Bounce .jar resources are extracted\n" +
                "and the 'levels/' folder is on the classpath (e.g., inside src/main/resources)."
            );
            loadFallbackLevel(levelNum);
            return;
        }

        try (DataInputStream dis = new DataInputStream(raw)) {

            startCol      = dis.read();
            startRow      = dis.read();
            ballSizeIndex = dis.read();
            int portalC   = dis.read();
            int portalR   = dis.read();

            // Portal sprite (sprite index 12)
            setPortal(portalC, portalR, portalC + 1, portalR + 2, sprites[12]);

            totalGems         = dis.read();
            tileCols          = dis.read();
            tileRows          = dis.read();
            tiles             = new short[tileRows][tileCols];

            for (int r = 0; r < tileRows; r++) {
                for (int c = 0; c < tileCols; c++) {
                    tiles[r][c] = (short) dis.read();
                }
            }

            movingObjectCount = dis.read();
            if (movingObjectCount != 0) {
                loadMovingObjects(dis);
            }

        } catch (IOException e) {
            System.err.println("Error reading level " + levelNum + " (" + resourcePath + "): " + e.getMessage());
            loadFallbackLevel(levelNum);
        }
    }

    /**
     * Load a minimal blank level so the game doesn't crash when resources are missing.
     * Displays a message on screen; replace with real levels from the original .jar.
     */
    private void loadFallbackLevel(int levelNum) {
        System.err.println("Loading fallback (empty) level for level " + levelNum);
        startCol          = 6;
        startRow          = 4;
        ballSizeIndex     = 0;
        totalGems         = 0;
        tileCols          = 20;
        tileRows          = 9;
        movingObjectCount = 0;
        tiles             = new short[tileRows][tileCols];

        // Solid floor and ceiling
        for (int c = 0; c < tileCols; c++) {
            tiles[0][c]            = 1;
            tiles[tileRows - 1][c] = 1;
        }
        // Solid walls
        for (int r = 0; r < tileRows; r++) {
            tiles[r][0]            = 1;
            tiles[r][tileCols - 1] = 1;
        }

        setPortal(tileCols - 3, 1, tileCols - 2, 3,
                  sprites != null ? sprites[12] : null);
        levelName     = "Level " + levelNum + " (resources missing — see README)";
        levelNameFull = levelName;
    }

    /** Load moving platform/enemy data. (Originally: sub_97) */
    protected void loadMovingObjects(DataInputStream dis) {
        movObjStart   = new short[movingObjectCount][2];
        movObjEnd     = new short[movingObjectCount][2];
        movObjSpeed   = new short[movingObjectCount][2];
        movObjOffset  = new short[movingObjectCount][2];
        movObjImage   = new BufferedImage[movingObjectCount];
        movObjGraphics = new Graphics2D[movingObjectCount];
        movObjX       = new int[movingObjectCount][2]; // alias

        try {
            for (int i = 0; i < movingObjectCount; i++) {
                movObjStart[i][0]  = (short) dis.read();
                movObjStart[i][1]  = (short) dis.read();
                movObjEnd[i][0]    = (short) dis.read();
                movObjEnd[i][1]    = (short) dis.read();
                movObjSpeed[i][0]  = (short) dis.read();
                movObjSpeed[i][1]  = (short) dis.read();
                movObjOffset[i][0] = (short) dis.read();
                movObjOffset[i][1] = (short) dis.read();
                movObjX[i][0]      = movObjStart[i][0];
                movObjX[i][1]      = movObjStart[i][1];
            }
        } catch (Exception e) {
            System.err.println("Error loading moving objects: " + e);
        }

        // Build the moving-object sprite (4-quadrant mirror of sprite 46)
        BufferedImage base = sprites[46];
        BufferedImage img  = ImageUtils.createImage(24, 24);
        Graphics2D g = img.createGraphics();
        ImageUtils.drawImage(g, base,                                0,  0, ImageUtils.ANCHOR_TOP | ImageUtils.ANCHOR_LEFT);
        ImageUtils.drawImage(g, ImageUtils.applyTransform(base, ImageUtils.TRANS_MIRROR), 12, 0, ImageUtils.ANCHOR_TOP | ImageUtils.ANCHOR_LEFT);
        ImageUtils.drawImage(g, ImageUtils.applyTransform(base, ImageUtils.TRANS_MIRROR_ROT180), 12, 12, ImageUtils.ANCHOR_TOP | ImageUtils.ANCHOR_LEFT);
        ImageUtils.drawImage(g, ImageUtils.applyTransform(base, ImageUtils.TRANS_MIRROR_ROT180), 0, 12, ImageUtils.ANCHOR_TOP | ImageUtils.ANCHOR_LEFT);
        g.dispose();
        // This combined image is the moving-object sprite (originally stored in var_7e1)
        for (int i = 0; i < movingObjectCount; i++) {
            movObjImage[i]    = img;
            movObjGraphics[i] = null;
        }
    }

    /** Free level resources. (Originally: sub_c3) */
    protected void freeLevel() {
        if (movObjImage != null) {
            for (int i = 0; i < movingObjectCount; i++) {
                movObjImage[i] = null;
            }
        }
        movObjImage    = null;
        movObjGraphics = null;
        tiles          = null;
        System.gc();
    }

    /** Update moving object positions. (Originally: sub_ed) */
    public void updateMovingObjects() {
        for (int i = 0; i < movingObjectCount; i++) {
            short prevOffX = movObjOffset[i][0];
            short prevOffY = movObjOffset[i][1];

            movObjOffset[i][0] += movObjSpeed[i][0];
            int maxX = (movObjEnd[i][0] - movObjStart[i][0] - 2) * 12;
            int maxY = (movObjEnd[i][1] - movObjStart[i][1] - 2) * 12;

            if (movObjOffset[i][0] < 0)    movObjOffset[i][0] = 0;
            else if (movObjOffset[i][0] > maxX) movObjOffset[i][0] = (short) maxX;
            if (movObjOffset[i][0] == 0 || movObjOffset[i][0] == maxX)
                movObjSpeed[i][0] = (short)(-movObjSpeed[i][0]);

            movObjOffset[i][1] += movObjSpeed[i][1];
            if (movObjOffset[i][1] < 0)    movObjOffset[i][1] = 0;
            else if (movObjOffset[i][1] > maxY) movObjOffset[i][1] = (short) maxY;
            if (movObjOffset[i][1] == 0 || movObjOffset[i][1] == maxY)
                movObjSpeed[i][1] = (short)(-movObjSpeed[i][1] );

            // Mark dirty tiles
            short ox = movObjOffset[i][0]; if (ox < prevOffX) { short t = ox; ox = prevOffX; prevOffX = t; }
            short oy = movObjOffset[i][1]; if (oy < prevOffY) { short t = oy; oy = prevOffY; prevOffY = t; }

            int cx1 = prevOffX / 12, cy1 = prevOffY / 12;
            int cx2 = (ox + 23) / 12 + 1, cy2 = (oy + 23) / 12 + 1;
            for (int c = cx1; c < cx2; c++) {
                for (int r = cy1; r < cy2; r++) {
                    int tr = movObjStart[i][1] + r;
                    int tc = movObjStart[i][0] + c;
                    if (tr >= 0 && tr < tileRows && tc >= 0 && tc < tileCols)
                        tiles[tr][tc] |= 128;
                }
            }
        }
    }

    /** Find the moving-object index that covers tile (col, row). (sub_126) */
    public int findMovingObject(int col, int row) {
        for (int i = 0; i < movingObjectCount; i++) {
            if (movObjStart[i][0] <= col && movObjEnd[i][0] > col &&
                movObjStart[i][1] <= row && movObjEnd[i][1] > row) return i;
        }
        return -1;
    }

    // ---- Tile rendering ----------------------------------------------------

    /** Draw a single tile onto levelBuffer. (Originally: sub_178) */
    public void drawTile(int col, int row, int px, int py) {
        if (levelGraphics == null) {
            levelGraphics = levelBuffer.createGraphics();
        }
        if ((tiles[row][col] & 128) != 0) {
            tiles[row][col] = (short)(tiles[row][col] & 0xFF7F);
        }
        int tileVal  = tiles[row][col];
        boolean dark = (tileVal & 64) != 0;
        if (dark) tileVal &= ~64;

        levelGraphics.setColor(dark ? ImageUtils.toColor(GameConstants.COLOR_TILE_DARK)
                                    : ImageUtils.toColor(GameConstants.COLOR_TILE_LIGHT));

        switch (tileVal) {
            case 0:  levelGraphics.fillRect(px, py, 12, 12); break;
            case 1:  ImageUtils.drawImage(levelGraphics, sprites[0],  px, py, 20); break;
            case 2:  ImageUtils.drawImage(levelGraphics, sprites[1],  px, py, 20); break;
            case 3:  ImageUtils.drawImage(levelGraphics, dark ? sprites[6] : sprites[2], px, py, 20); break;
            case 4:  ImageUtils.drawImage(levelGraphics, dark ? sprites[9] : sprites[5], px, py, 20); break;
            case 5:  ImageUtils.drawImage(levelGraphics, dark ? sprites[7] : sprites[3], px, py, 20); break;
            case 6:  ImageUtils.drawImage(levelGraphics, dark ? sprites[8] : sprites[4], px, py, 20); break;
            case 7:  ImageUtils.drawImage(levelGraphics, sprites[10], px, py, 20); break;
            case 8:  ImageUtils.drawImage(levelGraphics, sprites[11], px, py, 20); break;
            case 9: {
                int ox = (col - portalCol) * 12;
                int oy = (row - portalRow) * 12;
                ImageUtils.drawImage(levelGraphics, portalImage, px - ox, py - oy, 20);
                levelWidth = px - ox + 12 - 1;
                break;
            }
            case 10: {
                int idx = findMovingObject(col, row);
                if (idx != -1) {
                    int offX = (col - movObjStart[idx][0]) * 12;
                    int offY = (row - movObjStart[idx][1]) * 12;
                    int relOx = movObjOffset[idx][0] - offX;
                    int relOy = movObjOffset[idx][1] - offY;
                    if ((relOx <= -36 || relOx >= 12) && (relOy <= -36 || relOy >= 12)) {
                        levelGraphics.setColor(ImageUtils.toColor(GameConstants.COLOR_TILE_LIGHT));
                        levelGraphics.fillRect(px, py, 12, 12);
                    } else {
                        smallTileG.setColor(ImageUtils.toColor(GameConstants.COLOR_TILE_LIGHT));
                        smallTileG.fillRect(0, 0, 12, 12);
                        ImageUtils.drawImage(smallTileG, movObjImage[idx], relOx, relOy, 20);
                        ImageUtils.drawImage(levelGraphics, smallTile, px, py, 20);
                    }
                }
                break;
            }
            // Grow items (13–28) and special tiles (29–54): simplified to sprite lookup
            default:
                drawSpecialTile(tileVal, dark, px, py);
                break;
        }
    }

    /** Draw special (non-wall) tiles. */
    private void drawSpecialTile(int tileVal, boolean dark, int px, int py) {
        // Pairs of [sprite_idx_main, sprite_idx_overlay] for tile types 13–28
        int[][] pairMap = {
            null, null, null, null, null, null, null, null, null, null, null, null, // 0–11 (handled above)
            null,                            // 12
            {35, 33}, {36, 34},              // 13, 14
            {17, 18}, {19, 20},              // 15, 16
            {43, 41}, {44, 42},              // 17, 18
            {25, 26}, {27, 28},              // 19, 20
            {31, 29}, {32, 30},              // 21, 22
            {13, 14}, {15, 16},              // 23, 24
            {39, 37}, {40, 38},              // 25, 26
            {21, 22}, {23, 24},              // 27, 28
        };
        if (tileVal >= 13 && tileVal <= 28) {
            if (pairMap[tileVal] != null) {
                levelGraphics.fillRect(px, py, 12, 12);
                ImageUtils.drawImage(levelGraphics, sprites[pairMap[tileVal][0]], px, py, 20);
                addDeferred(sprites[pairMap[tileVal][1]], px, py);
                return;
            }
        }
        switch (tileVal) {
            case 29: ImageUtils.drawImage(levelGraphics, sprites[45], px, py, 20); break;
            case 30: ImageUtils.drawImage(levelGraphics, dark ? sprites[61] : sprites[57], px, py, 20); break;
            case 31: ImageUtils.drawImage(levelGraphics, dark ? sprites[60] : sprites[56], px, py, 20); break;
            case 32: ImageUtils.drawImage(levelGraphics, dark ? sprites[59] : sprites[55], px, py, 20); break;
            case 33: ImageUtils.drawImage(levelGraphics, dark ? sprites[62] : sprites[58], px, py, 20); break;
            case 34: levelGraphics.fillRect(px,py,12,12); ImageUtils.drawImage(levelGraphics, sprites[65], px,py,20); break;
            case 35: levelGraphics.fillRect(px,py,12,12); ImageUtils.drawImage(levelGraphics, sprites[64], px,py,20); break;
            case 36: levelGraphics.fillRect(px,py,12,12); ImageUtils.drawImage(levelGraphics, sprites[63], px,py,20); break;
            case 37: levelGraphics.fillRect(px,py,12,12); ImageUtils.drawImage(levelGraphics, sprites[66], px,py,20); break;
            case 38: ImageUtils.drawImage(levelGraphics, sprites[53], px, py, 20); break;
            case 39: levelGraphics.fillRect(px,py,12,12); ImageUtils.drawImage(levelGraphics, sprites[50], px,py,20); break;
            case 40: levelGraphics.fillRect(px,py,12,12); ImageUtils.drawImage(levelGraphics, ImageUtils.applyTransform(sprites[50], ImageUtils.TRANS_MIRROR_ROT180), px,py,20); break;
            case 41: levelGraphics.fillRect(px,py,12,12); ImageUtils.drawImage(levelGraphics, ImageUtils.applyTransform(sprites[50], ImageUtils.TRANS_MIRROR_ROT270), px,py,20); break;
            case 42: levelGraphics.fillRect(px,py,12,12); ImageUtils.drawImage(levelGraphics, ImageUtils.applyTransform(sprites[50], ImageUtils.TRANS_ROT270), px,py,20); break;
            case 43: levelGraphics.fillRect(px,py,12,12); ImageUtils.drawImage(levelGraphics, sprites[51], px,py,20); break;
            case 47: ImageUtils.drawImage(levelGraphics, sprites[52], px, py, 20); break;
            case 51: ImageUtils.drawImage(levelGraphics, sprites[54], px, py, 20); break;
            default:
                levelGraphics.fillRect(px, py, 12, 12);
                break;
        }
    }

    /** Add an image to the deferred overlay draw list. (sub_1bc) */
    public void addDeferred(BufferedImage img, int x, int y) {
        deferredImages.add(img);
        deferredX.add(x);
        deferredY.add(y);
    }

    /** Redraw all visible tiles. (sub_1dc) */
    public void redrawAllTiles() {
        for (int c = 0; c < 13; c++) {
            for (int r = 0; r < 8; r++) {
                drawTile(scrollLeft + c, scrollTop + r, c * 12, r * 12);
            }
        }
    }

    /** Redraw only dirty tiles (tiles marked with bit 128). (sub_20e) */
    public void redrawDirtyTiles(boolean doDraw) {
        int col = scrollLeft;
        int row = scrollTop;
        for (int c = 0; c < 13; c++) {
            if (c * 12 >= wrapBoundary && col >= scrollLeft) {
                col = scrollRight - 13;
            }
            for (int r = 0; r < 8; r++) {
                if ((tiles[row][col] & 128) != 0) {
                    tiles[row][col] = (short)(tiles[row][col] & 0xFF7F);
                    if (doDraw) drawTile(col, row, c * 12, r * 12);
                }
                row++;
            }
            row = scrollTop;
            col++;
        }
    }

    // ---- Scrolling ----------------------------------------------------------

    /**
     * Update scrolling position based on ball movement.
     * (Originally: sub_229)
     */
    public void updateScroll(int ballX, int ballDeltaX, int minBallX) {
        if (leadingEdge < 0)   leadingEdge += 156;
        if (trailingEdge >= 156) trailingEdge -= 156;
        if (trailingEdge < 0)  trailingEdge += 156;

        if (leadingEdge > wrapBoundary && leadingEdge <= wrapBoundary + 12) {
            if (scrollLeft + wrapBoundary / 12 >= tileCols) {
                trailingEdge -= ballDeltaX;
                leadingEdge  -= ballDeltaX;
                if (leadingEdge < 0) leadingEdge += 156;
                if (autoScroll) {
                    autoScroll = false;
                    cameraX    = leadingEdge - 64;
                    if (cameraX < minBallX) cameraX += 156;
                }
            } else {
                if (wrapBoundary >= 156) { wrapBoundary = 0; scrollLeft += 13; }
                if (leadingEdge >= 156) leadingEdge -= 156;
                int prevWrap = wrapBoundary;
                wrapBoundary += 12;
                scrollRight++;
                for (int r = 0; r < 8; r++) {
                    drawTile(scrollLeft + prevWrap / 12, scrollTop + r, prevWrap, r * 12);
                }
            }
        } else if (leadingEdge > 156) {
            leadingEdge -= 156;
        }

        if (trailingEdge >= 156) trailingEdge -= 156;
        if (trailingEdge < 0)   trailingEdge += 156;

        if (trailingEdge < wrapBoundary && trailingEdge >= wrapBoundary - 12) {
            if (scrollLeft - (13 - wrapBoundary / 12) <= 0) {
                trailingEdge -= ballDeltaX;
                leadingEdge  -= ballDeltaX;
                if (trailingEdge >= 156) trailingEdge -= 156;
                if (autoScroll) {
                    autoScroll = false;
                    cameraX    = (trailingEdge + 64) % 156;
                    if (cameraX < minBallX) cameraX += 156;
                }
            } else {
                wrapBoundary -= 12;
                int prevWrap = wrapBoundary;
                scrollRight--;
                if (wrapBoundary <= 0) { wrapBoundary = 156; scrollLeft -= 13; }
                for (int r = 0; r < 8; r++) {
                    drawTile(scrollRight - 13, scrollTopRight + r, prevWrap, r * 12);
                }
            }
        }
    }

    /**
     * Check if ball has triggered auto-scroll stop.
     * (Originally: sub_249)
     */
    public void checkAutoScrollStop(int ballX, int delta) {
        if (!autoScroll) {
            if (scrollLeft - (13 - wrapBoundary / 12) <= 0 &&
                ballX >= cameraX && ballX < cameraX + 10) {
                autoScroll = true;
                delta = ballX - cameraX;
            }
            if (scrollLeft + wrapBoundary / 12 >= tileCols &&
                ballX <= cameraX && ballX > cameraX - 10) {
                autoScroll = true;
                delta = ballX - cameraX;
            }
        }
        if (autoScroll) {
            trailingEdge += delta;
            leadingEdge  += delta;
        }
    }

    // ---- Portal -----------------------------------------------------------

    /**
     * Set portal location and image.
     * (Originally: sub_387)
     */
    public void setPortal(int col, int row, int endCol, int endRow, BufferedImage frame) {
        portalCol     = col;
        portalRow     = row;
        portalEndCol  = endCol;
        portalEndRow  = endRow;
        portalFrameImg = frame;
        portalImage   = ImageUtils.createImage(24, 24);
        portalAnim    = 0;
        rebuildPortalImage();
        exitOpen      = false;
    }

    /** Rebuild portal composite image. (sub_3af) */
    public void rebuildPortalImage() {
        Graphics2D g = portalImage.createGraphics();
        ImageUtils.drawImage(g, portalFrameImg, 0, -portalAnim, 20);
        g.dispose();
    }

    /** Advance portal open animation. (sub_3e3) */
    public void animatePortal() {
        portalAnim += 4;
        if (portalAnim >= 24) { portalAnim = 24; exitOpen = true; }
        rebuildPortalImage();
    }

    // ---- Score/lives (abstract — implemented by GameScreen) ----------------

    public abstract void addScore(int points);
    public abstract void playSoundUp();
    public abstract void playSoundPickup();
    public abstract void playSoundPop();
    public abstract void vibrate(int ms);

    // ---- Game loop ---------------------------------------------------------

    /** Called every frame by GameTimer. (Originally: sub_4ee → sub_42e) */
    public void tick() { onTick(); }

    /** Override in subclass to implement per-frame logic. */
    protected abstract void onTick();

    // ---- Timer control -----------------------------------------------------

    /** Start the game loop timer. (sub_47f) */
    public synchronized void startTimer() {
        if (gameTimer == null) {
            gameTimer = new GameTimer(this, this);
        }
    }

    /** Stop the game loop timer. (sub_4d4) */
    public synchronized void stopTimer() {
        if (gameTimer != null) {
            gameTimer.stopTimer();
            gameTimer = null;
        }
    }

    // ---- Sprite loading ----------------------------------------------------

    /**
     * Build the sprite array (67 entries) by loading PNG files from /bounce/icons/.
     * Each sprite is 12×12 px. Named: /bounce/icons/ROW COL.png  (e.g., /bounce/icons/03.png)
     * (Originally: sub_298)
     */
    public void loadAllSprites() {
        sprites = new BufferedImage[67];

        sprites[0]  = ImageUtils.loadSpriteColored(1, 0, 22);
        sprites[1]  = ImageUtils.loadSpriteColored(1, 2, 22);
        // Spikes: load base (upward) then derive 3 rotations, same pattern as ramp sprites 55-62
        // Light colour variants
        sprites[2]  = ImageUtils.loadSpriteColored(0, 3,  -5185296);                                        // up
        sprites[3]  = ImageUtils.applyTransform(sprites[2], ImageUtils.TRANS_ROT90);                        // right
        sprites[4]  = ImageUtils.applyTransform(sprites[2], ImageUtils.TRANS_ROT180);                       // down
        sprites[5]  = ImageUtils.applyTransform(sprites[2], ImageUtils.TRANS_ROT270);                       // left
        // Dark colour variants
        sprites[6]  = ImageUtils.loadSpriteColored(0, 3,  -15703888);
        sprites[7]  = ImageUtils.applyTransform(sprites[6], ImageUtils.TRANS_ROT90);
        sprites[8]  = ImageUtils.applyTransform(sprites[6], ImageUtils.TRANS_ROT180);
        sprites[9]  = ImageUtils.applyTransform(sprites[6], ImageUtils.TRANS_ROT270);
        sprites[10] = ImageUtils.loadSpriteColored(0, 4,  22);
        sprites[11] = ImageUtils.loadSpriteColored(3, 4,  22);
        sprites[12] = buildPortalSprite(ImageUtils.loadSpriteColored(2, 3, 22));
        sprites[13] = ImageUtils.loadSpriteColored(0, 53, 22);
        sprites[14] = ImageUtils.loadSpriteColored(0, 5,  22);
        sprites[15] = ImageUtils.loadSpriteColored(0, 51, 22);
        sprites[16] = ImageUtils.loadSpriteColored(0, 52, 22);
        sprites[17] = ImageUtils.loadSpriteColored(1, 53, 22);
        sprites[18] = ImageUtils.loadSpriteColored(1, 5,  22);
        sprites[19] = ImageUtils.loadSpriteColored(1, 51, 22);
        sprites[20] = ImageUtils.loadSpriteColored(1, 52, 22);
        sprites[21] = ImageUtils.loadSpriteColored(2, 51, 22);
        sprites[22] = ImageUtils.loadSpriteColored(2, 5,  22);
        sprites[23] = ImageUtils.loadSpriteColored(2, 52, 22);
        sprites[24] = ImageUtils.loadSpriteColored(2, 53, 22);
        sprites[25] = ImageUtils.loadSpriteColored(3, 51, 22);
        sprites[26] = ImageUtils.loadSpriteColored(3, 5,  22);
        sprites[27] = ImageUtils.loadSpriteColored(3, 52, 22);
        sprites[28] = ImageUtils.loadSpriteColored(3, 53, 22);
        sprites[29] = ImageUtils.loadSpriteColored(0, 54, 22);
        sprites[30] = ImageUtils.loadSpriteColored(0, 56, 22);
        sprites[31] = ImageUtils.loadSpriteColored(0, 55, 22);
        sprites[32] = ImageUtils.loadSpriteColored(0, 57, 22);
        sprites[33] = ImageUtils.loadSpriteColored(1, 54, 22);
        sprites[34] = ImageUtils.loadSpriteColored(1, 56, 22);
        sprites[35] = ImageUtils.loadSpriteColored(1, 55, 22);
        sprites[36] = ImageUtils.loadSpriteColored(1, 57, 22);
        sprites[37] = ImageUtils.loadSpriteColored(2, 54, 22);
        sprites[38] = ImageUtils.loadSpriteColored(2, 56, 22);
        sprites[39] = ImageUtils.loadSpriteColored(2, 55, 22);
        sprites[40] = ImageUtils.loadSpriteColored(2, 57, 22);
        sprites[41] = ImageUtils.loadSpriteColored(3, 54, 22);
        sprites[42] = ImageUtils.loadSpriteColored(3, 56, 22);
        sprites[43] = ImageUtils.loadSpriteColored(3, 55, 22);
        sprites[44] = ImageUtils.loadSpriteColored(3, 57, 22);
        sprites[45] = ImageUtils.loadSpriteColored(3, 3,  22);
        sprites[46] = ImageUtils.loadSpriteColored(1, 3,  22);
        sprites[47] = ImageUtils.loadSpriteColored(2, 0,  22);
        sprites[48] = ImageUtils.loadSpriteColored(0, 1,  22);
        sprites[49] = ImageUtils.loadImage("/bounce/icons/30.png");
        sprites[50] = ImageUtils.loadSpriteColored(3, 1,  22);
        sprites[51] = ImageUtils.loadSpriteColored(2, 4,  22);
        sprites[52] = ImageUtils.loadSpriteColored(3, 2,  22);
        sprites[53] = ImageUtils.loadSpriteColored(1, 1,  22);
        sprites[54] = ImageUtils.loadSpriteColored(2, 2,  22);
        sprites[55] = ImageUtils.loadSpriteColored(0, 0,  -5185296);
        sprites[56] = ImageUtils.applyTransform(sprites[55], ImageUtils.TRANS_ROT270);
        sprites[57] = ImageUtils.applyTransform(sprites[55], ImageUtils.TRANS_MIRROR_ROT270);
        sprites[58] = ImageUtils.applyTransform(sprites[55], ImageUtils.TRANS_MIRROR_ROT90);
        sprites[59] = ImageUtils.loadSpriteColored(0, 0,  -15703888);
        sprites[60] = ImageUtils.applyTransform(sprites[59], ImageUtils.TRANS_ROT270);
        sprites[61] = ImageUtils.applyTransform(sprites[59], ImageUtils.TRANS_MIRROR_ROT270);
        sprites[62] = ImageUtils.applyTransform(sprites[59], ImageUtils.TRANS_MIRROR_ROT90);
        sprites[63] = ImageUtils.loadSpriteColored(0, 2,  22);
        sprites[64] = ImageUtils.applyTransform(sprites[63], ImageUtils.TRANS_ROT270);
        sprites[65] = ImageUtils.applyTransform(sprites[63], ImageUtils.TRANS_MIRROR_ROT270);
        sprites[66] = ImageUtils.applyTransform(sprites[63], ImageUtils.TRANS_MIRROR_ROT90);

        lifeSprite  = ImageUtils.loadSpriteColored(2, 1, 22);
        gemSprite   = ImageUtils.loadSpriteColored(1, 4, 22);
        arrowSprite = ImageUtils.loadSpriteColored(6, 6, 22);
    }

    /** Assign ball sprites from sprite array. (Originally: sub_2a9) */
    public void assignBallSprites(Ball b) {
        b.smallBallSprite = sprites[47];
        b.deathSprite     = sprites[48];
        b.largeBallSprite = sprites[49];
    }

    /** Build the 24×48 portal sprite from a 12×12 source tile. (Originally: sub_272) */
    private BufferedImage buildPortalSprite(BufferedImage src) {
        if (src == null) return ImageUtils.createImage(24, 48);
        BufferedImage img = ImageUtils.createImage(24, 48);
        Graphics2D g = img.createGraphics();
        g.setColor(ImageUtils.toColor(GameConstants.COLOR_TILE_LIGHT)); g.fillRect(0, 0, 24, 48);
        g.setColor(new Color(16555422)); g.fillRect(4, 0, 16, 48);
        g.setColor(new Color(14891583)); g.fillRect(6, 0, 10, 48);
        g.setColor(new Color(12747918)); g.fillRect(10, 0, 4, 48);
        ImageUtils.drawImage(g, src,                                          0,  0, 20);
        ImageUtils.drawImage(g, ImageUtils.applyTransform(src, ImageUtils.TRANS_MIRROR), 12, 0, 20);
        ImageUtils.drawImage(g, ImageUtils.applyTransform(src, ImageUtils.TRANS_MIRROR_ROT180), 0, 12, 20);
        ImageUtils.drawImage(g, ImageUtils.applyTransform(src, ImageUtils.TRANS_ROT180), 12, 12, 20);
        g.dispose();
        return img;
    }

    /** Get sprite by index. (Originally: sub_37c) */
    public BufferedImage getSprite(int idx) {
        return (idx < 67) ? sprites[idx] : null;
    }

    // ---- Static geometry utility -------------------------------------------

    /** AABB overlap test. (Originally: sub_418) */
    public static boolean rectsOverlap(int x1, int y1, int x2, int y2,
                                       int ax, int ay, int bx, int by) {
        return x1 <= bx && y1 <= by && ax <= x2 && ay <= y2;
    }

    // ---- Formatted score string --------------------------------------------

    /** Left-pad score with zeros to 8 digits. (Originally: sub_289) */
    public static String formatScore(int score) {
        String prefix = "";
        if      (score < 100)      prefix = "0000000";
        else if (score < 1000)     prefix = "00000";
        else if (score < 10000)    prefix = "0000";
        else if (score < 100000)   prefix = "000";
        else if (score < 1000000)  prefix = "00";
        else if (score < 10000000) prefix = "0";
        return prefix + score;
    }
}
