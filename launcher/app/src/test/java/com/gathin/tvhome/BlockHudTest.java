package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** block_hud: the values behind the rows of icons, the pixel size, and the sprites' shape. */
public class BlockHudTest {

    @Test
    public void halvesOfARowOfTen() {
        assertEquals(0, BlockHud.halves(0, 1000));
        assertEquals(10, BlockHud.halves(500, 1000));
        assertEquals(20, BlockHud.halves(1000, 1000));
        assertEquals(1, BlockHud.halves(26, 1000));
        assertEquals(0, BlockHud.halves(24, 1000));
    }

    @Test
    public void halvesWithNothingToMeasureIsEmpty() {
        assertEquals(0, BlockHud.halves(5, 0));
        assertEquals(0, BlockHud.halves(-1, 1000));
        assertEquals(20, BlockHud.halves(2000, 1000));
    }

    @Test
    public void dayFromMidnightToMidnight() {
        assertEquals(0, BlockHud.dayPermille(0, 0, 0));
        assertEquals(500, BlockHud.dayPermille(12, 0, 0));
        assertEquals(999, BlockHud.dayPermille(23, 59, 59));
    }

    @Test
    public void wholePixelsOfAtLeastOne() {
        assertEquals(9, BlockHud.pixel(1656));
        assertEquals(1, BlockHud.pixel(100));
        assertEquals(1, BlockHud.pixel(0));
    }

    @Test
    public void spritesAreRectangles() {
        for (String[] sprite : new String[][]{BlockHud.HEART, BlockHud.FOOD, BlockHud.ARMOUR, BlockHud.CLOCK}) {
            for (String row : sprite) assertEquals(sprite[0].length(), row.length());
        }
        assertEquals(9, BlockHud.HEART[0].length());
        assertEquals(9, BlockHud.FOOD[0].length());
        assertEquals(9, BlockHud.ARMOUR[0].length());
    }
}
