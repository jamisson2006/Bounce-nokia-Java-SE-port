package bounce;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Utility methods to emulate J2ME LCDUI Graphics behavior in Java SE / Java2D.
 *
 * J2ME anchor constants:
 *   HCENTER = 1, VCENTER = 2, LEFT = 4, RIGHT = 8, TOP = 16, BOTTOM = 32
 *
 * J2ME Sprite transform constants:
 *   TRANS_NONE = 0, TRANS_MIRROR_ROT180 = 1, TRANS_MIRROR = 2, TRANS_ROT180 = 3
 *   TRANS_MIRROR_ROT270 = 4, TRANS_ROT90 = 5, TRANS_ROT270 = 6, TRANS_MIRROR_ROT90 = 7
 */
public class ImageUtils {

    // --- J2ME LCDUI anchor constants ---
    public static final int ANCHOR_HCENTER = 1;
    public static final int ANCHOR_VCENTER = 2;
    public static final int ANCHOR_LEFT    = 4;
    public static final int ANCHOR_RIGHT   = 8;
    public static final int ANCHOR_TOP     = 16;
    public static final int ANCHOR_BOTTOM  = 32;

    // --- J2ME Sprite transform constants ---
    public static final int TRANS_NONE          = 0;
    public static final int TRANS_MIRROR_ROT180 = 1;  // vertical flip
    public static final int TRANS_MIRROR        = 2;  // horizontal flip
    public static final int TRANS_ROT180        = 3;
    public static final int TRANS_MIRROR_ROT270 = 4;
    public static final int TRANS_ROT90         = 5;
    public static final int TRANS_ROT270        = 6;
    public static final int TRANS_MIRROR_ROT90  = 7;

    /**
     * Draw an image at (x,y) using J2ME anchor semantics.
     * Anchor 20 = TOP(16)|LEFT(4) → standard top-left draw.
     */
    public static void drawImage(Graphics2D g, BufferedImage img, int x, int y, int anchor) {
        if (img == null) return;
        int w = img.getWidth();
        int h = img.getHeight();
        int drawX = x;
        int drawY = y;

        // Horizontal adjustment
        if ((anchor & ANCHOR_HCENTER) != 0)      drawX -= w / 2;
        else if ((anchor & ANCHOR_RIGHT) != 0)   drawX -= w;
        // LEFT or 0 → no adjustment

        // Vertical adjustment
        if ((anchor & ANCHOR_VCENTER) != 0)      drawY -= h / 2;
        else if ((anchor & ANCHOR_BOTTOM) != 0)  drawY -= h;
        // TOP or 0 → no adjustment

        g.drawImage(img, drawX, drawY, null);
    }

    /**
     * Emulate J2ME Graphics.drawRegion().
     * Extracts a sub-region, applies a transform, then draws with anchor.
     */
    public static void drawRegion(Graphics2D g, BufferedImage src,
                                  int xSrc, int ySrc, int wSrc, int hSrc,
                                  int transform, int xDest, int yDest, int anchor) {
        if (src == null) return;
        // Clamp source region to image bounds
        xSrc = Math.max(0, xSrc);
        ySrc = Math.max(0, ySrc);
        wSrc = Math.min(wSrc, src.getWidth()  - xSrc);
        hSrc = Math.min(hSrc, src.getHeight() - ySrc);
        if (wSrc <= 0 || hSrc <= 0) return;

        BufferedImage region = src.getSubimage(xSrc, ySrc, wSrc, hSrc);
        BufferedImage transformed = applyTransform(region, transform);
        drawImage(g, transformed, xDest, yDest, anchor);
    }

    /**
     * Apply a J2ME Sprite transform to a BufferedImage and return a pixel-perfect result.
     *
     * Uses direct getRGB/setRGB loops instead of AffineTransform to avoid any
     * sub-pixel drift or rounding artefacts that affect alignment on the 12×12 grid.
     *
     * J2ME MIDP transform semantics (y-axis points DOWN, same as screen):
     *   TRANS_NONE(0)          – no change
     *   TRANS_MIRROR_ROT180(1) – flip vertically   (around horizontal axis)
     *   TRANS_MIRROR(2)        – flip horizontally  (around vertical axis)
     *   TRANS_ROT180(3)        – rotate 180°
     *   TRANS_MIRROR_ROT270(4) – flip H then rotate 270° CW  = transpose
     *   TRANS_ROT90(5)         – rotate 90° CW  (on screen)
     *   TRANS_ROT270(6)        – rotate 90° CCW (on screen)
     *   TRANS_MIRROR_ROT90(7)  – flip H then rotate 90° CW  = anti-transpose
     */
    public static BufferedImage applyTransform(BufferedImage src, int transform) {
        if (src == null || transform == TRANS_NONE) return src;

        int W = src.getWidth();
        int H = src.getHeight();

        // Output dimensions: rotations by 90°/270° swap W and H
        int outW, outH;
        switch (transform) {
            case TRANS_ROT90:
            case TRANS_ROT270:
            case TRANS_MIRROR_ROT270:
            case TRANS_MIRROR_ROT90:
                outW = H; outH = W; break;
            default:
                outW = W; outH = H; break;
        }

        BufferedImage result = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int pixel = src.getRGB(x, y);
                int dx, dy;
                switch (transform) {
                    case TRANS_MIRROR_ROT180: dx = x;       dy = H-1-y; break; // flip vertical
                    case TRANS_MIRROR:        dx = W-1-x;   dy = y;     break; // flip horizontal
                    case TRANS_ROT180:        dx = W-1-x;   dy = H-1-y; break; // 180°
                    case TRANS_MIRROR_ROT270: dx = y;       dy = x;     break; // transpose
                    case TRANS_ROT90:         dx = H-1-y;   dy = x;     break; // 90° CW
                    case TRANS_ROT270:        dx = y;       dy = W-1-x; break; // 90° CCW
                    case TRANS_MIRROR_ROT90:  dx = H-1-y;   dy = W-1-x; break; // anti-transpose
                    default:                  dx = x;       dy = y;     break;
                }
                result.setRGB(dx, dy, pixel);
            }
        }
        return result;
    }

    /**
     * Ensure a sprite image is EXACTLY targetW × targetH pixels.
     * Uses nearest-neighbour scaling (pixel-art safe).
     * Called by loadSprite() so every sprite in the game is grid-aligned.
     */
    public static BufferedImage normalizeSprite(BufferedImage img, int targetW, int targetH) {
        if (img == null) return null;
        if (img.getWidth() == targetW && img.getHeight() == targetH) return img;
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_SPEED);
        g.drawImage(img, 0, 0, targetW, targetH, null);
        g.dispose();
        return out;
    }

    /**
     * Create a blank mutable image (replaces J2ME Image.createImage(w, h)).
     */
    public static BufferedImage createImage(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Load an image from classpath resource (replaces J2ME Image.createImage(path)).
     * Returns null on failure.
     */
    public static BufferedImage loadImage(String path) {
        try {
            InputStream in = ImageUtils.class.getResourceAsStream(path);
            if (in == null) {
                System.err.println("Resource not found: " + path);
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            System.err.println("Error loading image: " + path + " – " + e.getMessage());
            return null;
        }
    }

    /**
     * Copy an image (replaces J2ME Image.createImage(Image src)).
     */
    public static BufferedImage copyImage(BufferedImage src) {
        if (src == null) return null;
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    /**
     * Convert J2ME packed RGB int to java.awt.Color (ignores alpha, treats as 0xRRGGBB).
     */
    public static Color toColor(int rgb) {
        return new Color(rgb & 0xFFFFFF);
    }

    /**
     * Draw a string using J2ME anchor semantics.
     */
    public static void drawString(Graphics2D g, String str, int x, int y, int anchor) {
        if (str == null) return;
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(str);
        int h = fm.getAscent();

        int drawX = x;
        int drawY = y + h; // Java draws from baseline

        if ((anchor & ANCHOR_HCENTER) != 0)      drawX -= w / 2;
        else if ((anchor & ANCHOR_RIGHT) != 0)   drawX -= w;

        if ((anchor & ANCHOR_VCENTER) != 0)      drawY -= h / 2;
        else if ((anchor & ANCHOR_BOTTOM) != 0)  drawY -= h;

        g.drawString(str, drawX, drawY);
    }

    /**
     * Get a 12×12 sprite from a sprite sheet loaded from classpath.
     * row = row index, col = column index (each cell is 12×12 px).
     */
    public static BufferedImage getSpriteFromSheet(BufferedImage sheet, int row, int col) {
        if (sheet == null) return null;
        int x = col * 12;
        int y = row * 12;
        if (x + 12 > sheet.getWidth() || y + 12 > sheet.getHeight()) return null;
        return sheet.getSubimage(x, y, 12, 12);
    }

    /**
     * Load a 12×12 sprite from /bounce/icons/ and GUARANTEE it is exactly 12×12 pixels.
     * If the PNG file has different dimensions (padding, odd size), it is scaled
     * with nearest-neighbour to the tile grid. This prevents sprites from spilling
     * into adjacent tiles on the 12×12 rendering grid.
     */
    public static BufferedImage loadSprite(int row, int col) {
        BufferedImage img = loadImage("/bounce/icons/" + row + col + ".png");
        return normalizeSprite(img, 12, 12);
    }

    /**
     * Load a sprite, optionally fill its background with tintColor, and return
     * a GUARANTEED 12×12 image (sub_30c equivalent).
     *
     * tintColor == 22  → plain load, no tint (sentinel value from J2ME original)
     * tintColor == any other int → fill 12×12 with that RGB, draw sprite on top
     */
    public static BufferedImage loadSpriteColored(int row, int col, int tintColor) {
        BufferedImage src = loadSprite(row, col); // already normalized to 12×12

        // "Plain" load — no tint background needed
        if (tintColor == 22) return src;

        // Composite: solid background + sprite on top → always exactly 12×12
        BufferedImage result = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setColor(toColor(tintColor));
        g.fillRect(0, 0, 12, 12);
        if (src != null) g.drawImage(src, 0, 0, 12, 12, null); // force 12×12
        g.dispose();
        return result;
    }

    /** Convert J2ME Font size constant to an approximate AWT font size in points. */
    public static int j2meFontSize(int sizeConst) {
        switch (sizeConst) {
            case 8:  return 10; // SIZE_SMALL
            case 16: return 14; // SIZE_LARGE
            default: return 12; // SIZE_MEDIUM (0)
        }
    }

    /** Convert J2ME Font face constant to an AWT font family name. */
    public static String j2meFontFace(int faceConst) {
        switch (faceConst) {
            case 32: return Font.MONOSPACED;    // FACE_MONOSPACE
            case 64: return Font.SERIF;          // FACE_PROPORTIONAL
            default: return Font.SANS_SERIF;     // FACE_SYSTEM
        }
    }

    /** Convert J2ME Font style constant to an AWT font style int. */
    public static int j2meFontStyle(int styleConst) {
        int style = Font.PLAIN;
        if ((styleConst & 1) != 0) style |= Font.BOLD;
        if ((styleConst & 2) != 0) style |= Font.ITALIC;
        return style;
    }
}
