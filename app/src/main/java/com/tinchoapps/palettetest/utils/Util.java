package com.tinchoapps.palettetest.utils;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.view.Display;
import android.view.WindowManager;

public final class Util
{
    private static Handler handler;

    private Util()
    {
    }

    public static Point getScreenSize(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        return new Point(display.getWidth(), display.getHeight());
    }

    // Returns the next power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0 or
    // the answer overflows.
    public static int nextPowerOf2(int n)
    {
        if (n <= 0 || n > (1 << 30)) throw new IllegalArgumentException("n is invalid: " + n);
        n -= 1;
        n |= n >> 16;
        n |= n >> 8;
        n |= n >> 4;
        n |= n >> 2;
        n |= n >> 1;
        return n + 1;
    }

    // Returns the previous power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0
    public static int prevPowerOf2(int n)
    {
        if (n <= 0) throw new IllegalArgumentException();
        return Integer.highestOneBit(n);
    }

    // Throws AssertionError if the input is false.
    public static void assertTrue(boolean cond)
    {
        if (!cond)
        {
            throw new AssertionError();
        }
    }


}
