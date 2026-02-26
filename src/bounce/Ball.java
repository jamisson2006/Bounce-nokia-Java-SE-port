package bounce;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * The player-controlled ball — handles physics, input, collision, rendering.
 * Originally: Class_c8 (obfuscated).
 * Ported to Java SE 1.8.
 */
public class Ball {

    // ---- Ball position (pixels, fixed-point /10 for velocity) ----
    public int x;           // horizontal position (was var_72)
    public int y;           // vertical  position (was var_bf)

    /** "Relative" x/y offset within the visible tile grid. */
    public int relX;        // was var_cd
    public int relY;        // was var_e8

    // ---- Velocity ----
    public int velY;        // vertical   velocity ×10 (was var_141)
    public int velX;        // horizontal velocity ×10 (was var_15d)

    // ---- Ball state ----
    public int  size;           // diameter in pixels (12 or 16) — was var_195
    public int  radius;         // half of size — was var_1b6
    public int  teleportRow;    // last teleport destination row — was var_1ee
    public int  teleportCol;    // last teleport destination col — was var_267
    public int  deltaX;         // x movement this frame — was var_11a
    public int  onGroundDir;    // direction flags — was var_297
    public int  deathTimer;     // death countdown (was var_671)
    public int  waveTimer;      // wave-speed timer (was var_36f)
    public int  reverseTimer;   // reverse-gravity timer (was var_3bb)
    public int  stickyTimer;    // sticky floor timer (was var_404)
    public int  animFrame;      // animation frame counter (was var_4e0)

    // ---- Flags ----
    public boolean onGround;    // touching floor (was var_455)
    public boolean hasBoost;    // speed-boost powerup active (was var_4b0)
    public boolean slideAid;    // slide correction active (was var_4be)

    // ---- Input bitmask (UP=8, LEFT=1, RIGHT=2, DOWN=4) ----
    public int  inputKeys;      // was var_181

    // ---- Collision masks (pixel bitmaps for 12x12 and 16x16 balls) ----
    // var_529 — wedge/ramp collision mask
    public static final byte[][] RAMP_MASK = {
        {0,0,0,0,0,0,0,0,0,0,0,1},
        {0,0,0,0,0,0,0,0,0,0,1,1},
        {0,0,0,0,0,0,0,0,0,1,1,1},
        {0,0,0,0,0,0,0,0,1,1,1,1},
        {0,0,0,0,0,0,0,1,1,1,1,1},
        {0,0,0,0,0,0,1,1,1,1,1,1},
        {0,0,0,0,0,1,1,1,1,1,1,1},
        {0,0,0,0,1,1,1,1,1,1,1,1},
        {0,0,0,1,1,1,1,1,1,1,1,1},
        {0,0,1,1,1,1,1,1,1,1,1,1},
        {0,1,1,1,1,1,1,1,1,1,1,1},
        {1,1,1,1,1,1,1,1,1,1,1,1}
    };

    // var_55e — small ball (12×12) pixel mask
    public static final byte[][] BALL_MASK_12 = {
        {0,0,0,0,1,1,1,1,0,0,0,0},
        {0,0,1,1,1,1,1,1,1,1,0,0},
        {0,1,1,1,1,1,1,1,1,1,1,0},
        {0,1,1,1,1,1,1,1,1,1,1,0},
        {1,1,1,1,1,1,1,1,1,1,1,1},
        {1,1,1,1,1,1,1,1,1,1,1,1},
        {1,1,1,1,1,1,1,1,1,1,1,1},
        {1,1,1,1,1,1,1,1,1,1,1,1},
        {0,1,1,1,1,1,1,1,1,1,1,0},
        {0,1,1,1,1,1,1,1,1,1,1,0},
        {0,0,1,1,1,1,1,1,1,1,0,0},
        {0,0,0,0,1,1,1,1,0,0,0,0}
    };

    // var_57c — large ball (16×16) pixel mask
    public static final byte[][] BALL_MASK_16 = {
        {0,0,0,0,0,1,1,1,1,1,1,0,0,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,1,0,0,0},
        {0,0,1,1,1,1,1,1,1,1,1,1,1,1,0,0},
        {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
        {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
        {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
        {0,0,1,1,1,1,1,1,1,1,1,1,1,1,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,1,0,0,0},
        {0,0,0,0,0,1,1,1,1,1,1,0,0,0,0,0}
    };

    // ---- References ----
    public GameCanvas canvas;       // owning canvas (was var_58c)
    public BufferedImage sprite;    // current ball sprite (was var_5cb)
    public BufferedImage deathSprite;   // explosion (was var_600)
    public BufferedImage largeBallSprite; // big-mode sprite (was var_658)
    public BufferedImage smallBallSprite; // normal-mode sprite (was var_665)

    // ---- Constructor -------------------------------------------------------

    /**
     * @param startX   starting x in pixels within the visible area
     * @param startY   starting y in pixels
     * @param sizeMode 0 = small (12 px), 1 = large (16 px)
     * @param canvas   owning game canvas
     */
    public Ball(int startX, int startY, int sizeMode, GameCanvas canvas) {
        this.x        = startX;
        this.y        = startY;
        this.relX     = 0;
        this.relY     = 0;
        this.velX     = 0;
        this.velY     = 0;
        this.canvas   = canvas;
        this.onGroundDir  = 0;
        this.onGround     = false;
        this.hasBoost     = false;
        this.slideAid     = false;
        this.deathTimer   = 0;
        this.waveTimer    = 0;
        this.reverseTimer = 0;
        this.stickyTimer  = 0;
        this.animFrame    = 0;
        this.inputKeys    = 0;

        // Load sprites from the canvas
        canvas.assignBallSprites(this);

        if (sizeMode == 0) {
            this.size   = 12;
            this.radius = 6;
            this.sprite = smallBallSprite;
        } else {
            this.size   = 16;
            this.radius = 8;
            this.sprite = largeBallSprite;
        }
    }

    // ---- Input handling ----------------------------------------------------

    /** Set a directional key as pressed. key: UP=8, LEFT=1, RIGHT=2, DOWN=4 */
    public void keyPressed(int key) {
        if (key == 8 || key == 4 || key == 2 || key == 1) {
            inputKeys |= key;
        }
    }

    /** Set a directional key as released. */
    public void keyReleased(int key) {
        if (key == 8 || key == 4 || key == 2 || key == 1) {
            inputKeys &= ~key;
        }
    }

    /** Release all directional keys. */
    public void releaseAllKeys() {
        inputKeys &= 0xFFFFFFF0;  // clear lower 4 bits
    }

    // ---- Size changes -------------------------------------------------------

    /** Update position/state when ball teleports to a tile destination. */
    public void setTeleportDestination(int col, int row) {
        this.teleportCol = col;
        this.teleportRow = row;
        if (size == 16) {
            canvas.ballSizeIndex = 1;
        } else {
            canvas.ballSizeIndex = 0;
        }
    }

    /** Grow to large size (16 px). */
    public void growLarge() {
        int step = 2;
        this.size   = 16;
        this.radius = 8;
        this.sprite = largeBallSprite;
        boolean placed = false;
        while (!placed) {
            placed = true;
            if      (checkCollision(x,      y - step)) { y -= step; }
            else if (checkCollision(x-step, y - step)) { x -= step; y -= step; }
            else if (checkCollision(x+step, y - step)) { x += step; y -= step; }
            else if (checkCollision(x,      y + step)) { y += step; }
            else if (checkCollision(x-step, y + step)) { x -= step; y += step; }
            else if (checkCollision(x+step, y + step)) { x += step; y += step; }
            else { placed = false; step++; }
        }
    }

    /** Shrink to small size (12 px). */
    public void shrinkSmall() {
        int step = 2;
        this.size   = 12;
        this.radius = 6;
        this.sprite = smallBallSprite;
        if      (checkCollision(x, y + step)) { y += step; }
        else if (checkCollision(x, y - step)) { y -= step; }
    }

    // ---- Physics / update --------------------------------------------------

    /**
     * Main physics tick — called each frame.
     * Handles gravity, movement, collision, wrapping.
     * (Originally: sub_33d)
     */
    public void update() {
        int prevX = x;

        // --- Death animation ---
        if (onGroundDir == 2) {
            deltaX = 0;
            deathTimer--;
            if (deathTimer == 0) {
                onGroundDir = 1;
                if (canvas.lives < 0) {
                    canvas.gameOverFlag = true;
                }
            }
            return;
        }

        // --- Gravity parameters ---
        int tileCol = x / 12;
        int tileRow = y / 12;

        // Adjust for scrolling position
        if (x >= 156) {
            tileCol = canvas.scrollLeft + tileCol - 13;
            tileRow += canvas.scrollTop;
        } else if (x < canvas.wrapBoundary) {
            tileCol += canvas.scrollLeft;
            tileRow += canvas.scrollTop;
        } else {
            tileCol += canvas.scrollRight - 13 - canvas.wrapBoundary / 12;
            tileRow += canvas.scrollTopRight;
        }

        int gravity, gravStep;
        boolean antiGrav = (canvas.tiles[tileRow][tileCol] & 64) != 0;
        if (antiGrav) {
            // Anti-gravity zone: invert gravity direction
            if (size == 16) { gravity = -30; gravStep = -2; if (onGround) velY = -10; }
            else             { gravity = -80; gravStep = -4; if (onGround) velY = -10; } // was 42/6 (wrong sign!)
        } else {
            if (size == 16) { gravity =  38; gravStep =  3; }
            else             { gravity =  80; gravStep =  4; }
        }

        // --- Reverse-gravity powerup ---
        boolean reversed = false;
        if (reverseTimer != 0) {
            reversed = true;
            gravity  *= -1;
            gravStep *= -1;
            reverseTimer--;
            if (reverseTimer == 0) {
                reversed = false;
                onGround  = false;
                gravity  *= -1;
                gravStep *= -1;
            }
        }

        // --- Sticky floor ---
        if (stickyTimer != 0) {
            if (-1 * Math.abs(velY) > -80) {
                velY = reversed ? 80 : -80;
            }
            stickyTimer--;
        }

        // --- Animation frame ---
        animFrame = (animFrame + 1) % 3;

        // --- Clamp velocities ---
        velY = Math.max(-150, Math.min(150, velY));
        velX = Math.max(-150, Math.min(150, velX));

        // --- Vertical movement ---
        int stepsY = Math.abs(velY) / 10;
        for (int i = 0; i < stepsY; i++) {
            int dirY = velY == 0 ? 0 : (velY > 0 ? 1 : -1);

            if (checkCollision(x, y + dirY)) {
                y += dirY;
                onGround = false;
                // Friction on anti-grav floor
                if (gravity == -30) {
                    int adjRow = canvas.scrollTop + (y / 12);
                    if ((canvas.tiles[adjRow][tileCol] & 64) == 0) {
                        velY >>= 1;
                        if (velY <= 10 && velY >= -10) velY = 0;
                    }
                }
            } else {
                // Slide-aid: nudge 1px horizontally to help navigate narrow gaps
                if (slideAid && velX < 10 && animFrame == 0) {
                    if      (checkCollision(x+1, y + dirY)) { x++; y += dirY; slideAid = false; }
                    else if (checkCollision(x-1, y + dirY)) { x--; y += dirY; slideAid = false; }
                }

                boolean hitFloor = (dirY > 0 || (reversed && dirY < 0));
                if (hitFloor) {
                    velY = velY * -1 / 2;
                    onGround = true;
                    if (hasBoost && (inputKeys & 8) != 0) {
                        hasBoost = false;
                        velY += reversed ? 10 : -10;
                    } else if (stickyTimer == 0) {
                        velY = 0; // no horizontal correction reset here
                    }
                    if (velY < 10 && velY > -10) {
                        velY = reversed ? -10 : 10;
                    }
                    break;
                }

                boolean hitCeiling = (dirY < 0 || (reversed && dirY > 0));
                if (hitCeiling) {
                    velY = reversed ? -20 : 20;
                }
            }
        }

        // --- Apply gravity ---
        if (reversed) {
            if (gravStep == -2 && velY < gravity) { velY += gravStep; if (velY > gravity) velY = gravity; }
            else if (!onGround && velY > gravity)  { velY += gravStep; if (velY < gravity) velY = gravity; }
        } else {
            if (gravStep == -2 && velY > gravity) { velY += gravStep; if (velY < gravity) velY = gravity; }
            else if (!onGround && velY < gravity)  { velY += gravStep; if (velY > gravity) velY = gravity; }
        }

        // --- Speed boost lateral cap ---
        int lateralMax = (waveTimer != 0) ? 100 : 50;

        // --- Horizontal input ---
        if      ((inputKeys & 2) != 0 && velX < lateralMax)  velX += 6;
        else if ((inputKeys & 1) != 0 && velX > -lateralMax) velX -= 6;
        else if (velX > 0)  velX -= 4;
        else if (velX < 0)  velX += 4;

        // --- Jump press ---
        if (onGround && (inputKeys & 8) != 0) {
            velY = reversed ? (100 + velY) : (-100 + velY);
            onGround = false;
        }

        // --- Horizontal movement ---
        int stepsX = Math.abs(velX) / 10;
        for (int i = 0; i < stepsX; i++) {
            int dirX = velX == 0 ? 0 : (velX > 0 ? 1 : -1);
            if (checkCollision(x + dirX, y)) {
                x += dirX;
            } else if (slideAid) {
                slideAid = false;
                int upDown = reversed ? 1 : -1;
                if      (checkCollision(x+dirX, y+upDown)) { x += dirX; y += upDown; }
                else if (checkCollision(x+dirX, y-upDown)) { x += dirX; y -= upDown; }
                else                                        { velX = -(velX >> 1); }
            }
        }

        deltaX = x - prevX;

        // --- World wrap ---
        if (x > 156 + size) {
            x -= 156;
            if (canvas.cameraX - 10 > 156 + size) canvas.cameraX -= 156;
        }
        if (x - size < 0) {
            x += 156;
            if (canvas.cameraX - size < 10) canvas.cameraX += 156;
        }
    }

    // ---- Tile collision ----------------------------------------------------

    /**
     * Check whether the ball can occupy position (px, py) without overlapping solid tiles.
     * Returns true if the position is free.
     * (Originally: sub_c7)
     */
    public boolean checkCollision(int px, int py) {
        int offsetY = 0;
        if (py < 0) offsetY = 12;

        int tileLeft   = (px - radius) / 12;
        int tileTop    = (py - offsetY - radius) / 12;
        relX = px - radius;
        relY = py - radius;

        // Adjust for scrolling
        if (x < canvas.wrapBoundary) {
            relX += canvas.scrollLeft * 12;
            relY += canvas.scrollTop  * 12;
        } else {
            relX += (canvas.scrollRight - 13) * 12 - canvas.wrapBoundary;
            relY += canvas.scrollTopRight * 12;
        }

        int tileRight  = (px - 1 + radius) / 12 + 1;
        int tileBottom = (py - offsetY - 1 + radius) / 12 + 1;
        boolean free = true;

        for (int col = tileLeft; col < tileRight; col++) {
            for (int row = tileTop; row < tileBottom; row++) {
                int mapRow, mapCol;
                if (col * 12 > 156) {
                    mapCol = canvas.scrollTop  + row;
                    mapRow = canvas.scrollLeft + col - 13;
                } else if (x < canvas.wrapBoundary) {
                    mapCol = canvas.scrollTop  + row;
                    mapRow = canvas.scrollLeft + col;
                } else {
                    int shift = canvas.scrollRight - 13 - canvas.wrapBoundary / 12;
                    mapCol = canvas.scrollTopRight + row;
                    mapRow = shift + col;
                }
                free = processTileCollision(mapCol, mapRow, free);
            }
        }
        return free;
    }

    /**
     * Process a single tile's collision effect on the ball.
     * (Originally: sub_2ee)
     */
    private boolean processTileCollision(int row, int col, boolean free) {
        if (row < 0 || row >= canvas.tileRows || col < 0 || col >= canvas.tileCols) {
            return false;
        }
        if (onGroundDir == 2) return false;

        int tileType  = canvas.tiles[row][col] & ~64 & ~128;

        switch (tileType) {
            case 1: // wall
                if (circleOverlapsTile(row, col)) free = false; else slideAid = true;
                break;
            case 2: // spiked wall
                if (circleOverlapsTile(row, col)) { hasBoost = true; free = false; } else slideAid = true;
                break;
            case 3: case 4: case 5: case 6: // spikes
                if (rectOverlapsTile(row, col, tileType)) { free = false; die(); }
                break;
            case 7: // teleport
                canvas.addScore(200);
                canvas.tiles[teleportRow][teleportCol] = 128;
                setTeleportDestination(col, row);
                canvas.tiles[row][col] = 136;
                canvas.playSoundPickup();
                break;
            case 9: // portal (exit)
                if (rectOverlapsTile(row, col, tileType)) {
                    if (canvas.exitOpen) { canvas.nextLevelFlag = true; canvas.playSoundPickup(); }
                    else free = false;
                }
                break;
            case 10: { // moving platform / enemy
                int idx = canvas.findMovingObject(col, row);
                if (idx != -1) {
                    int ox = canvas.movObjX[idx][0]*12 + canvas.movObjOffset[idx][0];
                    int oy = canvas.movObjX[idx][1]*12 + canvas.movObjOffset[idx][1];
                    if (GameCanvas.rectsOverlap(relX, relY, relX+size, relY+size, ox, oy, ox+24, oy+24)) {
                        free = false; die();
                    }
                }
                break;
            }
            case 13: case 14: case 15: case 16: // grow item
                if (rectOverlapsTile(row, col, tileType)) {
                    if (size == 16) { free = false; }
                    else {
                        if (rampOverlapsTile(row, col, tileType)) free = false;
                        collectGrowItem(row, col, tileType);
                    }
                }
                break;
            case 17: case 19: case 20: // anti-grav / score tiles
                if (rectOverlapsTile(row, col, tileType)) {
                    if (size == 16) free = false;
                    else if (rampOverlapsTile(row, col, tileType)) free = false;
                }
                break;
            case 18:
                if (rectOverlapsTile(row, col, tileType) && size == 16) free = false;
                break;
            case 21: case 22: case 23: case 24:
                if (rectOverlapsTile(row, col, tileType)) {
                    if (rampOverlapsTile(row, col, tileType)) free = false;
                    collectScoreItem(row, col, tileType);
                }
                break;
            case 25: case 27: case 28:
                if (rampOverlapsTile(row, col, tileType)) free = false;
                break;
            case 29: // extra life
                canvas.addScore(1000);
                if (canvas.lives < 5) { canvas.lives++; canvas.hudDirty = true; }
                canvas.tiles[row][col] = 128;
                canvas.playSoundPickup();
                break;
            case 30: case 31: case 32: case 33: // bounce pads
                if (rampBounce(row, col, tileType)) { free = false; slideAid = true; }
                break;
            case 34: case 35: case 36: case 37: // bounce pads (with boost)
                if (rampBounce(row, col, tileType)) { hasBoost = true; free = false; slideAid = true; }
                break;
            case 38: // wave speed boost
                waveTimer = 300;
                canvas.playSoundPickup();
                free = false;
                break;
            case 39: case 40: case 41: case 42: // slow-down
                free = false;
                if (size == 16) shrinkSmall();
                break;
            case 43: case 44: case 45: case 46: // grow platforms
                if (rectOverlapsTile(row, col, tileType)) { free = false; if (size == 12) growLarge(); }
                break;
            case 47: case 48: case 49: case 50: // shrink items
                reverseTimer = 300;
                canvas.playSoundPickup();
                free = false;
                break;
            case 51: case 52: case 53: case 54: // sticky floor
                stickyTimer = 300;
                canvas.playSoundPickup();
                free = false;
                break;
            default:
                break;
        }
        return free;
    }

    // ---- Collision geometry helpers ----------------------------------------

    /**
     * Circle vs tile collision using pixel masks.
     * (Originally: sub_220)
     */
    public boolean circleOverlapsTile(int row, int col) {
        int tileX = col * 12;
        int tileY = row * 12;
        int dx    = relX - tileX;
        int dy    = relY - tileY;

        int startX = dx >= 0 ? dx : 0;
        int endX   = dx >= 0 ? 12 : size + dx;
        int startY = dy >= 0 ? dy : 0;
        int endY   = dy >= 0 ? 12 : size + dy;

        byte[][] mask = (size == 16) ? BALL_MASK_16 : BALL_MASK_12;
        if (endX > 12) endX = 12;
        if (endY > 12) endY = 12;

        for (int bx = startX; bx < endX; bx++) {
            for (int by = startY; by < endY; by++) {
                if (mask[by - dy][bx - dx] != 0) return true;
            }
        }
        return false;
    }

    /**
     * Ramp/angled tile collision using combined pixel masks.
     * (Originally: sub_22d)
     */
    public boolean rampBounce(int row, int col, int tileType) {
        int tileX = col * 12;
        int tileY = row * 12;
        int dx    = relX - tileX;
        int dy    = relY - tileY;
        int cx = 0, cy = 0;
        switch (tileType) {
            case 30: case 34: cy = 11; cx = 11; break;
            case 31: case 35: cy = 11; break;
            case 33: case 37: cx = 11; break;
        }

        int startX = dx >= 0 ? dx : 0;
        int endX   = dx >= 0 ? 12 : size + dx;
        int startY = dy >= 0 ? dy : 0;
        int endY   = dy >= 0 ? 12 : size + dy;

        byte[][] ball = (size == 16) ? BALL_MASK_16 : BALL_MASK_12;
        if (endX > 12) endX = 12;
        if (endY > 12) endY = 12;

        for (int bx = startX; bx < endX; bx++) {
            for (int by = startY; by < endY; by++) {
                if ((RAMP_MASK[Math.abs(by - cy)][Math.abs(bx - cx)] &
                     ball[by - dy][bx - dx]) != 0) {
                    if (!onGround) applyBounceReflect(tileType);
                    return true;
                }
            }
        }
        return false;
    }

    /** Apply velocity reflection for angled bounce-pad tiles. (sub_20c) */
    private void applyBounceReflect(int tileType) {
        int prevVelX = velX;
        switch (tileType) {
            case 30: case 32: velX = -(velY >> 1); velY = -(prevVelX >> 1); break;
            case 31: case 33: velX =  (velY >> 1); velY =  (prevVelX >> 1); break;
            case 34: case 36: velX = -velY;         velY = -prevVelX;         break;
            case 35: case 37: velX =  velY;         velY =  prevVelX;         break;
        }
    }

    /**
     * Rectangle-based tile collision (box vs box).
     * (Originally: sub_280)
     */
    public boolean rectOverlapsTile(int row, int col, int tileType) {
        int tx1 = col * 12, ty1 = row * 12;
        int tx2 = tx1 + 12, ty2 = ty1 + 12;
        // Shrink hitbox for angled tiles
        switch (tileType) {
            case 3: case 5: case 9: case 13: case 14: case 17: case 18:
            case 21: case 22: case 43: case 45:
                tx1 += 4; tx2 -= 4; break;
            case 4: case 6: case 15: case 16: case 19: case 20:
            case 23: case 24: case 44: case 46:
                ty1 += 4; ty2 -= 4; break;
        }
        return GameCanvas.rectsOverlap(relX, relY, relX+size, relY+size, tx1, ty1, tx2, ty2);
    }

    /**
     * Narrow hitbox for ramp / angled tile corners.
     * (Originally: sub_29d)
     */
    public boolean rampOverlapsTile(int row, int col, int tileType) {
        int tx1 = col * 12, ty1 = row * 12;
        int tx2 = tx1 + 12, ty2 = ty1 + 12;
        switch (tileType) {
            case 13: case 17:
                tx1 += 6; tx2 -= 6; ty2 -= 11;
                return GameCanvas.rectsOverlap(relX, relY, relX+size, relY+size, tx1, ty1, tx2, ty2);
            case 14: case 18: case 22: case 26:
                tx1 += 6; tx2 -= 6; ty1 += 11;
                return GameCanvas.rectsOverlap(relX, relY, relX+size, relY+size, tx1, ty1, tx2, ty2);
            case 15: case 19: case 23: case 27:
                ty1 += 6; ty2 -= 6; tx2 -= 11;
                return GameCanvas.rectsOverlap(relX, relY, relX+size, relY+size, tx1, ty1, tx2, ty2);
            case 16: case 20: case 24: case 28:
                ty1 += 6; ty2 -= 6; tx1 += 11;
                return GameCanvas.rectsOverlap(relX, relY, relX+size, relY+size, tx1, ty1, tx2, ty2);
            case 21: case 25:
                int tmpY2 = ty1; ty1--;
                tx1 += 6; tx2 -= 6;
                return GameCanvas.rectsOverlap(relX, relY, relX+size, relY+size, tx1, ty1, tx2, tmpY2);
        }
        return false;
    }

    // ---- Item collection helpers -------------------------------------------

    /** Handle grow-item collection (sets tile to show acquired state). (sub_1c1) */
    private void collectGrowItem(int row, int col, int tileType) {
        canvas.addScore(500);
        canvas.gems++;
        canvas.hudDirty = true;
        int base = (canvas.tiles[row][col] & 64);
        switch (tileType) {
            case 13: canvas.tiles[row][col] = (short)(145 | base); canvas.tiles[row+1][col] = (short)(146 | base); break;
            case 14: canvas.tiles[row][col] = (short)(146 | base); canvas.tiles[row-1][col] = (short)(145 | base); break;
            case 15: canvas.tiles[row][col] = (short)(147 | base); canvas.tiles[row][col+1] = (short)(148 | base); break;
            case 16: canvas.tiles[row][col] = (short)(148 | base); canvas.tiles[row][col-1] = (short)(147 | base); break;
        }
        canvas.playSoundUp();
    }

    /** Handle score/shrink item collection. */
    private void collectScoreItem(int row, int col, int tileType) {
        canvas.addScore(500);
        canvas.gems++;
        canvas.hudDirty = true;
        int base = (canvas.tiles[row][col] & 64);
        switch (tileType) {
            case 21: canvas.tiles[row][col] = (short)(153 | base); canvas.tiles[row+1][col] = (short)(154 | base); break;
            case 22: canvas.tiles[row][col] = (short)(154 | base); canvas.tiles[row-1][col] = (short)(153 | base); break;
            case 23:
                canvas.tiles[row][col] = (short)(155 | base);
                canvas.tiles[row][col+1] = (short)(156 | base);
                break;
            case 24:
                canvas.tiles[row][col] = (short)(156 | base);
                canvas.tiles[row][col-1] = (short)(155 | base);
                break;
        }
        canvas.playSoundUp();
    }

    // ---- Death / respawn ---------------------------------------------------

    /**
     * Trigger ball death sequence.
     * (Originally: sub_15e)
     */
    public void die() {
        if (!canvas.godMode) {
            deathTimer   = 5;
            onGroundDir  = 2;
            velX         = 0;
            waveTimer    = 0;
            reverseTimer = 0;
            stickyTimer  = 0;
            canvas.lives--;
            canvas.hudDirty   = true;
            canvas.playSoundPop();
            canvas.vibrate(100);
        }
    }

    // ---- Rendering ---------------------------------------------------------

    /**
     * Draw the ball at its current position.
     * (Originally: sub_39f)
     */
    public void draw(Graphics2D g) {
        if (onGroundDir == 2) {
            // Death explosion sprite
            ImageUtils.drawImage(g, deathSprite, x - 6, y - 6, ImageUtils.ANCHOR_TOP | ImageUtils.ANCHOR_LEFT);
            if (x > 144) {
                ImageUtils.drawImage(g, deathSprite, x - 156 - 6, y - 6, ImageUtils.ANCHOR_TOP | ImageUtils.ANCHOR_LEFT);
            }
        } else {
            ImageUtils.drawImage(g, sprite, x - radius, y - radius, ImageUtils.ANCHOR_TOP | ImageUtils.ANCHOR_LEFT);
            if (x > 156 - size) {
                ImageUtils.drawImage(g, sprite, x - 156 - radius, y - radius, ImageUtils.ANCHOR_TOP | ImageUtils.ANCHOR_LEFT);
            }
        }
        markVisibilityBits();
    }

    /**
     * Mark tiles near the ball as "dirty" so they get redrawn.
     * (Originally: sub_3eb)
     */
    private void markVisibilityBits() {
        int col1 = (x - radius) / 12;
        int row1 = (y - radius) / 12;
        int col2 = (x - 1 + radius) / 12 + 1;
        int row2 = (y - 1 + radius) / 12 + 1;
        if (row1 < 0) row1 = 0;
        if (row2 > 8) row2 = 8;

        for (int c = col1; c < col2; c++) {
            for (int r = row1; r < row2; r++) {
                int mr, mc;
                if (c * 12 >= 156) {
                    mc = canvas.scrollLeft  + c - 13;
                    mr = canvas.scrollTop   + r;
                } else if (x < canvas.wrapBoundary) {
                    mc = canvas.scrollLeft  + c;
                    mr = canvas.scrollTop   + r;
                } else {
                    mc = canvas.scrollRight - 13 - canvas.wrapBoundary / 12 + c;
                    mr = canvas.scrollTopRight + r;
                }
                if (mr >= 0 && mr < canvas.tileRows && mc >= 0 && mc < canvas.tileCols) {
                    canvas.tiles[mr][mc] |= 128;
                }
            }
        }
    }
}
