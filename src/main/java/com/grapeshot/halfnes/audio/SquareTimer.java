/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.audio;

/**
 *
 * @author Andrew
 */
public class SquareTimer extends Timer {

    protected int[] values;
    final private int periodadd;
    private int divider = 0;

    @Override
    public final void clock() {
        if (period + periodadd <= 0) {
            return;
        }
        ++divider;
        // note: stay away from negative division to avoid rounding problems
        int periods = (divider + period + periodadd) / (period + periodadd);
        if (periods < 0) {
            periods = 0; // can happen if period or periodadd were made smaller
        }
        position = (position + periods) % values.length;
        divider -= (period + periodadd) * periods;
    }

    @Override
    public final void clock(final int cycles) {
        if (period < 8) {
            return;
        }
        divider += cycles;
        // note: stay away from negative division to avoid rounding problems
        int periods = (divider + period + periodadd) / (period + periodadd);
        if (periods < 0) {
            periods = 0; // can happen if period or periodadd were made smaller
        }
        position = (position + periods) % values.length;
        divider -= (period + periodadd) * periods;
    }

    public SquareTimer(final int ctrlen, final int periodadd) {
        this.periodadd = periodadd;
        values = new int[ctrlen];
        period = 0;
        position = 0;
        setduty(ctrlen / 2);
    }

    public SquareTimer(final int ctrlen) {
        this.periodadd = 0;
        values = new int[ctrlen];
        period = 0;
        position = 0;
        setduty(ctrlen / 2);
    }

    @Override
    public final void reset() {
        position = 0;
    }

    @Override
    public final void setduty(final int duty) {
        for (int i = 0; i < values.length; ++i) {
            values[i] = (i < duty) ? 1 : 0;
        }
    }

    @Override
    public final void setduty(int[] dutyarray) {
        values = dutyarray;
    }

    @Override
    public final int getval() {
        return values[position];
    }

    @Override
    public final void setperiod(final int newperiod) {
        period = newperiod;
    }
}