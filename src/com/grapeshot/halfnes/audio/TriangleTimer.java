/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.audio;

/**
 *
 * @author Andrew
 */
public class TriangleTimer extends Timer {

    private int divider = 0;
    private final static int periodadd = 1;
    private final static int[] triangle = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        12, 13, 14, 15, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6,
        5, 4, 3, 2, 1, 0,};

    public TriangleTimer() {
        period = 0;
        position = 0;
    }

    @Override
    public final void reset() {
        //no way to reset the triangle
    }

    @Override
    public final void clock() {
        if (period == 0) {
            return;
        }
        --divider;
        while (divider <= 0) {
            divider += period + periodadd;
            position = (++position & 31);
        }
    }

    public final void clock(final int cycles) {
        if (period == 0) {
            return;
        }
        divider -= cycles;
        while (divider <= 0) {
            divider += period + periodadd;
            ++position;
            position &= 31;
            if (position > 31) {
                System.err.println("how");
            }
        }
    }

    @Override
    public final int getval() {
        return (period == 0) ? 7 : triangle[position];
        //needed to avoid screech when period is zero
    }

    public final void setperiod(final int newperiod) {
        period = newperiod;
        if (period == 0) {
            position = 7;
        }
    }

    @Override
    public void setduty(int duty) {
        throw new UnsupportedOperationException("Triangle counter has no duty setting.");
    }

    @Override
    public void setduty(int[] duty) {
        throw new UnsupportedOperationException("Triangle counter has no duty setting.");
    }
}
