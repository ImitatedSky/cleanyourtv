package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.StatFs;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Calendar;

/**
 * block_hud: the dock as a row of hotbar slots with a status HUD over it, all plain rectangles on a whole-pixel
 * grid (no bitmaps). Hearts are the free memory, hunger the free storage, armour the Wi-Fi signal, and the XP bar
 * the time of day with the day of the month as its level. DockDecoration draws it; MainActivity's stats reader
 * fills in the values every few seconds while Home is on screen.
 */
final class BlockHud {

    // Sprites, one string per row. '.' is transparent, 'k' the outline; the other letters are colours, see colour().
    static final String[] HEART = {
            ".kk...kk.",
            "kwrk.krrk",
            "kwrrkrrrk",
            "krrrrrrmk",
            ".krrrrmk.",
            "..krrmk..",
            "...kmk...",
            "....k....",
    };
    static final String[] FOOD = {
            "....kkkk.",
            "...kooobk",
            "..koobbbk",
            "..kobbbnk",
            "..kbbbnk.",
            ".kgknnk..",
            "kwwgkk...",
            "kwgk.....",
            ".kk......",
    };
    static final String[] ARMOUR = {
            "kkk...kkk",
            "kaak.kaak",
            "kwaakaadk",
            "kkaaaaakk",
            ".kwaaadk.",
            ".kaaaadk.",
            ".kaaaadk.",
            ".kkkkkkk.",
    };
    static final String[] CLOCK = {
            "...kkkkk...",
            "..kyyyyyk..",
            ".kyffhffyk.",
            "kyfffhfffYk",
            "kyfffhfffYk",
            "kyfffhhhfYk",
            "kyfffffffYk",
            "kyfffffffYk",
            ".kYfffffYk.",
            "..kYYYYYk..",
            "...kkkkk...",
    };

    /** Width of the HUD in its own pixels, as wide as the hotbar; sets the pixel size (see pixel()). */
    static final int HUD_WIDTH = 182;
    /** Height of the rows above the slots in pixels: armour, hearts, the XP bar and the gap above the slots. */
    static final int ROWS_HEIGHT = 27;

    private static final int SLOT_EDGE = 0xE6000000, SLOT_FILL = 0x8C141414;
    private static final int SLOT_LIGHT = 0x40FFFFFF, SLOT_SHADE = 0x59000000;
    private static final int XP_EMPTY = 0xCC1A1A1A, XP_LIGHT = 0xFFB4FF6E, XP_GREEN = 0xFF7ED81E, XP_DARK = 0xFF4E9A12;
    private static final int LEVEL_GREEN = 0xFF80FF20;

    private final Paint paint = new Paint();
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Hearts and hunger in half icons (0 to 20), armour 0 to 10 (-1 off Wi-Fi), XP in 1/1000 of the day, level. */
    private int[] values;
    /** The rows as last drawn, recorded once and replayed on every frame of the grid until a value changes. */
    private Picture rows;
    private int rowsWidth, rowsPixel;

    BlockHud(Typeface font) {
        text.setTypeface(font);
    }

    /** New values from read(); true when any differs and the HUD has to be drawn again. */
    boolean set(int[] next) {
        if (Arrays.equals(values, next)) return false;
        values = next;
        rows = null;
        return true;
    }

    /** {hearts, hunger, armour, xp, level} for set(). Binder calls and a statfs: run it off the main thread. */
    static int[] read(Context ctx) {
        ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
        ActivityManager am = ctx.getSystemService(ActivityManager.class);
        if (am != null) am.getMemoryInfo(mem);
        long free = 0, total = 0;
        try {
            StatFs data = new StatFs(Environment.getDataDirectory().getPath());
            free = data.getAvailableBytes();
            total = data.getTotalBytes();
        } catch (IllegalArgumentException ignored) {
        }
        Calendar now = Calendar.getInstance();
        int xp = dayPermille(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
        return new int[]{halves(mem.availMem, mem.totalMem), halves(free, total), wifiLevel(ctx), xp,
                now.get(Calendar.DAY_OF_MONTH)};
    }

    /** 0 to 10 for the Wi-Fi signal, or -1 when the TV is not on Wi-Fi (the armour row is hidden then). */
    private static int wifiLevel(Context ctx) {
        try {
            ConnectivityManager cm = ctx.getSystemService(ConnectivityManager.class);
            NetworkCapabilities caps = cm != null ? cm.getNetworkCapabilities(cm.getActiveNetwork()) : null;
            if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return -1;
            WifiManager wm = ctx.getApplicationContext().getSystemService(WifiManager.class);
            return wm != null ? WifiManager.calculateSignalLevel(wm.getConnectionInfo().getRssi(), 11) : -1;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** The share part/whole in half icons of a row of ten: 0 to 20. */
    static int halves(long part, long whole) {
        if (whole <= 0 || part <= 0) return 0;
        return (int) Math.min(20, Math.round(part * 20.0 / whole));
    }

    /** How far through the day this time is, in 1/1000. */
    static int dayPermille(int hour, int minute, int second) {
        return (hour * 3600 + minute * 60 + second) * 1000 / 86400;
    }

    /** Screen pixels per HUD pixel for a hotbar this wide (whole pixels, so the pixel art stays sharp). */
    static int pixel(int hotbarWidthPx) {
        return Math.max(1, hotbarWidthPx / HUD_WIDTH);
    }

    /** One hotbar slot: a dark translucent square with a 1-pixel edge, lighter at the top and left, darker below. */
    void drawSlot(Canvas c, int l, int t, int r, int b, int p) {
        ring(c, paint, l, t, r, b, p, SLOT_EDGE);
        fill(c, paint, l + p, t + p, r - p, b - p, SLOT_FILL);
        fill(c, paint, l + p, t + p, r - p, t + 2 * p, SLOT_LIGHT);
        fill(c, paint, l + p, t + 2 * p, l + 2 * p, b - p, SLOT_LIGHT);
        fill(c, paint, l + 2 * p, b - 2 * p, r - p, b - p, SLOT_SHADE);
        fill(c, paint, r - 2 * p, t + 2 * p, r - p, b - 2 * p, SLOT_SHADE);
    }

    /** The focused slot's frame: thick white, a pixel larger than the slot all round, with a black line outside. */
    void drawSelection(Canvas c, int l, int t, int r, int b, int p) {
        ring(c, paint, l - 2 * p, t - 2 * p, r + 2 * p, b + 2 * p, p, 0xFF000000);
        ring(c, paint, l - p, t - p, r + p, b + p, 2 * p, 0xFFFFFFFF);
    }

    /** The rows above a hotbar from left to right whose slots start at slotTop. Nothing until the first read. */
    void drawRows(Canvas c, int left, int right, int slotTop, int p) {
        if (values == null) return;
        int width = right - left;
        if (rows == null || width != rowsWidth || p != rowsPixel) {
            rows = new Picture();
            recordRows(rows.beginRecording(width, ROWS_HEIGHT * p), width, p);
            rows.endRecording();
            rowsWidth = width;
            rowsPixel = p;
        }
        c.save();
        c.translate(left, slotTop - ROWS_HEIGHT * p);
        c.drawPicture(rows);
        c.restore();
    }

    /**
     * Armour over the hearts on the left, hunger on the right filling from the right, the XP bar across under them
     * with the level centred on its top edge. Rows of ten 9-pixel icons, 8 pixels apart.
     */
    private void recordRows(Canvas c, int width, int p) {
        int hearts = values[0], food = values[1], armour = values[2], xp = values[3], level = values[4];
        for (int i = 0; i < 10; i++) {
            int x = p + 8 * p * i;
            if (armour >= 0) icon(c, ARMOUR, x, 0, p, Math.min(2, Math.max(0, 2 * (armour - i))), false);
            icon(c, HEART, x, 10 * p, p, Math.min(2, Math.max(0, hearts - 2 * i)), false);
            icon(c, FOOD, width - 10 * p - 8 * p * i, 10 * p, p, Math.min(2, Math.max(0, food - 2 * i)), true);
        }
        int top = 20 * p;
        fill(c, paint, 0, top, width, top + 5 * p, 0xFF000000);
        fill(c, paint, p, top + p, width - p, top + 4 * p, XP_EMPTY);
        int filled = (width - 2 * p) * xp / 1000;
        fill(c, paint, p, top + p, p + filled, top + 2 * p, XP_LIGHT);
        fill(c, paint, p, top + 2 * p, p + filled, top + 3 * p, XP_GREEN);
        fill(c, paint, p, top + 3 * p, p + filled, top + 4 * p, XP_DARK);
        // The level in the pixel font, 11 font pixels tall, outlined in black and standing on the bar's top edge.
        int m = Math.max(1, Math.round(p * 0.7f));
        text.setTextSize(12f * m);
        String s = String.valueOf(level);
        float x = (width - text.measureText(s)) / 2f;
        float y = top + p;
        text.setColor(0xFF000000);
        c.drawText(s, x - m, y, text);
        c.drawText(s, x + m, y, text);
        c.drawText(s, x, y - m, text);
        c.drawText(s, x, y + m, text);
        text.setColor(LEVEL_GREEN);
        c.drawText(s, x, y, text);
    }

    /** One icon at (x, y): fill 2 is whole, 1 half (the left half, or the right one for a row filling from the right), 0 empty. */
    private void icon(Canvas c, String[] sprite, int x, int y, int p, int fill, boolean fromRight) {
        int w = sprite[0].length();
        int from = fill == 2 ? 0 : fill == 0 ? w : fromRight ? w / 2 : 0;
        int to = fill == 2 ? w : fill == 0 ? 0 : fromRight ? w : w / 2 + 1;
        // 9 pixels high: shorter sprites stand on the row's bottom.
        draw(c, paint, sprite, x, y + (9 - sprite.length) * p, p, from, to);
    }

    /** Draws the sprite in whole pixels of p px, in full colour between columns from and to and empty elsewhere. */
    private static void draw(Canvas c, Paint paint, String[] sprite, int x, int y, int p, int from, int to) {
        for (int row = 0; row < sprite.length; row++) {
            String line = sprite[row];
            int col = 0;
            while (col < line.length()) {
                int color = colour(line.charAt(col), col < from || col >= to);
                int end = col + 1;
                while (end < line.length() && colour(line.charAt(end), end < from || end >= to) == color) end++;
                if (color != 0) fill(c, paint, x + col * p, y + row * p, x + end * p, y + (row + 1) * p, color);
                col = end;
            }
        }
    }

    private static int colour(char ch, boolean empty) {
        if (ch == '.') return 0;
        if (ch == 'k') return 0xFF101010;
        if (empty) return 0x8C000000;
        switch (ch) {
            case 'w': return 0xFFF6F6F6;   // highlight, bone
            case 'r': return 0xFFE3262C;   // heart
            case 'm': return 0xFF8E1218;
            case 'o': return 0xFFE59A55;   // meat
            case 'b': return 0xFFB2602A;
            case 'n': return 0xFF6E3412;
            case 'g': return 0xFFC9C2B0;   // bone shade
            case 'a': return 0xFFC4C4C4;   // iron
            case 'd': return 0xFF7C7C7C;
            case 'y': return 0xFFF5C83A;   // clock case
            case 'Y': return 0xFFB07F12;
            case 'f': return 0xFFF3EEDC;   // clock face
            case 'h': return 0xFF2B2B2B;   // hands
            default: return 0;
        }
    }

    private static void fill(Canvas c, Paint paint, int l, int t, int r, int b, int color) {
        if (r <= l || b <= t) return;
        paint.setColor(color);
        c.drawRect(l, t, r, b, paint);
    }

    /** A frame w px thick just inside (l, t, r, b), as four rectangles that do not overlap. */
    private static void ring(Canvas c, Paint paint, int l, int t, int r, int b, int w, int color) {
        fill(c, paint, l, t, r, t + w, color);
        fill(c, paint, l, b - w, r, b, color);
        fill(c, paint, l, t + w, l + w, b - w, color);
        fill(c, paint, r - w, t + w, r, b - w, color);
    }

    /** A sprite as a drawable in pixels of p px, for the clock icon beside the time. */
    static final class Icon extends Drawable {
        private final String[] sprite;
        private final int p;
        private final Paint paint = new Paint();

        Icon(String[] sprite, int p) {
            this.sprite = sprite;
            this.p = p;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            BlockHud.draw(canvas, paint, sprite, getBounds().left, getBounds().top, p, 0, sprite[0].length());
        }

        @Override
        public int getIntrinsicWidth() {
            return sprite[0].length() * p;
        }

        @Override
        public int getIntrinsicHeight() {
            return sprite.length * p;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
